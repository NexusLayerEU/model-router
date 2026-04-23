# ModelRouter — Setup Guide

## Prerequisites

| Tool        | Version    | Install                                          |
|-------------|------------|--------------------------------------------------|
| Java        | 21+        | `brew install openjdk@21` or [adoptium.net](https://adoptium.net) |
| Maven       | 3.9+       | `brew install maven`                             |
| Node.js     | 18+        | `brew install node`                              |
| npm         | 9+         | Included with Node.js                            |

Optional:
- [Ollama](https://ollama.com) — only needed if using the Ollama provider

---

## 1. Clone / Open Project

```bash
cd /path/to/ModelRouter
```

Directory structure:
```
ModelRouter/
├── backend/          # Java Spring Boot
├── frontend/         # React + Vite
└── docs/             # Documentation
```

---

## 2. Start the Backend

```bash
# Use the helper script (auto-selects Java 21 on macOS if needed):
./start-backend.sh

# Or manually, ensuring JAVA_HOME points to Java 21:
cd backend
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home mvn spring-boot:run
```

On first run, Maven downloads dependencies (~2 min). Subsequent runs are fast.

The server starts on **http://localhost:8080**.

Verify:
```bash
curl http://localhost:8080/v1/health
# → {"status":"ok","service":"ModelRouter"}
```

The H2 database file is created at `backend/data/router-config.mv.db`.
You can access the H2 console at http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:file:./data/router-config`).

---

## 3. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173** in your browser.

---

## 4. Configure Your First Provider

### Anthropic Claude (default)

1. Get an API key from https://console.anthropic.com
2. In the UI, enter the key in the **Anthropic** panel and click **Save Key**
3. Anthropic is already the default active provider

### Google Gemini

1. Get an API key from https://aistudio.google.com/app/apikey
2. In the UI, enter the key in the **Gemini** panel
3. Optionally select a model (default: `gemini-2.0-flash`)
4. Click **Save Config**, then click the **Gemini card** to switch

### Ollama (Local)

1. Install Ollama: https://ollama.com/download
2. Pull a model: `ollama pull llama3`
3. In the UI, set the base URL (default: `http://localhost:11434`) and model name
4. Click **Save Config**, then click the **Ollama card** to switch

---

## 5. Point Your Products at the Router

Replace the Anthropic base URL in all your products with the router URL:

```
http://localhost:8080
```

Use **any value** for the `x-api-key` header — the router uses its own stored keys.

### Python (Anthropic SDK)
```python
client = anthropic.Anthropic(
    api_key="router",
    base_url="http://localhost:8080",
)
```

### Node.js (Anthropic SDK)
```js
const client = new Anthropic({
  apiKey: 'router',
  baseURL: 'http://localhost:8080',
})
```

### Direct HTTP
```
POST http://localhost:8080/v1/messages
x-api-key: router
Content-Type: application/json
```

---

## 6. Production / Network Deployment

If you want to run the router on a server accessible to multiple machines:

1. **Build the backend JAR:**
   ```bash
   cd backend
   mvn clean package -DskipTests
   java -jar target/model-router-1.0.0.jar
   ```

2. **Build the frontend:**
   ```bash
   cd frontend
   npm run build
   # Serve dist/ with nginx, caddy, or any static server
   ```

3. **Update CORS** in `WebConfig.java` to allow your production domains.

4. **Security**: Add a shared API key check in `ProxyController` before deploying publicly.

---

## 7. Troubleshooting

| Symptom                            | Fix                                                           |
|------------------------------------|---------------------------------------------------------------|
| UI shows "Cannot reach backend"    | Ensure backend is running on port 8080                        |
| "Anthropic API key not configured" | Enter key in UI → Anthropic panel → Save                      |
| Gemini 400 errors                  | Check the API key and that billing is enabled in Google Cloud |
| Ollama connection refused          | Ensure Ollama is running: `ollama serve`                      |
| Port 8080 in use                   | Change `server.port` in `backend/src/main/resources/application.yml` |
| Port 5173 in use                   | Run `npm run dev -- --port 3000`                              |
