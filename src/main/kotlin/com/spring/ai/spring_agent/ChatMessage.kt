package com.spring.ai.spring_agent

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

data class ChatRequest(val messages: List<ChatMessage>) {
	init {
		require(messages.isNotEmpty()) { "messages must not be empty" }
	}
}

data class ChatMessage(val role: ChatRole, val content: String) {
	init {
		require(content.isNotBlank()) { "message content must not be blank" }
	}
}

enum class ChatRole(@get:JsonValue val value: String) {
	SYSTEM("system"),
	USER("user"),
	ASSISTANT("assistant"),
	;

	companion object {
		@JvmStatic
		@JsonCreator
		fun fromValue(value: String): ChatRole =
			entries.firstOrNull { it.value == value }
				?: throw IllegalArgumentException("Unsupported chat role: $value")
	}
}
