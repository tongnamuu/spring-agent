package com.spring.ai.spring_agent

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnowledgeIngestionAndSearchTests {

	@Test
	fun `stores every generated chunk`() {
		val storage = FakeVectorStorage()
		val ingestion = KnowledgeIngestion(
			vectorStorage = storage,
			documentChunking = DocumentChunking { listOf("chunk one", "chunk two") },
		)
		val request = KnowledgeDocumentRequest(
			title = "MySQL indexes",
			topic = "mysql",
			source = "Database book",
			content = "Long source content",
		)

		val result = ingestion.add(request)

		assertEquals(2, result.chunkCount)
		assertEquals(request, storage.addedRequest)
		assertEquals(listOf("chunk one", "chunk two"), storage.addedChunks)
	}

	@Test
	fun `keeps applicable candidates and rejects misleading related candidates`() {
		val dijkstra = result(
			title = "Dijkstra's algorithm",
			content = "Dijkstra is correct only when every edge weight is non-negative.",
		)
		val bellmanFord = result(
			id = "bellman-ford-1",
			title = "Bellman-Ford algorithm",
			content = "Bellman-Ford finds shortest paths when edges can have negative weights.",
		)
		val storage = FakeVectorStorage(candidates = listOf(dijkstra, bellmanFord))
		val relevance = SearchRelevance { query, candidate ->
			!(query.contains("negative", ignoreCase = true) && candidate.title.contains("Dijkstra"))
		}
		val search = search(storage = storage, relevance = relevance)

		val matches = search.search("shortest paths with negative edge weights", topK = 5)

		assertEquals(listOf("Bellman-Ford algorithm"), matches.map { it.title })
		assertEquals(10, storage.lastTopK)
		assertEquals(0.35, storage.lastSimilarityThreshold)
	}

	@Test
	fun `returns no Dijkstra passage for a negative weight query`() {
		val dijkstra = result(
			title = "다익스트라 알고리즘",
			content = "모든 간선의 가중치가 0 이상일 때 최단 경로를 구한다.",
		)
		val search = search(
			storage = FakeVectorStorage(candidates = listOf(dijkstra)),
			relevance = SearchRelevance { _, _ -> false },
		)

		val matches = search.search("음의 가중치가 있는 그래프의 최단거리", topK = 5)

		assertTrue(matches.isEmpty())
	}

	private fun search(
		storage: FakeVectorStorage,
		relevance: SearchRelevance = SearchRelevance { _, _ -> true },
	) = VectorKnowledgeSearch(
		vectorStorage = storage,
		searchRelevance = relevance,
		defaultTopK = 5,
		candidateMultiplier = 2,
		similarityThreshold = 0.35,
	)

	private fun result(
		id: String = "dijkstra-1",
		title: String,
		content: String,
	) = KnowledgeSearchResult(
		id = id,
		documentId = id.substringBeforeLast('-'),
		title = title,
		topic = "algorithms",
		source = "Algorithm book",
		content = content,
		score = 0.9,
	)

	private class FakeVectorStorage(
		private val candidates: List<KnowledgeSearchResult> = emptyList(),
	) : VectorStorage {
		var addedRequest: KnowledgeDocumentRequest? = null
		var addedChunks: List<String>? = null
		var lastTopK: Int? = null
		var lastSimilarityThreshold: Double? = null

		override fun add(request: KnowledgeDocumentRequest, chunks: List<String>): KnowledgeDocumentResponse {
			addedRequest = request
			addedChunks = chunks
			return KnowledgeDocumentResponse("document-1", chunks.size)
		}

		override fun searchCandidates(
			query: String,
			topK: Int,
			similarityThreshold: Double,
		): List<KnowledgeSearchResult> {
			lastTopK = topK
			lastSimilarityThreshold = similarityThreshold
			return candidates
		}
	}
}
