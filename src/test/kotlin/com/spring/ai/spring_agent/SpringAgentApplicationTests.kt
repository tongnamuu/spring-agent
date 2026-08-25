package com.spring.ai.spring_agent

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["spring.ai.vectorstore.elasticsearch.initialize-schema=false"])
class SpringAgentApplicationTests {
	@Test
	fun contextLoads() {
	}

}
