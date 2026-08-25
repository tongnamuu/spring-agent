# Embedding model selection

## Decision

Use `qwen3-embedding:0.6b` through the local Ollama instance with 1024-dimensional cosine vectors in Elasticsearch.

## Candidates

| Model | Ollama size | Dimensions | Context | Multilingual and code retrieval | Decision |
| --- | ---: | ---: | ---: | --- | --- |
| `qwen3-embedding:0.6b` | 639 MB | 1024 | 32K | 100+ languages and programming-language retrieval | Selected |
| `bge-m3` | 1.2 GB | 1024 | 8K | 100+ languages | Good quality, but about twice the local model size |
| `nomic-embed-text` | 274 MB | 768 | 2K in the Ollama package | Smallest, but less suitable for long multilingual technical material |

The selected model provides the best balance for a knowledge base that mixes Korean, English, and code-related terminology. Its dimensions also match the Elasticsearch index setting in `application.properties`. Changing the embedding model or dimensions requires creating a new index and re-embedding the stored documents.

Sources: [Qwen3 Embedding](https://ollama.com/library/qwen3-embedding), [Qwen3 model table](https://github.com/QwenLM/Qwen3-Embedding), [BGE-M3](https://ollama.com/library/bge-m3), [Nomic Embed Text](https://ollama.com/library/nomic-embed-text).

## Local retrieval check

The following cosine scores were measured with `qwen3-embedding:0.6b`, three passages (Dijkstra, MySQL, Kubernetes), and the query instruction used by the application.

| Query | Dijkstra | MySQL | Kubernetes |
| --- | ---: | ---: | ---: |
| How does Dijkstra find a shortest path? | 0.7960 | 0.2036 | 0.1750 |
| shortest paths with negative edge weights | 0.6986 | 0.1400 | 0.1127 |
| MySQL secondary index leaf nodes | 0.1990 | 0.6612 | 0.2027 |
| 쿠버네티스 롤링 업데이트 | 0.1708 | 0.1355 | 0.3850 |

The negative-weight query demonstrates why vector similarity alone is insufficient: it is semantically close to the Dijkstra passage even though Dijkstra cannot solve that problem. The application therefore retrieves vector candidates with a 0.35 threshold and then applies a strict LLM relevance gate. A passage is excluded when its limitations conflict with the query.
