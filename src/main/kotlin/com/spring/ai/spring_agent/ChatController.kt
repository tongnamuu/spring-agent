package com.spring.ai.spring_agent

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat")
class ChatController(private val chatService: ChatService) {
	@PostMapping(consumes = [MediaType.TEXT_PLAIN_VALUE], produces = [MediaType.TEXT_PLAIN_VALUE])
	fun chat(@RequestBody message: String): String = chatService.chat(message)
}
