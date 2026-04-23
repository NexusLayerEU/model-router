# ModelRouter — Integration Reference

> **Status**: Live on staging  
> **API endpoint**: `http://192.168.68.111:8082/v1/messages`  
> **Admin UI**: `http://192.168.68.111:3002`  
> **Health**: `http://192.168.68.111:8082/v1/health`

---

## What is ModelRouter?

ModelRouter is a local proxy that speaks the **Anthropic Messages API** protocol on one side,
and routes your traffic to whichever LLM provider is currently active (Anthropic Claude,
Google Gemini, or Ollama) on the other side.

**You never change your code when switching providers.** Only the router config changes.

```
Your code  →  POST /v1/messages  →  ModelRouter  →  Active Provider (Claude / Gemini / Ollama)
```

---

## Quick-start (30 seconds)

### 1. Verify the router is up

```bash
curl http://192.168.68.111:8082/v1/health
# {"status":"ok","service":"ModelRouter"}
```

### 2. Send your first message

```bash
curl -X POST http://192.168.68.111:8082/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: any-value" \
  -d '{
    "model": "claude-sonnet-4-20250514",
    "max_tokens": 256,
    "messages": [{ "role": "user", "content": "Reply with one word: working" }]
  }'
```

Expected response shape:
```json
{
  "id": "...",
  "type": "message",
  "role": "assistant",
  "content": [{ "type": "text", "text": "Working" }],
  "model": "claude-sonnet-4-20250514",
  "stop_reason": "end_turn",
  "usage": { "input_tokens": 14, "output_tokens": 2 }
}
```

---

## Integration by language

### Python — Anthropic SDK (recommended)

```python
import anthropic

client = anthropic.Anthropic(
    api_key="router",                    # any non-empty string
    base_url="http://192.168.68.111:8082",
)

response = client.messages.create(
    model="claude-sonnet-4-20250514",   # model field is accepted but ignored by router
    max_tokens=1024,
    messages=[{"role": "user", "content": "Hello!"}],
)

print(response.content[0].text)
```

With environment variables (recommended for portability):

```bash
# .env
ANTHROPIC_API_KEY=router
ANTHROPIC_BASE_URL=http://192.168.68.111:8082
```

```python
import os
import anthropic

client = anthropic.Anthropic(
    api_key=os.environ["ANTHROPIC_API_KEY"],
    base_url=os.environ["ANTHROPIC_BASE_URL"],
)
```

---

### Node.js / TypeScript — Anthropic SDK

```bash
npm install @anthropic-ai/sdk
```

```typescript
import Anthropic from '@anthropic-ai/sdk'

const client = new Anthropic({
  apiKey: process.env.ANTHROPIC_API_KEY ?? 'router',
  baseURL: process.env.ANTHROPIC_BASE_URL ?? 'http://192.168.68.111:8082',
})

const message = await client.messages.create({
  model: 'claude-sonnet-4-20250514',
  max_tokens: 1024,
  messages: [{ role: 'user', content: 'Hello!' }],
})

console.log(message.content[0].text)
```

---

### Java — Spring WebClient

```java
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

WebClient client = WebClient.builder()
    .baseUrl("http://192.168.68.111:8082")
    .defaultHeader("x-api-key", "router")
    .defaultHeader("Content-Type", "application/json")
    .build();

Map<String, Object> body = Map.of(
    "model", "claude-sonnet-4-20250514",
    "max_tokens", 1024,
    "messages", List.of(Map.of("role", "user", "content", "Hello!"))
);

JsonNode response = client.post()
    .uri("/v1/messages")
    .bodyValue(body)
    .retrieve()
    .bodyToMono(JsonNode.class)
    .block();

String text = response.path("content").get(0).path("text").asText();
System.out.println(text);
```

---

### Go — net/http

```go
package main

import (
    "bytes"
    "encoding/json"
    "fmt"
    "io"
    "net/http"
)

func main() {
    payload, _ := json.Marshal(map[string]any{
        "model":      "claude-sonnet-4-20250514",
        "max_tokens": 1024,
        "messages":   []map[string]string{{"role": "user", "content": "Hello!"}},
    })

    req, _ := http.NewRequest("POST", "http://192.168.68.111:8082/v1/messages", bytes.NewBuffer(payload))
    req.Header.Set("Content-Type", "application/json")
    req.Header.Set("x-api-key", "router")

    resp, _ := http.DefaultClient.Do(req)
    defer resp.Body.Close()
    body, _ := io.ReadAll(resp.Body)
    fmt.Println(string(body))
}
```

---

### cURL (shell scripts / CI)

```bash
# One-liner helper function
router_chat() {
  local message="$1"
  curl -sf -X POST http://192.168.68.111:8082/v1/messages \
    -H "Content-Type: application/json" \
    -H "x-api-key: router" \
    -d "{
      \"model\": \"claude-sonnet-4-20250514\",
      \"max_tokens\": 1024,
      \"messages\": [{\"role\": \"user\", \"content\": \"${message}\"}]
    }" | jq -r '.content[0].text'
}

# Usage
router_chat "Summarise this PR in one sentence"
router_chat "Generate a commit message for: fixed null pointer in auth module"
```

---

## Integration for AI Agents & CLI tools

### Environment variable convention

Set these two variables in your agent's environment and it will transparently use the router:

```bash
export ANTHROPIC_API_KEY=router
export ANTHROPIC_BASE_URL=http://192.168.68.111:8082
```

Any tool that uses the official Anthropic SDK will automatically pick these up.

### LangChain (Python)

```python
from langchain_anthropic import ChatAnthropic

llm = ChatAnthropic(
    model="claude-sonnet-4-20250514",
    anthropic_api_key="router",
    anthropic_api_url="http://192.168.68.111:8082",
)

response = llm.invoke("Hello!")
print(response.content)
```

### LangChain (Node.js)

```typescript
import { ChatAnthropic } from '@langchain/anthropic'

const model = new ChatAnthropic({
  model: 'claude-sonnet-4-20250514',
  anthropicApiKey: 'router',
  clientOptions: { baseURL: 'http://192.168.68.111:8082' },
})

const response = await model.invoke('Hello!')
console.log(response.content)
```

### OpenAI-compatible clients (via /v1/messages)

The router exposes the **Anthropic** Messages API — not the OpenAI Chat Completions API.
If your tool only supports OpenAI format, you have two options:

1. **Preferred**: Switch to the Anthropic SDK (direct drop-in, same concepts).
2. **Workaround**: Wrap with a lightweight adapter (e.g. `litellm` in proxy mode).

```bash
# litellm adapter (if you must use OpenAI format)
pip install litellm
litellm --model anthropic/claude-sonnet-4-20250514 \
        --api_base http://192.168.68.111:8082 \
        --api_key router
# → Now exposes an OpenAI-compatible endpoint on localhost:4000
```

---

## Admin API — switch providers programmatically

Agents and CI pipelines can switch the active provider without opening a browser:

```bash
# Check current provider
curl -s http://192.168.68.111:8082/api/admin/config | jq '{provider, geminiModel, ollamaModel}'

# Switch to Gemini
curl -s -X POST http://192.168.68.111:8082/api/admin/provider/switch \
  -H "Content-Type: application/json" \
  -d '{"provider": "gemini"}' | jq .provider

# Switch to Ollama (local, zero cost)
curl -s -X POST http://192.168.68.111:8082/api/admin/provider/switch \
  -H "Content-Type: application/json" \
  -d '{"provider": "ollama"}' | jq .provider

# Switch back to Anthropic
curl -s -X POST http://192.168.68.111:8082/api/admin/provider/switch \
  -H "Content-Type: application/json" \
  -d '{"provider": "anthropic"}' | jq .provider
```

### Save API keys (first-time setup)

```bash
# Set Anthropic key
curl -s -X PUT http://192.168.68.111:8082/api/admin/config \
  -H "Content-Type: application/json" \
  -d '{"anthropicApiKey": "sk-ant-YOUR-KEY-HERE"}' | jq .anthropicApiKey

# Set Gemini key + model
curl -s -X PUT http://192.168.68.111:8082/api/admin/config \
  -H "Content-Type: application/json" \
  -d '{"geminiApiKey": "AIzaSy-YOUR-KEY-HERE", "geminiModel": "gemini-2.0-flash"}' | jq '{geminiApiKey, geminiModel}'

# Configure Ollama (already running on this server at :11434)
curl -s -X PUT http://192.168.68.111:8082/api/admin/config \
  -H "Content-Type: application/json" \
  -d '{"ollamaBaseUrl": "http://192.168.68.111:11434", "ollamaModel": "llama3"}' | jq '{ollamaBaseUrl, ollamaModel}'
```

---

## Request format reference

```
POST http://192.168.68.111:8082/v1/messages
Content-Type: application/json
x-api-key: <any string>

{
  "model":      string,          // accepted, ignored — router uses configured model
  "messages":   Message[],       // required
  "max_tokens": integer,         // optional, default 1024
  "system":     string,          // optional system prompt
  "temperature": float,          // optional 0.0–1.0
  "top_p":      float,           // optional
  "top_k":      integer,         // optional (Gemini/Ollama only)
  "stream":     false            // streaming not yet supported — always false
}

Message = { "role": "user" | "assistant", "content": string | ContentBlock[] }
ContentBlock = { "type": "text", "text": string }
```

Response always in Anthropic format:

```json
{
  "id":          "string",
  "type":        "message",
  "role":        "assistant",
  "content":     [{ "type": "text", "text": "..." }],
  "model":       "string",
  "stop_reason": "end_turn",
  "usage":       { "input_tokens": N, "output_tokens": N }
}
```

---

## Provider capability matrix

| Feature            | Anthropic | Gemini | Ollama | Notes                               |
|--------------------|-----------|--------|--------|-------------------------------------|
| Text chat          | ✅         | ✅      | ✅      | All providers                       |
| System prompts     | ✅         | ✅*     | ✅      | *Gemini: auto-converted to user turn|
| Multi-turn history | ✅         | ✅      | ✅      | Pass full `messages[]` array        |
| Token counts       | ✅         | ✅      | ✅      | Mapped to `usage.input/output_tokens`|
| Tool / function    | ✅         | ❌      | ❌      | Anthropic only; passed through      |
| Streaming          | ❌         | ❌      | ❌      | Planned — returns full response     |
| Vision / images    | ❌         | ❌      | ❌      | Planned                             |

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `connection refused` on :8082 | Check containers: `ssh thomas@192.168.68.111 'docker ps'` |
| `422 Unprocessable` + "API key not configured" | Set key via Admin UI or `/api/admin/config` PUT |
| `502 Bad Gateway` from Gemini/Anthropic | Verify the upstream API key is valid |
| Ollama returns empty text | Ensure model is pulled: `ollama pull llama3` on the server |
| Response is slow | Switch to Ollama (local, no network latency) or Gemini Flash |

---

## Deploy / redeploy

```bash
# From your local machine (ModelRouter project root):
./deploy-staging.sh
```

Redeploy takes ~2 min (Maven build cached after first run). Config (API keys, active provider)
is persisted in a Docker volume (`modelrouter-data`) and survives redeployments.

---

## Container management

```bash
SSH_TARGET="thomas@192.168.68.111"

# Status
ssh $SSH_TARGET 'docker compose -f ~/ModelRouter/docker-compose.yml ps'

# Live logs
ssh $SSH_TARGET 'docker logs modelrouter-backend -f'

# Restart
ssh $SSH_TARGET 'docker compose -f ~/ModelRouter/docker-compose.yml restart'

# Stop
ssh $SSH_TARGET 'docker compose -f ~/ModelRouter/docker-compose.yml down'
```
