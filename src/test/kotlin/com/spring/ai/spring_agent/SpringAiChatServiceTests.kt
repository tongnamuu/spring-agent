package com.spring.ai.spring_agent

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
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
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.net.SocketTimeoutException
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class SpringAiChatServiceTests {

	private lateinit var wireMock: WireMockServer

	@BeforeEach
	fun startWireMock() {
		wireMock = WireMockServer(wireMockConfig().dynamicPort())
		wireMock.start()
	}

	@AfterEach
	fun stopWireMock() {
		if (::wireMock.isInitialized) {
			wireMock.stop()
		}
	}

	@Test
	fun `returns the Ollama API response`() {
		wireMock.stubFor(
			post(urlEqualTo("/api/chat"))
				.willReturn(
					aResponse()
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(ollamaResponse("Hello from WireMock")),
				),
		)
		val chatService = chatService(readTimeout = Duration.ofSeconds(1))

		val response = chatService.chat(listOf(ChatMessage(ChatRole.USER, "Hello")))

		assertEquals("Hello from WireMock", response)
		wireMock.verify(
			postRequestedFor(urlEqualTo("/api/chat"))
				.withRequestBody(matchingJsonPath("$.model", equalTo(TEST_MODEL)))
				.withRequestBody(matchingJsonPath("$.messages[0].content", equalTo("Hello"))),
		)
	}

	@Test
	fun `sends conversation history to the Ollama API in order`() {
		wireMock.stubFor(
			post(urlEqualTo("/api/chat"))
				.willReturn(
					aResponse()
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(ollamaResponse("Your name is Rook")),
				),
		)
		val chatService = chatService(readTimeout = Duration.ofSeconds(1))

		val response = chatService.chat(
			listOf(
				ChatMessage(ChatRole.SYSTEM, "Remember user details"),
				ChatMessage(ChatRole.USER, "My name is Rook"),
				ChatMessage(ChatRole.ASSISTANT, "Nice to meet you, Rook"),
				ChatMessage(ChatRole.USER, "What is my name?"),
			),
		)

		assertEquals("Your name is Rook", response)
		wireMock.verify(
			postRequestedFor(urlEqualTo("/api/chat"))
				.withRequestBody(matchingJsonPath("$.messages[0].role", equalTo("system")))
				.withRequestBody(matchingJsonPath("$.messages[0].content", equalTo("Remember user details")))
				.withRequestBody(matchingJsonPath("$.messages[1].role", equalTo("user")))
				.withRequestBody(matchingJsonPath("$.messages[1].content", equalTo("My name is Rook")))
				.withRequestBody(matchingJsonPath("$.messages[2].role", equalTo("assistant")))
				.withRequestBody(matchingJsonPath("$.messages[2].content", equalTo("Nice to meet you, Rook")))
				.withRequestBody(matchingJsonPath("$.messages[3].role", equalTo("user")))
				.withRequestBody(matchingJsonPath("$.messages[3].content", equalTo("What is my name?"))),
		)
	}

	@Test
	fun `fails when the Ollama API exceeds the read timeout`() {
		wireMock.stubFor(
			post(urlEqualTo("/api/chat"))
				.willReturn(
					aResponse()
						.withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
						.withBody(ollamaResponse("Too late"))
						.withFixedDelay(500),
				),
		)
		val chatService = chatService(readTimeout = Duration.ofMillis(100))

		val exception = assertFailsWith<RestClientException> {
			chatService.chat(listOf(ChatMessage(ChatRole.USER, "Wait for me")))
		}

		assertIs<SocketTimeoutException>(exception.rootCause())
		wireMock.verify(1, postRequestedFor(urlEqualTo("/api/chat")))
	}

	private fun chatService(readTimeout: Duration): SpringAiChatService {
		val requestFactory = SimpleClientHttpRequestFactory().apply {
			setReadTimeout(readTimeout)
		}
		val ollamaApi = OllamaApi.builder()
			.baseUrl(wireMock.baseUrl())
			.restClientBuilder(RestClient.builder().requestFactory(requestFactory))
			.build()
		val chatModel = OllamaChatModel.builder()
			.ollamaApi(ollamaApi)
			.options(OllamaChatOptions.builder().model(TEST_MODEL).build())
			.retryTemplate(RetryTemplate(RetryPolicy.withMaxRetries(0)))
			.build()

		return SpringAiChatService(ChatClient.builder(chatModel))
	}

	private fun ollamaResponse(content: String): String =
		"""
		{
		  "model": "$TEST_MODEL",
		  "created_at": "2026-08-17T00:00:00Z",
		  "message": {
		    "role": "assistant",
		    "content": "$content"
		  },
		  "done_reason": "stop",
		  "done": true
		}
		""".trimIndent()

	private fun Throwable.rootCause(): Throwable =
		generateSequence(this) { it.cause }
			.last()

	private companion object {
		const val TEST_MODEL = "wiremock-model"
	}
}
