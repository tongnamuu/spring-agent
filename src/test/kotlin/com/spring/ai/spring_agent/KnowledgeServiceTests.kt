package com.spring.ai.spring_agent

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KnowledgeServiceTests {

	@Test
	fun `stores every generated chunk`() {
		val store = FakeKnowledgeStore()
		val service = service(
			store = store,
			chunker = KnowledgeChunker { listOf("chunk one", "chunk two") },
		)
		val request = KnowledgeDocumentRequest(
			title = "MySQL indexes",
			topic = "mysql",
			source = "Database book",
			content = "Long source content",
		)

		val result = service.add(request)

		assertEquals(2, result.chunkCount)
		assertEquals(request, store.addedRequest)
		assertEquals(listOf("chunk one", "chunk two"), store.addedChunks)
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
		val store = FakeKnowledgeStore(candidates = listOf(dijkstra, bellmanFord))
		val evaluator = KnowledgeRelevanceEvaluator { query, candidate ->
			!(query.contains("negative", ignoreCase = true) && candidate.title.contains("Dijkstra"))
		}
		val service = service(store = store, relevanceEvaluator = evaluator)

		val matches = service.search("shortest paths with negative edge weights", topK = 5)

		assertEquals(listOf("Bellman-Ford algorithm"), matches.map { it.title })
		assertEquals(10, store.lastTopK)
		assertEquals(0.35, store.lastSimilarityThreshold)
	}

	@Test
	fun `returns no Dijkstra passage for a negative weight query`() {
		val dijkstra = result(
			title = "다익스트라 알고리즘",
			content = "모든 간선의 가중치가 0 이상일 때 최단 경로를 구한다.",
		)
		val service = service(
			store = FakeKnowledgeStore(candidates = listOf(dijkstra)),
			relevanceEvaluator = KnowledgeRelevanceEvaluator { _, _ -> false },
		)

		val matches = service.search("음의 가중치가 있는 그래프의 최단거리", topK = 5)

		assertTrue(matches.isEmpty())
	}

	private fun service(
		store: FakeKnowledgeStore,
		chunker: KnowledgeChunker = KnowledgeChunker { listOf(it) },
		relevanceEvaluator: KnowledgeRelevanceEvaluator = KnowledgeRelevanceEvaluator { _, _ -> true },
	) = KnowledgeService(
		knowledgeStore = store,
		chunker = chunker,
		relevanceEvaluator = relevanceEvaluator,
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

	private class FakeKnowledgeStore(
		private val candidates: List<KnowledgeSearchResult> = emptyList(),
	) : KnowledgeStore {
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
