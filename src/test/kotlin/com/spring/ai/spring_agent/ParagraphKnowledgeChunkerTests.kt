package com.spring.ai.spring_agent

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParagraphKnowledgeChunkerTests {

	@Test
	fun `keeps short paragraphs as separate chunks`() {
		val chunker = ParagraphKnowledgeChunker(maxCharacters = 100)

		val chunks = chunker.split("First paragraph.\n\nSecond paragraph.")

		assertEquals(listOf("First paragraph.", "Second paragraph."), chunks)
	}

	@Test
	fun `splits an oversized paragraph without losing content`() {
		val chunker = ParagraphKnowledgeChunker(maxCharacters = 30)
		val content = "Dijkstra selects a vertex. It then relaxes adjacent edges."

		val chunks = chunker.split(content)

		assertTrue(chunks.size > 1)
		assertEquals(content.replace(" ", ""), chunks.joinToString("").replace(" ", ""))
	}
}
