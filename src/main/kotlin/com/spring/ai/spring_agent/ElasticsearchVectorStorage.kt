package com.spring.ai.spring_agent

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Repository
import org.springframework.web.client.RestClient
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.util.UUID

interface VectorStorage {
	fun add(request: KnowledgeDocumentRequest, chunks: List<String>): KnowledgeDocumentResponse
	fun searchCandidates(query: String, topK: Int, similarityThreshold: Double): List<KnowledgeSearchResult>
}

@Repository
class ElasticsearchVectorStorage(
	private val textEmbedding: TextEmbedding,
	private val objectMapper: ObjectMapper,
	@Value("\${spring.elasticsearch.uris:http://localhost:9200}") elasticsearchUri: String,
	@Value("\${spring.ai.vectorstore.elasticsearch.index-name:spring-agent-knowledge}") private val indexName: String,
	@Value("\${spring.ai.vectorstore.elasticsearch.dimensions:1024}") private val dimensions: Int,
	@Value("\${spring.ai.vectorstore.elasticsearch.initialize-schema:true}") private val initializeSchema: Boolean,
) : VectorStorage {
	private val restClient = RestClient.builder().baseUrl(elasticsearchUri.substringBefore(',')).build()

	@PostConstruct
	fun initializeIndex() {
		if (!initializeSchema || indexExists()) return

		restClient.put()
			.uri("/$indexName")
			.contentType(MediaType.APPLICATION_JSON)
			.body(indexMapping())
			.retrieve()
			.toBodilessEntity()
	}

	override fun add(request: KnowledgeDocumentRequest, chunks: List<String>): KnowledgeDocumentResponse {
		val documentId = UUID.randomUUID().toString()
		val contents = chunks.map { "${request.title}\n\n$it" }
		val embeddings = textEmbedding.embed(contents)
		require(embeddings.size == contents.size) { "Embedding count does not match chunk count" }

		val bulkBody = contents.indices.joinToString(separator = "", transform = { index ->
			val id = "$documentId-${index + 1}"
			val embedding = embeddings[index]
			require(embedding.size == dimensions) {
				"Embedding dimensions ${embedding.size} do not match Elasticsearch index dimensions $dimensions"
			}
			val source = mapOf(
				"document_id" to documentId,
				"title" to request.title,
				"topic" to request.topic,
				"source" to request.source,
				"chunk_index" to index,
				"content" to contents[index],
				"embedding" to embedding,
			)
			objectMapper.writeValueAsString(mapOf("index" to mapOf("_id" to id))) + "\n" +
				objectMapper.writeValueAsString(source) + "\n"
		})

		@Suppress("UNCHECKED_CAST")
		val response = restClient.post()
			.uri("/$indexName/_bulk?refresh=wait_for")
			.contentType(NDJSON)
			.body(bulkBody)
			.retrieve()
			.body(Map::class.java) as Map<String, Any?>
		require(response["errors"] == false) { "Elasticsearch rejected one or more document chunks" }

		return KnowledgeDocumentResponse(documentId, chunks.size)
	}

	@Suppress("UNCHECKED_CAST")
	override fun searchCandidates(
		query: String,
		topK: Int,
		similarityThreshold: Double,
	): List<KnowledgeSearchResult> {
		val instructedQuery = "$QUERY_INSTRUCTION\nQuery: $query"
		val queryVector = textEmbedding.embed(listOf(instructedQuery)).single()
		require(queryVector.size == dimensions) {
			"Embedding dimensions ${queryVector.size} do not match Elasticsearch index dimensions $dimensions"
		}
		val elasticsearchMinimumScore = (similarityThreshold + 1.0) / 2.0
		val body = mapOf(
			"size" to topK,
			"min_score" to elasticsearchMinimumScore,
			"_source" to mapOf("excludes" to listOf("embedding")),
			"knn" to mapOf(
				"field" to "embedding",
				"query_vector" to queryVector,
				"k" to topK,
				"num_candidates" to maxOf(100, topK * 10),
			),
		)

		val response = restClient.post()
			.uri("/$indexName/_search")
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.retrieve()
			.body(Map::class.java) as Map<String, Any?>
		val hits = ((response.getValue("hits") as Map<*, *>)["hits"] as List<Map<String, Any?>>)

		return hits.map { hit ->
			val source = hit.getValue("_source") as Map<String, Any?>
			val elasticsearchScore = (hit.getValue("_score") as Number).toDouble()
			KnowledgeSearchResult(
				id = hit.getValue("_id").toString(),
				documentId = source.getValue("document_id").toString(),
				title = source.getValue("title").toString(),
				topic = source.getValue("topic").toString(),
				source = source.getValue("source").toString(),
				content = source.getValue("content").toString(),
				score = elasticsearchScore * 2.0 - 1.0,
			)
		}
	}

	private fun indexExists(): Boolean = restClient.head()
		.uri("/$indexName")
		.exchange { _, response -> response.statusCode.is2xxSuccessful }

	private fun indexMapping(): Map<String, Any> = mapOf(
		"mappings" to mapOf(
			"dynamic" to "strict",
			"properties" to mapOf(
				"document_id" to mapOf("type" to "keyword"),
				"title" to mapOf("type" to "text"),
				"topic" to mapOf("type" to "keyword"),
				"source" to mapOf("type" to "keyword"),
				"chunk_index" to mapOf("type" to "integer"),
				"content" to mapOf("type" to "text"),
				"embedding" to mapOf(
					"type" to "dense_vector",
					"dims" to dimensions,
					"index" to true,
					"similarity" to "cosine",
				),
			),
		),
	)

	private companion object {
		val NDJSON: MediaType = MediaType("application", "x-ndjson", StandardCharsets.UTF_8)
		const val QUERY_INSTRUCTION = "Instruct: Given a web search query, retrieve relevant passages that answer the query"
	}
}
