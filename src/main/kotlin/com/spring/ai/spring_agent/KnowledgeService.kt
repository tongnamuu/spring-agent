package com.spring.ai.spring_agent

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

fun interface KnowledgeRetriever {
	fun search(query: String): List<KnowledgeSearchResult>
}

fun interface KnowledgeRelevanceEvaluator {
	fun isRelevant(query: String, candidate: KnowledgeSearchResult): Boolean
}

@Service
class KnowledgeService(
	private val knowledgeStore: KnowledgeStore,
	private val chunker: KnowledgeChunker,
	private val relevanceEvaluator: KnowledgeRelevanceEvaluator,
	@Value("\${spring.rag.search.top-k:5}") private val defaultTopK: Int,
	@Value("\${spring.rag.search.candidate-multiplier:2}") private val candidateMultiplier: Int,
	@Value("\${spring.rag.search.similarity-threshold:0.55}") private val similarityThreshold: Double,
) : KnowledgeRetriever {
	fun add(request: KnowledgeDocumentRequest): KnowledgeDocumentResponse =
		knowledgeStore.add(request, chunker.split(request.content))

	override fun search(query: String): List<KnowledgeSearchResult> = search(query, defaultTopK)

	fun search(query: String, topK: Int): List<KnowledgeSearchResult> =
		knowledgeStore.searchCandidates(
			query = query,
			topK = topK * candidateMultiplier,
			similarityThreshold = similarityThreshold,
		).asSequence()
			.filter { relevanceEvaluator.isRelevant(query, it) }
			.take(topK)
			.toList()
}
