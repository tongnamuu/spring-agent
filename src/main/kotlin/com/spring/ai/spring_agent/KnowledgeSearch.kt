package com.spring.ai.spring_agent

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

fun interface KnowledgeSearch {
	fun search(query: String): List<KnowledgeSearchResult>
}

@Component
class VectorKnowledgeSearch(
	private val vectorStorage: VectorStorage,
	private val searchRelevance: SearchRelevance,
	@Value("\${spring.rag.search.top-k:5}") private val defaultTopK: Int,
	@Value("\${spring.rag.search.candidate-multiplier:2}") private val candidateMultiplier: Int,
	@Value("\${spring.rag.search.similarity-threshold:0.55}") private val similarityThreshold: Double,
) : KnowledgeSearch {
	override fun search(query: String): List<KnowledgeSearchResult> = search(query, defaultTopK)

	fun search(query: String, topK: Int): List<KnowledgeSearchResult> =
		vectorStorage.searchCandidates(
			query = query,
			topK = topK * candidateMultiplier,
			similarityThreshold = similarityThreshold,
		).asSequence()
			.filter { searchRelevance.isRelevant(query, it) }
			.take(topK)
			.toList()
}
