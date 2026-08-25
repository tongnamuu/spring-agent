# spring-agent

Spring AI chat backed by local Ollama models and an Elasticsearch 8 retrieval-augmented knowledge base.

## Run locally

Prerequisites: Java 25, Docker, and Ollama.

```bash
ollama pull qwen3.5:9B
ollama pull qwen3-embedding:0.6b
docker compose up -d elasticsearch
OLLAMA_MODEL=qwen3.5:9B ./gradlew bootRun
```

The default embedding model is `qwen3-embedding:0.6b`. Override the chat model, embedding model, Elasticsearch URL, index name, dimensions, or similarity threshold with the environment variables documented in `application.properties`.

## Add knowledge

The topic is metadata rather than a fixed enum, so MySQL, Kubernetes, LLM, MLOps, refactoring, and later subjects use the same endpoint and index structure.

```bash
curl -sS -H 'Content-Type: application/json' \
  --data @examples/knowledge/dijkstra.json \
  http://localhost:8080/api/knowledge/documents

curl -sS -H 'Content-Type: application/json' \
  --data @examples/knowledge/mysql.json \
  http://localhost:8080/api/knowledge/documents

curl -sS -H 'Content-Type: application/json' \
  --data @examples/knowledge/kubernetes.json \
  http://localhost:8080/api/knowledge/documents
```

## Search

```bash
curl -sS -H 'Content-Type: application/json' \
  --data '{"query":"다익스트라는 어떻게 동작해?","topK":5}' \
  http://localhost:8080/api/knowledge/search
```

The relevance gate checks whether each vector candidate can correctly answer the query. For example, searching for `음의 가중치 최단거리` does not return the Dijkstra passage because its non-negative-weight limitation conflicts with the query.

## RAG chat

```bash
curl -sS -H 'Content-Type: application/json' -H 'Accept: text/plain' \
  --data '{"messages":[{"role":"user","content":"InnoDB 보조 인덱스의 리프에는 무엇이 저장돼?"}]}' \
  http://localhost:8080/api/chat
```

Retrieved passages are placed before the conversation history as grounded context, and the model is instructed to cite their titles.

## Test

```bash
./gradlew clean test
```

See [the embedding model decision](docs/embedding-model-selection.md) for the candidate comparison and retrieval-quality rationale.
