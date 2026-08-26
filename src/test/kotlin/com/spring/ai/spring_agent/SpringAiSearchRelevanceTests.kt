package com.spring.ai.spring_agent

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.containing
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.ai.ollama.api.OllamaChatOptions
import org.springframework.core.retry.RetryPolicy
import org.springframework.core.retry.RetryTemplate
import org.springframework.http.MediaType
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpringAiSearchRelevanceTests {
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
	fun `accepts an applicable passage`() {
		stubDecision("RELEVANT")
		val evaluator = evaluator()

		val relevant = evaluator.isRelevant("다익스트라는 어떻게 동작해?", dijkstra())

		assertTrue(relevant)
		wireMock.verify(
			postRequestedFor(urlEqualTo("/api/chat"))
				.withRequestBody(matchingJsonPath("$.think", equalTo("false")))
				.withRequestBody(matchingJsonPath("$.messages[0].role", equalTo("system")))
				.withRequestBody(matchingJsonPath("$.messages[1].content", containing("다익스트라는 어떻게 동작해?")))
				.withRequestBody(matchingJsonPath("$.messages[1].content", containing("다익스트라 알고리즘")))
				.withRequestBody(matchingJsonPath("$.messages[1].content", containing("비음수 가중치"))),
		)
	}

	@Test
	fun `rejects a passage whose applicability conflicts with the query`() {
		stubDecision("IRRELEVANT")
		val evaluator = evaluator()

		val relevant = evaluator.isRelevant("음의 가중치 최단거리", dijkstra())

		assertFalse(relevant)
	}

	private fun evaluator(): SpringAiSearchRelevance {
		val ollamaApi = OllamaApi.builder().baseUrl(wireMock.baseUrl()).build()
		val chatModel = OllamaChatModel.builder()
			.ollamaApi(ollamaApi)
			.options(OllamaChatOptions.builder().model(TEST_MODEL).build())
			.retryTemplate(RetryTemplate(RetryPolicy.withMaxRetries(0)))
			.build()
		return SpringAiSearchRelevance(ChatClient.builder(chatModel))
	}

	private fun stubDecision(decision: String) {
		wireMock.stubFor(
			post(urlEqualTo("/api/chat"))
				.willReturn(
					aResponse()
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(
							"""
							{
							  "model": "$TEST_MODEL",
							  "message": { "role": "assistant", "content": "$decision" },
							  "done": true
							}
							""".trimIndent(),
						),
				),
		)
	}

	private fun dijkstra() = KnowledgeSearchResult(
		id = "dijkstra-1",
		documentId = "dijkstra",
		title = "다익스트라 알고리즘",
		topic = "algorithms",
		source = "알고리즘 교재 노트",
		content = "다익스트라는 비음수 가중치 그래프에서만 올바른 최단거리를 구한다.",
		score = 0.9,
	)

	private companion object {
		const val TEST_MODEL = "wiremock-model"
	}
}
