# ModelRouter — API Specification

> **Base URL**: `http://localhost:8080`
> **Protocol**: All proxy endpoints follow the [Anthropic Messages API](https://docs.anthropic.com/en/api/messages) contract.

---

## 1. Proxy Endpoints (Client-Facing)

### POST `/v1/messages`

Send a chat request through the router to the currently active LLM provider.

**Headers**

| Header           | Required | Description                                           |
|------------------|----------|-------------------------------------------------------|
| `Content-Type`   | Yes      | `application/json`                                    |
| `x-api-key`      | No       | Any value (router ignores it; uses stored keys)       |

**Request Body**

```json
{
  "model": "claude-sonnet-4-20250514",
  "max_tokens": 1024,
  "messages": [
    { "role": "user", "content": "Hello, who are you?" }
  ],
  "system": "You are a helpful assistant.",
  "temperature": 0.7
}
```

| Field           | Type              | Required | Description                                         |
|-----------------|-------------------|----------|-----------------------------------------------------|
| `model`         | string            | Yes      | Ignored by router; kept for compatibility           |
| `messages`      | Message[]         | Yes      | Conversation history                                |
| `max_tokens`    | integer           | No       | Maximum response tokens (default: 1024)             |
| `system`        | string            | No       | System prompt                                       |
| `temperature`   | float             | No       | Sampling temperature (0.0–1.0)                      |
| `top_p`         | float             | No       | Nucleus sampling                                    |
| `top_k`         | integer           | No       | Top-K sampling (Gemini/Ollama only)                 |
| `stop_sequences`| string[]          | No       | Stop sequences (Anthropic only)                     |
| `stream`        | boolean           | No       | Not yet implemented; defaults to false              |

**Message Object**

```json
{ "role": "user", "content": "Hello" }
{ "role": "assistant", "content": "Hi there!" }
```

`content` can be a plain string or an array of content blocks:
```json
[{ "type": "text", "text": "Hello" }]
```

**Response Body** — identical regardless of active provider

```json
{
  "id": "msg_01XFDUDYJgAACzvnptvVoYEL",
  "type": "message",
  "role": "assistant",
  "content": [
    { "type": "text", "text": "Hi! I'm an AI assistant." }
  ],
  "model": "claude-sonnet-4-20250514",
  "stop_reason": "end_turn",
  "usage": {
    "input_tokens": 25,
    "output_tokens": 42
  }
}
```

**Error Response**

```json
{
  "error": {
    "type": "bad_gateway",
    "message": "Provider error: Anthropic API key is not configured"
  },
  "timestamp": "2025-05-14T12:00:00.000Z"
}
```

---

### GET `/v1/health`

```json
{ "status": "ok", "service": "ModelRouter" }
```

---

## 2. Admin Endpoints (UI-Facing)

### GET `/api/admin/config`

Returns the current router configuration. API keys are masked.

**Response**

```json
{
  "provider": "anthropic",
  "anthropicApiKey": "sk-a****key",
  "geminiApiKey": null,
  "geminiModel": "gemini-2.0-flash",
  "ollamaBaseUrl": "http://localhost:11434",
  "ollamaModel": "llama3",
  "masked": true
}
```

---

### PUT `/api/admin/config`

Partial update — only supplied non-blank fields are persisted.

**Request**

```json
{
  "anthropicApiKey": "sk-ant-api03-xxxx",
  "geminiApiKey": "AIzaSy-xxxx",
  "geminiModel": "gemini-1.5-pro",
  "ollamaBaseUrl": "http://192.168.1.10:11434",
  "ollamaModel": "mistral",
  "provider": "gemini"
}
```

All fields are optional. Include only what you want to change.

**Response** — updated config (masked).

---

### POST `/api/admin/provider/switch`

Atomically switch the active provider.

**Request**

```json
{ "provider": "gemini" }
```

`provider` must be one of: `"anthropic"`, `"gemini"`, `"ollama"`.

**Response** — updated config (masked).

---

### GET `/api/admin/providers`

```json
["anthropic", "gemini", "ollama"]
```

---

## 3. Error Codes

| HTTP Status | Meaning                                     |
|-------------|---------------------------------------------|
| 200         | Success                                     |
| 400         | Bad request (e.g. unknown provider name)    |
| 422         | Unprocessable (e.g. missing API key)        |
| 502         | Upstream provider error                     |

---

## 4. SDK / Client Examples

### cURL

```bash
curl -X POST http://localhost:8080/v1/messages \
  -H "Content-Type: application/json" \
  -H "x-api-key: any-value" \
  -d '{
    "model": "claude-sonnet-4-20250514",
    "max_tokens": 512,
    "messages": [{"role": "user", "content": "Tell me a joke"}]
  }'
```

### Python (using Anthropic SDK — zero changes needed)

```python
import anthropic

client = anthropic.Anthropic(
    api_key="any-value",          # router ignores this
    base_url="http://localhost:8080",
)

message = client.messages.create(
    model="claude-sonnet-4-20250514",
    max_tokens=1024,
    messages=[{"role": "user", "content": "Hello!"}],
)
print(message.content[0].text)
```

### JavaScript / Node.js (using Anthropic SDK)

```js
import Anthropic from '@anthropic-ai/sdk'

const client = new Anthropic({
  apiKey: 'any-value',
  baseURL: 'http://localhost:8080',
})

const msg = await client.messages.create({
  model: 'claude-sonnet-4-20250514',
  max_tokens: 1024,
  messages: [{ role: 'user', content: 'Hello!' }],
})

console.log(msg.content[0].text)
```

> **Key insight**: Because the router uses the Anthropic Messages API format, you can use the
> official `anthropic` SDK pointing at `http://localhost:8080` — with **any** API key value.
> The router uses its own stored keys for the actual upstream call.
