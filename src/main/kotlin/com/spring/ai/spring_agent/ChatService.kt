package com.spring.ai.spring_agent

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.stereotype.Service

fun interface ChatService {
	fun chat(messages: List<ChatMessage>): String
}

@Service
class SpringAiChatService(chatClientBuilder: ChatClient.Builder) : ChatService {
	private val chatClient = chatClientBuilder.build()

	override fun chat(messages: List<ChatMessage>): String =
		requireNotNull(
			chatClient.prompt()
				.messages(messages.map { it.toSpringAiMessage() })
				.call()
				.content(),
		) { "Ollama returned an empty response" }

	private fun ChatMessage.toSpringAiMessage(): Message = when (role) {
		ChatRole.SYSTEM -> SystemMessage(content)
		ChatRole.USER -> UserMessage(content)
		ChatRole.ASSISTANT -> AssistantMessage(content)
	}
}
