# ModelRouter — Usage Guide

ModelRouter is a local LLM traffic router that exposes a **Claude-compatible API** (`POST /v1/messages`) and routes requests to Anthropic Claude, Google Gemini, or a local Ollama instance — switchable at runtime from the UI or API. Drop it in front of any product that speaks the Anthropic Messages protocol and gain provider portability, cost control, and a single configuration point.

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [API Reference](#api-reference)
3. [Provider Configuration](#provider-configuration)
4. [Integration Examples](#integration-examples)
   - [Anthropic Python SDK](#anthropic-python-sdk)
   - [OpenAI SDK (compatible mode)](#openai-sdk-compatible-mode)
   - [FlowMesh + ModelRouter](#flowmesh--modelrouter)
   - [BrainVault + ModelRouter](#brainvault--modelrouter)
   - [PeriodAI + ModelRouter](#periodai--modelrouter)
   - [AgentVault — store and inject provider keys](#agentvault--store-and-inject-provider-keys)
5. [Cost Optimization Guide](#cost-optimization-guide)
6. [Failover Configuration](#failover-configuration)

---

## Quick Start

### 1 — Open the UI and configure a provider

Navigate to **http://192.168.68.111:3002** and enter your API key for the provider you want to activate (Anthropic, Gemini, or point to a local Ollama URL).

### 2 — Send a test message

```bash
curl -s -X POST http://192.168.68.111:8082/api/test \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, ModelRouter!"}'
```

### 3 — Point your products at ModelRouter

Replace any `https://api.anthropic.com` base URL in your services with `http://192.168.68.111:8082`. Everything else — headers, request body, response shape — stays identical.

### 4 — Switch providers instantly

```bash
# Switch to Gemini at runtime — no restarts required
curl -X PUT http://192.168.68.111:8082/api/config \
  -H "Content-Type: application/json" \
  -d '{"provider": "gemini", "geminiKey": "AIza..."}'
```

---

## API Reference

All endpoints are served at **http://192.168.68.111:8082**.

### POST /v1/messages

Proxy an LLM request. Accepts the **Anthropic Messages API** format and transparently forwards to the active provider, translating the request/response as needed.

**Request**

```http
POST /v1/messages
Content-Type: application/json
x-api-key: any-value          # pass-through; ModelRouter uses its stored keys
anthropic-version: 2023-06-01
```

```json
{
  "model": "claude-3-5-sonnet-20241022",
  "max_tokens": 1024,
  "messages": [
    { "role": "user", "content": "Explain vector embeddings in one paragraph." }
  ]
}
```

**Response** — standard Anthropic `Message` object regardless of active provider.

```json
{
  "id": "msg_01XFDUDYJgAACzvnptvVoYEL",
  "type": "message",
  "role": "assistant",
  "content": [{ "type": "text", "text": "Vector embeddings are..." }],
  "model": "claude-3-5-sonnet-20241022",
  "stop_reason": "end_turn",
  "usage": { "input_tokens": 18, "output_tokens": 102 }
}
```

---

### GET /api/config

Returns the currently active configuration.

```bash
curl http://192.168.68.111:8082/api/config
```

```json
{
  "provider": "anthropic",
  "model": "claude-3-5-sonnet-20241022",
  "anthropicKeySet": true,
  "geminiKeySet": false,
  "ollamaBaseUrl": "http://localhost:11434",
  "ollamaModel": "llama3.2"
}
```

---

### PUT /api/config

Update configuration. Only send the fields you want to change.

| Field           | Type   | Description                                    |
|-----------------|--------|------------------------------------------------|
| `provider`      | string | `"anthropic"` \| `"gemini"` \| `"ollama"`      |
| `anthropicKey`  | string | Anthropic API key (`sk-ant-...`)               |
| `geminiKey`     | string | Google AI Studio key (`AIza...`)               |
| `ollamaBaseUrl` | string | Ollama server base URL (default: `http://localhost:11434`) |
| `ollamaModel`   | string | Ollama model tag (e.g. `llama3.2`, `mistral`)  |

```bash
curl -X PUT http://192.168.68.111:8082/api/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "ollama",
    "ollamaBaseUrl": "http://192.168.68.111:11434",
    "ollamaModel": "llama3.2"
  }'
```

---

### GET /api/providers

List all configured providers and their readiness.

```bash
curl http://192.168.68.111:8082/api/providers
```

```json
{
  "providers": [
    { "id": "anthropic", "configured": true,  "active": true  },
    { "id": "gemini",    "configured": true,  "active": false },
    { "id": "ollama",    "configured": true,  "active": false }
  ]
}
```

---

### POST /api/test

Send a one-shot test message using the active provider.

```bash
curl -X POST http://192.168.68.111:8082/api/test \
  -H "Content-Type: application/json" \
  -d '{"message": "Ping!"}'
```

```json
{ "response": "Hello! I'm ready.", "provider": "anthropic", "latencyMs": 843 }
```

---

## Provider Configuration

### Anthropic Claude

```bash
curl -X PUT http://192.168.68.111:8082/api/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "anthropic",
    "anthropicKey": "sk-ant-api03-..."
  }'
```

Supported models (set `model` in each `/v1/messages` request body):

| Model                       | Context | Best for                          |
|-----------------------------|---------|-----------------------------------|
| `claude-3-5-sonnet-20241022`| 200 k   | Balance of speed and quality      |
| `claude-3-5-haiku-20241022` | 200 k   | Low-latency, cost-sensitive tasks |
| `claude-3-opus-20240229`    | 200 k   | Complex reasoning, long documents |

---

### Google Gemini

```bash
curl -X PUT http://192.168.68.111:8082/api/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "gemini",
    "geminiKey": "AIzaSy..."
  }'
```

Supported models:

| Model                  | Context | Best for                     |
|------------------------|---------|------------------------------|
| `gemini-1.5-pro`       | 1 M     | Huge context, multimodal     |
| `gemini-1.5-flash`     | 1 M     | Fast and cheap at scale      |
| `gemini-2.0-flash-exp` | 1 M     | Latest experimental features |

---

### Ollama (local)

Install Ollama on any machine in the LAN and pull the model you want:

```bash
curl -fsSL https://ollama.com/install.sh | sh
ollama pull llama3.2        # or mistral, codestral, phi3, etc.
```

Configure ModelRouter to use it:

```bash
curl -X PUT http://192.168.68.111:8082/api/config \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "ollama",
    "ollamaBaseUrl": "http://192.168.68.111:11434",
    "ollamaModel": "llama3.2"
  }'
```

---

## Integration Examples

### Anthropic Python SDK

The Anthropic SDK lets you override the `base_url`. Set it to ModelRouter and set `api_key` to any non-empty string — ModelRouter authenticates with providers using its own stored keys.

```python
import anthropic

client = anthropic.Anthropic(
    base_url="http://192.168.68.111:8082",
    api_key="model-router",  # arbitrary; ModelRouter uses its configured keys
)

message = client.messages.create(
    model="claude-3-5-sonnet-20241022",
    max_tokens=1024,
    messages=[
        {"role": "user", "content": "What is RAG and why does it matter?"}
    ],
)

print(message.content[0].text)
```

To switch providers mid-workflow without changing application code:

```python
import httpx

def switch_provider(provider: str, **kwargs):
    httpx.put(
        "http://192.168.68.111:8082/api/config",
        json={"provider": provider, **kwargs},
    )

# Use Gemini for a batch job, then switch back
switch_provider("gemini", geminiKey="AIza...")
run_batch_job()
switch_provider("anthropic")
```

---

### OpenAI SDK (compatible mode)

ModelRouter translates the Anthropic wire format internally. If your application uses the **OpenAI Python SDK**, point it at ModelRouter the same way — it will speak Anthropic format under the hood.

> **Note:** Use the `openai` package only if your integration layer already translates OpenAI ↔ Anthropic format. Prefer the Anthropic SDK for direct compatibility.

```python
from openai import OpenAI

# Works when ModelRouter has an OpenAI-compat shim layer
client = OpenAI(
    base_url="http://192.168.68.111:8082/v1",
    api_key="model-router",
)

response = client.chat.completions.create(
    model="claude-3-5-sonnet-20241022",
    messages=[{"role": "user", "content": "Summarise the CAP theorem."}],
)

print(response.choices[0].message.content)
```

---

### FlowMesh + ModelRouter

FlowMesh **LLM nodes** read their backend URL from the `LLM_BASE_URL` environment variable. Set it in your FlowMesh `.env`:

```env
LLM_BASE_URL=http://192.168.68.111:8082
LLM_API_KEY=model-router
```

Then any FlowMesh pipeline that contains an `LLM` node will route its inference calls through ModelRouter. Switching providers in ModelRouter instantly affects all running pipelines — no pipeline redeploy required.

```yaml
# flowmesh/pipelines/summarise.yaml
nodes:
  - id: fetch_docs
    type: HTTP_GET
    url: "{{input.source_url}}"

  - id: summarise
    type: LLM
    model: claude-3-5-haiku-20241022
    prompt: |
      Summarise the following document in 3 bullet points:
      {{nodes.fetch_docs.body}}

  - id: notify
    type: WEBHOOK
    url: "{{env.SLACK_WEBHOOK}}"
    body: "{{nodes.summarise.output}}"
```

---

### BrainVault + ModelRouter

BrainVault uses ModelRouter for both **embedding** (when notes are saved) and **generation** (during RAG queries). Configure BrainVault's backend with the ModelRouter base URL:

```env
# BrainVault backend .env
MODEL_ROUTER_URL=http://192.168.68.111:8082
```

A RAG query then flows as:

```
User question → BrainVault API
  → vector search (pgvector)
  → top-k note chunks retrieved
  → BrainVault assembles prompt
  → POST http://192.168.68.111:8082/v1/messages
  → ModelRouter → active provider
  → answer returned to user with citations
```

You can lower BrainVault's inference cost at any time by switching ModelRouter to Gemini Flash or Ollama without touching BrainVault's configuration.

---

### PeriodAI + ModelRouter

PeriodAI generates period-tracking reports using ModelRouter as its LLM backend. Set the base URL in PeriodAI's environment:

```env
# PeriodAI .env
LLM_BASE_URL=http://192.168.68.111:8082
LLM_MODEL=claude-3-5-sonnet-20241022
```

PeriodAI's report generation call (internal):

```python
import anthropic

client = anthropic.Anthropic(
    base_url=os.getenv("LLM_BASE_URL"),
    api_key="model-router",
)

report = client.messages.create(
    model=os.getenv("LLM_MODEL"),
    max_tokens=2048,
    system="You are a medical-grade menstrual health analyst.",
    messages=[{"role": "user", "content": prompt}],
)
```

---

### AgentVault — store and inject provider keys

[AgentVault](http://192.168.68.111) is the secrets manager for the NexusLayer stack. Store provider API keys there and inject them into ModelRouter at startup or on rotation.

**Store a secret in AgentVault:**

```bash
curl -X POST http://192.168.68.111/api/secrets \
  -H "Authorization: Bearer $AGENTVAULT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "ANTHROPIC_API_KEY",
    "value": "sk-ant-api03-...",
    "tags": ["llm", "production"]
  }'
```

**Fetch and inject into ModelRouter (Python):**

```python
import httpx

def rotate_provider_key():
    # Fetch from AgentVault
    secret = httpx.get(
        "http://192.168.68.111/api/secrets/ANTHROPIC_API_KEY",
        headers={"Authorization": f"Bearer {AGENTVAULT_TOKEN}"},
    ).json()

    # Push to ModelRouter
    httpx.put(
        "http://192.168.68.111:8082/api/config",
        json={"provider": "anthropic", "anthropicKey": secret["value"]},
    )
```

Run `rotate_provider_key()` from a cron job or a FlowMesh scheduled pipeline to automate key rotation without hardcoding secrets anywhere.

---

## Cost Optimization Guide

| Scenario                                  | Recommended Provider     | Reason                                              |
|-------------------------------------------|--------------------------|-----------------------------------------------------|
| Production user-facing responses          | `anthropic` (Haiku)      | Fast, cheap, high quality for most tasks            |
| Long document analysis (> 100 k tokens)   | `gemini` (1.5 Pro)       | 1 M token context window, competitive pricing       |
| Batch embedding / nightly jobs            | `gemini` (Flash) or `ollama` | Lowest cost per token at volume                 |
| Air-gapped / on-prem / no internet        | `ollama`                 | Fully local, zero egress cost                       |
| Complex reasoning, multi-step agents      | `anthropic` (Sonnet/Opus)| Best reasoning; worth the higher cost for accuracy  |
| Development and testing                   | `ollama`                 | Free, instant, no API key required                  |

**Switching for a nightly batch (bash script):**

```bash
#!/usr/bin/env bash
# Switch to cheap provider for batch, restore afterwards

ROUTER="http://192.168.68.111:8082"

echo "Switching to Gemini Flash for batch..."
curl -sX PUT "$ROUTER/api/config" \
  -H "Content-Type: application/json" \
  -d '{"provider":"gemini","geminiKey":"'"$GEMINI_KEY"'"}'

python run_nightly_batch.py

echo "Restoring Anthropic..."
curl -sX PUT "$ROUTER/api/config" \
  -H "Content-Type: application/json" \
  -d '{"provider":"anthropic","anthropicKey":"'"$ANTHROPIC_KEY"'"}'
```

---

## Failover Configuration

ModelRouter does not implement automatic failover out of the box, but you can build a lightweight wrapper that catches errors and promotes the next provider.

```python
import anthropic
import httpx
from typing import Any

ROUTER = "http://192.168.68.111:8082"
PROVIDER_CHAIN = ["anthropic", "gemini", "ollama"]

client = anthropic.Anthropic(base_url=ROUTER, api_key="model-router")


def _set_provider(provider: str) -> None:
    httpx.put(f"{ROUTER}/api/config", json={"provider": provider})


def resilient_create(**kwargs: Any) -> anthropic.types.Message:
    """Try each provider in order; move on after a transient error."""
    last_error: Exception | None = None
    for provider in PROVIDER_CHAIN:
        _set_provider(provider)
        try:
            return client.messages.create(**kwargs)
        except (anthropic.APIStatusError, anthropic.APIConnectionError) as exc:
            print(f"[ModelRouter] {provider} failed: {exc}. Trying next provider.")
            last_error = exc
    raise RuntimeError("All providers exhausted.") from last_error


# Usage
response = resilient_create(
    model="claude-3-5-sonnet-20241022",
    max_tokens=512,
    messages=[{"role": "user", "content": "Are you available?"}],
)
print(response.content[0].text)
```

> **Tip:** Use [WatchGrid](http://192.168.68.111) to monitor `/v1/messages` error rates per provider and alert when failover is triggered repeatedly — a signal to investigate a provider outage or exhausted quota.
