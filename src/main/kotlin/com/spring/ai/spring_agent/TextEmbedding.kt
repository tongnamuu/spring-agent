package com.spring.ai.spring_agent

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Component

fun interface TextEmbedding {
	fun embed(texts: List<String>): List<FloatArray>
}

@Component
class SpringAiTextEmbedding(private val embeddingModel: EmbeddingModel) : TextEmbedding {
	override fun embed(texts: List<String>): List<FloatArray> = embeddingModel.embed(texts)
}
