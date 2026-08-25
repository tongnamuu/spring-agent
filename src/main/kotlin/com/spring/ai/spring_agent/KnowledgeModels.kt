package com.spring.ai.spring_agent

data class KnowledgeDocumentRequest(
	val title: String,
	val topic: String,
	val source: String,
	val content: String,
) {
	init {
		require(title.isNotBlank()) { "title must not be blank" }
		require(topic.isNotBlank()) { "topic must not be blank" }
		require(source.isNotBlank()) { "source must not be blank" }
		require(content.isNotBlank()) { "content must not be blank" }
	}
}

data class KnowledgeDocumentResponse(
	val documentId: String,
	val chunkCount: Int,
)

data class KnowledgeSearchRequest(
	val query: String,
	val topK: Int? = null,
) {
	init {
		require(query.isNotBlank()) { "query must not be blank" }
		require(topK == null || topK in 1..20) { "topK must be between 1 and 20" }
	}
}

data class KnowledgeSearchResult(
	val id: String,
	val documentId: String,
	val title: String,
	val topic: String,
	val source: String,
	val content: String,
	val score: Double,
)
