# ModelRouter — Architecture Document

## Overview

ModelRouter is a local proxy service that exposes a single **Anthropic Messages API-compatible** endpoint
(`POST /v1/messages`) and routes traffic to one of three configurable LLM backends:

| Provider        | Default Model              | Notes                      |
|-----------------|----------------------------|----------------------------|
| Anthropic Claude | claude-sonnet-4-20250514  | Direct pass-through        |
| Google Gemini   | gemini-2.0-flash           | Protocol translation       |
| Ollama (Local)  | User-configured            | Protocol translation       |

All products/clients in your ecosystem communicate **exclusively** with ModelRouter using the Anthropic
Messages API format. Switching between providers requires only a click in the Admin UI — zero client changes.

---

## System Diagram

```
                    ┌──────────────────────────────────────────┐
                    │              Your Products               │
                    │                                          │
                    │  Product A     Product B     Product C   │
                    │     │               │              │      │
                    └─────┼───────────────┼──────────────┼─────┘
                          │ POST /v1/messages (Anthropic format)
                          ▼
              ┌───────────────────────┐
              │     ModelRouter       │  :8080
              │                       │
              │  ┌─────────────────┐  │
              │  │ ProxyController │  │  /v1/messages
              │  └────────┬────────┘  │
              │           │           │
              │  ┌────────▼────────┐  │
              │  │  RouterService  │  │  reads activeProvider from DB
              │  └────────┬────────┘  │
              │           │           │
              │  ┌────────▼────────┐  │
              │  │ Provider Switch │  │
              │  └──┬──────┬──┬───┘  │
              │     │      │  │      │
              └─────┼──────┼──┼──────┘
                    │      │  │
          ┌─────────┘      │  └──────────┐
          ▼                ▼             ▼
  ┌───────────────┐ ┌───────────┐ ┌───────────────┐
  │   Anthropic   │ │  Gemini   │ │    Ollama      │
  │  Claude API   │ │   API     │ │  (localhost)   │
  └───────────────┘ └───────────┘ └───────────────┘

              ┌───────────────────────┐
              │   Admin REST API      │  /api/admin/*
              │   (consumed by UI)    │
              └───────────┬───────────┘
                          │
              ┌───────────▼───────────┐
              │    React UI           │  :5173 (dev)
              │    ModelRouter UI     │
              └───────────────────────┘
```

---

## Component Breakdown

### Backend (Java 21 + Spring Boot 3.2)

#### `ProxyController`
- `POST /v1/messages` — The single entry point for all client products.
- `GET  /v1/health`   — Healthcheck.
- Passes the raw Anthropic-format request to `RouterService`.

#### `AdminController`
- `GET  /api/admin/config`          — Returns current config (keys masked).
- `PUT  /api/admin/config`          — Partial update (only non-blank fields written).
- `POST /api/admin/provider/switch` — Atomically switches active provider.
- `GET  /api/admin/providers`       — Lists all available provider names.

#### `RouterService`
- Maintains a single `RouterConfig` row in H2 (id=1).
- Dispatches to the correct `LLMProvider` bean based on `activeProvider`.
- API key masking on reads.

#### Provider Adapters (`LLMProvider` interface)
| Class                | Translation                                  |
|----------------------|----------------------------------------------|
| `AnthropicProvider`  | Pass-through (reformats model field only)    |
| `GeminiProvider`     | Anthropic Messages → Gemini generateContent  |
| `OllamaProvider`     | Anthropic Messages → Ollama /api/chat        |

#### Persistence
- H2 file-based database at `./data/router-config.mv.db`
- Single `router_config` table, single row (id=1)
- Auto-created on first startup via `spring.jpa.hibernate.ddl-auto=update`

---

### Frontend (React 18 + Vite + Tailwind)

| Component           | Purpose                                          |
|---------------------|--------------------------------------------------|
| `Header`            | Shows router name + active provider badge        |
| `ProviderSelector`  | Card-based provider switcher with live indicator |
| `AnthropicConfig`   | API key input + save for Anthropic               |
| `GeminiConfig`      | API key + model selector for Gemini              |
| `OllamaConfig`      | Base URL + model picker for Ollama               |
| `TestConsole`       | In-browser chat to test the active provider      |
| `api.js`            | Axios service layer for all REST calls           |

---

## Data Flow — Request Lifecycle

```
1. Client sends:  POST http://router:8080/v1/messages
                  { "model": "...", "messages": [...], "max_tokens": 1024 }

2. ProxyController receives request, calls RouterService.route()

3. RouterService:
   a. Loads RouterConfig from H2 (gets activeProvider)
   b. Looks up matching LLMProvider bean
   c. Calls provider.chat(request, config)

4a. AnthropicProvider:
    - Replaces model with configured model
    - Calls api.anthropic.com/v1/messages with stored API key
    - Returns Anthropic-format response (no translation needed)

4b. GeminiProvider:
    - Maps messages[] → Gemini contents[]
    - Maps system prompt → prepended user turn
    - Calls generativelanguage.googleapis.com with stored API key
    - Maps Gemini candidates[] → Anthropic content[]

4c. OllamaProvider:
    - Maps messages[] → Ollama messages[]
    - Prepends system message if present
    - Calls {ollamaBaseUrl}/api/chat
    - Maps message.content → Anthropic content[]

5. ProxyController returns MessagesResponse to client
   (identical Anthropic format regardless of active provider)
```

---

## Security Considerations

- API keys are stored in the local H2 database file (`./data/`).
  **Do not expose the router on a public network** without adding authentication.
- Keys are masked in GET responses (first 4 + last 4 chars only).
- The router does NOT validate the `x-api-key` header from clients by default.
  Add a shared secret check in `ProxyController` if needed.
- For production use, consider encrypting the H2 database or using a secrets manager.

---

## Extension Points

| Feature              | Where to add                                      |
|----------------------|---------------------------------------------------|
| New LLM provider     | Implement `LLMProvider`, annotate `@Component`    |
| Request logging      | Add Spring AOP aspect on `RouterService.route()`  |
| Rate limiting        | Add `resilience4j` or Bucket4j in `ProxyController` |
| Authentication       | Add Spring Security + API key filter              |
| Streaming support    | Return `Flux<ServerSentEvent>` from `ProxyController` |
| Usage analytics      | Persist usage tokens to a `request_log` table     |
