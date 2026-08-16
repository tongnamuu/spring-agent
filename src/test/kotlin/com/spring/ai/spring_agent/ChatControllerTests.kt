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
			contentType = MediaType.TEXT_PLAIN
			accept = MediaType.TEXT_PLAIN
			content = "Hello"
		}.andExpect {
			status { isOk() }
			content {
				contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
				string("Hello from the fake service")
			}
		}

		assertEquals("Hello", chatService.lastMessage)
	}

	private class FakeChatService(private val response: String) : ChatService {
		var lastMessage: String? = null
			private set

		override fun chat(message: String): String {
			lastMessage = message
			return response
		}
	}
}
