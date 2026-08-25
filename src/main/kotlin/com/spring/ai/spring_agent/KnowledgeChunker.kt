package com.spring.ai.spring_agent

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

fun interface KnowledgeChunker {
	fun split(content: String): List<String>
}

@Component
class ParagraphKnowledgeChunker(
	@Value("\${spring.rag.chunk.max-characters:1200}") private val maxCharacters: Int,
) : KnowledgeChunker {
	override fun split(content: String): List<String> {
		val paragraphs = content.trim()
			.split(Regex("\\n\\s*\\n"))
			.map(String::trim)
			.filter(String::isNotEmpty)

		return paragraphs.flatMap(::splitParagraph)
	}

	private fun splitParagraph(paragraph: String): List<String> {
		if (paragraph.length <= maxCharacters) return listOf(paragraph)

		return buildList {
			var start = 0
			while (start < paragraph.length) {
				var end = minOf(start + maxCharacters, paragraph.length)
				if (end < paragraph.length) {
					val sentenceBoundary = paragraph.lastIndexOfAny(charArrayOf('.', '!', '?', '。'), end - 1)
					if (sentenceBoundary >= start + maxCharacters / 2) end = sentenceBoundary + 1
				}
				add(paragraph.substring(start, end).trim())
				start = end
			}
		}
	}
}
