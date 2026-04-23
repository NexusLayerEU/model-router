# ModelRouter — Integration Guide

## Overview

This guide explains how to integrate your existing products and services with ModelRouter
so they can switch between LLM providers transparently.

---

## The Golden Rule

> Point your Anthropic SDK (or raw HTTP calls) at **`http://localhost:8080`** instead of
> `https://api.anthropic.com`. Use **any string** as the API key. Done.

The router exposes the exact same `POST /v1/messages` API as Anthropic, so no code changes
are needed beyond the `baseURL` and `apiKey` config.

---

## Integration by Language / SDK

### Python — Anthropic SDK

```python
import anthropic

# Before (direct Anthropic):
# client = anthropic.Anthropic(api_key="sk-ant-...")

# After (through ModelRouter):
client = anthropic.Anthropic(
    api_key="router",            # any value — router ignores this
    base_url="http://localhost:8080",
)

response = client.messages.create(
    model="claude-sonnet-4-20250514",   # router ignores the model name too
    max_tokens=1024,
    messages=[{"role": "user", "content": "Hello!"}],
)

print(response.content[0].text)
```

### Node.js / TypeScript — Anthropic SDK

```typescript
import Anthropic from '@anthropic-ai/sdk'

const client = new Anthropic({
  apiKey: 'router',
  baseURL: 'http://localhost:8080',
})

const message = await client.messages.create({
  model: 'claude-sonnet-4-20250514',
  max_tokens: 1024,
  messages: [{ role: 'user', content: 'Hello!' }],
})

console.log(message.content[0].text)
```

### Go — raw HTTP

```go
package main

import (
    "bytes"
    "encoding/json"
    "fmt"
    "net/http"
)

func main() {
    body, _ := json.Marshal(map[string]any{
        "model":      "claude-sonnet-4-20250514",
        "max_tokens": 1024,
        "messages":   []map[string]string{{"role": "user", "content": "Hello!"}},
    })

    req, _ := http.NewRequest("POST", "http://localhost:8080/v1/messages", bytes.NewBuffer(body))
    req.Header.Set("Content-Type", "application/json")
    req.Header.Set("x-api-key", "router")

    resp, _ := http.DefaultClient.Do(req)
    defer resp.Body.Close()
    // parse resp.Body as MessagesResponse
}
```

### Java — Spring WebClient

```java
WebClient client = WebClient.builder()
    .baseUrl("http://localhost:8080")
    .defaultHeader("x-api-key", "router")
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
```

### cURL

```bash
curl -s -X POST http://localhost:8080/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: router" \
  -d '{
    "model": "claude-sonnet-4-20250514",
    "max_tokens": 512,
    "messages": [{"role": "user", "content": "Tell me a joke"}]
  }' | jq '.content[0].text'
```

---

## Environment Variable Pattern

Store the router URL in an environment variable so you can switch between the router
and the real Anthropic endpoint without code changes:

```bash
# .env (local development — via router)
ANTHROPIC_BASE_URL=http://localhost:8080
ANTHROPIC_API_KEY=router

# .env.production (direct — if you ever bypass the router)
ANTHROPIC_BASE_URL=https://api.anthropic.com
ANTHROPIC_API_KEY=sk-ant-real-key-here
```

```python
import os
import anthropic

client = anthropic.Anthropic(
    api_key=os.environ["ANTHROPIC_API_KEY"],
    base_url=os.environ.get("ANTHROPIC_BASE_URL"),  # None → uses Anthropic default
)
```

---

## Switching Providers via API

You can also switch providers programmatically (e.g. from a CI pipeline or another service):

```bash
# Switch to Gemini
curl -X POST http://localhost:8080/api/admin/provider/switch \
  -H "Content-Type: application/json" \
  -d '{"provider": "gemini"}'

# Switch to Ollama
curl -X POST http://localhost:8080/api/admin/provider/switch \
  -H "Content-Type: application/json" \
  -d '{"provider": "ollama"}'

# Get current status
curl http://localhost:8080/api/admin/config
```

---

## Conversation History

The router is **stateless** — it does not store conversation history. Your client is
responsible for maintaining the `messages` array and appending the assistant's response
before the next turn:

```python
messages = []

def chat(user_input):
    messages.append({"role": "user", "content": user_input})
    response = client.messages.create(
        model="claude-sonnet-4-20250514",
        max_tokens=1024,
        messages=messages,
    )
    assistant_text = response.content[0].text
    messages.append({"role": "assistant", "content": assistant_text})
    return assistant_text
```

---

## System Prompts

Pass system prompts in the `system` field — all three providers support this:

```python
response = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    system="You are a senior software engineer specializing in Python.",
    messages=[{"role": "user", "content": "Review this code..."}],
)
```

> **Note for Gemini**: The router automatically converts the `system` field into an
> alternating user/model turn pair at the beginning of the conversation, since Gemini's
> API does not have a native system prompt field.

---

## Limitations & Compatibility Notes

| Feature         | Anthropic | Gemini | Ollama | Notes                                     |
|-----------------|-----------|--------|--------|-------------------------------------------|
| Text chat       | ✅         | ✅      | ✅      | Full support                              |
| System prompts  | ✅         | ✅*     | ✅      | *Converted to user turn for Gemini        |
| Multi-turn      | ✅         | ✅      | ✅      | Pass full history in `messages[]`         |
| Tool use        | ✅         | ❌      | ❌      | Tools passed through to Anthropic only    |
| Vision / images | ✅         | ❌      | ❌      | Not yet implemented in router             |
| Streaming       | ❌         | ❌      | ❌      | Planned — router returns full response    |
| Token counts    | ✅         | ✅      | ✅*     | *Ollama returns eval_count                |
