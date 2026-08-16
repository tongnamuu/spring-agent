package com.spring.ai.spring_agent

import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Service

fun interface ChatService {
	fun chat(message: String): String
}

@Service
class SpringAiChatService(chatClientBuilder: ChatClient.Builder) : ChatService {
	private val chatClient = chatClientBuilder.build()

	override fun chat(message: String): String =
		requireNotNull(
			chatClient.prompt()
				.user(message)
				.call()
				.content(),
		) { "Ollama returned an empty response" }
}
