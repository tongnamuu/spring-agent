package com.spring.ai.spring_agent

import org.springframework.stereotype.Component

@Component
class KnowledgeIngestion(
	private val vectorStorage: VectorStorage,
	private val documentChunking: DocumentChunking,
) {
	fun add(request: KnowledgeDocumentRequest): KnowledgeDocumentResponse =
		vectorStorage.add(request, documentChunking.split(request.content))
}
