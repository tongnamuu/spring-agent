package com.spring.ai.spring_agent

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import kotlin.test.assertEquals

class ChatControllerTests {

	@Test
	fun `returns the fake service response as plain text`() {
		val chatService = FakeChatService(response = "Hello from the fake service")
		val mockMvc = MockMvcBuilders.standaloneSetup(ChatController(chatService)).build()

		mockMvc.post("/api/chat") {
			contentType = MediaType.APPLICATION_JSON
			accept = MediaType.TEXT_PLAIN
			content =
				"""
				{
				  "messages": [
				    { "role": "system", "content": "Remember user details" },
				    { "role": "user", "content": "My name is Rook" },
				    { "role": "assistant", "content": "Nice to meet you, Rook" },
				    { "role": "user", "content": "What is my name?" }
				  ]
				}
				""".trimIndent()
		}.andExpect {
			status { isOk() }
			content {
				contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
				string("Hello from the fake service")
			}
		}

		assertEquals(
			listOf(
				ChatMessage(ChatRole.SYSTEM, "Remember user details"),
				ChatMessage(ChatRole.USER, "My name is Rook"),
				ChatMessage(ChatRole.ASSISTANT, "Nice to meet you, Rook"),
				ChatMessage(ChatRole.USER, "What is my name?"),
			),
			chatService.lastMessages,
		)
	}

	private class FakeChatService(private val response: String) : ChatService {
		var lastMessages: List<ChatMessage>? = null
			private set

		override fun chat(messages: List<ChatMessage>): String {
			lastMessages = messages
			return response
		}
	}
}
