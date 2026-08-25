package com.spring.ai.spring_agent

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/knowledge")
class KnowledgeController(private val knowledgeService: KnowledgeService) {
	@PostMapping("/documents", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
	@ResponseStatus(HttpStatus.CREATED)
	fun add(@RequestBody request: KnowledgeDocumentRequest): KnowledgeDocumentResponse = knowledgeService.add(request)

	@PostMapping("/search", consumes = [MediaType.APPLICATION_JSON_VALUE], produces = [MediaType.APPLICATION_JSON_VALUE])
	fun search(@RequestBody request: KnowledgeSearchRequest): List<KnowledgeSearchResult> =
		request.topK
			?.let { knowledgeService.search(request.query, it) }
			?: knowledgeService.search(request.query)
}
