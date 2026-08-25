package com.spring.ai.spring_agent

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.head
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import kotlin.test.assertEquals

class ElasticsearchKnowledgeStoreTests {
	private lateinit var wireMock: WireMockServer

	@BeforeEach
	fun startWireMock() {
		wireMock = WireMockServer(wireMockConfig().dynamicPort())
		wireMock.start()
	}

	@AfterEach
	fun stopWireMock() {
		if (::wireMock.isInitialized) wireMock.stop()
	}

	@Test
	fun `creates an Elasticsearch 8 dense vector index`() {
		wireMock.stubFor(head(urlEqualTo("/knowledge")).willReturn(aResponse().withStatus(404)))
		wireMock.stubFor(
			put(urlEqualTo("/knowledge"))
				.willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{}")),
		)
		val store = store(initializeSchema = true)

		store.initializeIndex()

		wireMock.verify(
			putRequestedFor(urlEqualTo("/knowledge"))
				.withRequestBody(containing("\"type\":\"dense_vector\""))
				.withRequestBody(containing("\"dims\":3"))
				.withRequestBody(containing("\"similarity\":\"cosine\"")),
		)
	}

	@Test
	fun `stores Korean chunks as UTF-8 NDJSON`() {
		wireMock.stubFor(
			post(urlEqualTo("/knowledge/_bulk?refresh=wait_for"))
				.willReturn(
					aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("{\"errors\":false}"),
				),
		)
		val store = store()

		val response = store.add(
			KnowledgeDocumentRequest(
				title = "다익스트라 최단 경로 알고리즘",
				topic = "algorithms",
				source = "알고리즘 교재",
				content = "음의 가중치에는 적용할 수 없다.",
			),
			listOf("음의 가중치에는 적용할 수 없다."),
		)

		assertEquals(1, response.chunkCount)
		wireMock.verify(
			postRequestedFor(urlEqualTo("/knowledge/_bulk?refresh=wait_for"))
				.withHeader("Content-Type", containing("application/x-ndjson"))
				.withHeader("Content-Type", containing("charset=UTF-8"))
				.withRequestBody(containing("다익스트라 최단 경로 알고리즘"))
				.withRequestBody(containing("음의 가중치에는 적용할 수 없다")),
		)
	}

	@Test
	fun `maps Elasticsearch cosine scores and document fields`() {
		wireMock.stubFor(
			post(urlEqualTo("/knowledge/_search"))
				.willReturn(
					aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody(
							"""
							{
							  "hits": {
							    "hits": [{
							      "_id": "dijkstra-1",
							      "_score": 0.9,
							      "_source": {
							        "document_id": "dijkstra",
							        "title": "다익스트라",
							        "topic": "algorithms",
							        "source": "알고리즘 교재",
							        "content": "비음수 가중치에서 동작한다."
							      }
							    }]
							  }
							}
							""".trimIndent(),
						),
				),
		)
		val store = store()

		val result = store.searchCandidates("다익스트라", topK = 5, similarityThreshold = 0.35).single()

		assertEquals("dijkstra-1", result.id)
		assertEquals("다익스트라", result.title)
		assertEquals(0.8, result.score, absoluteTolerance = 0.000_001)
		wireMock.verify(
			postRequestedFor(urlEqualTo("/knowledge/_search"))
				.withRequestBody(containing("\"min_score\":0.675"))
				.withRequestBody(containing("\"query_vector\":[1.0,0.0,0.0]")),
		)
	}

	private fun store(initializeSchema: Boolean = false) = ElasticsearchKnowledgeStore(
		textEmbedder = TextEmbedder { texts -> texts.map { floatArrayOf(1f, 0f, 0f) } },
		objectMapper = JsonMapper.builder().build(),
		elasticsearchUri = wireMock.baseUrl(),
		indexName = "knowledge",
		dimensions = 3,
		initializeSchema = initializeSchema,
	)
}
