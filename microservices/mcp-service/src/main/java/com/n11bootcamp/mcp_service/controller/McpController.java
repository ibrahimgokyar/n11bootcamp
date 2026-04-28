package com.n11bootcamp.mcp_service.controller;

import com.n11bootcamp.mcp_service.mcp.ProductTools;
import com.n11bootcamp.mcp_service.service.RouteService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/mcp")
@CrossOrigin(
        origins = {
                "http://localhost:3000",
                "http://94.73.134.50:3000",
                "http://94.73.134.50",
                "https://cd1a-94-73-134-50.ngrok-free.app"
        },
        allowedHeaders = "*",
        allowCredentials = "true",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS}
)
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

    private final RouteService routeService;
    private final ProductTools productTools;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService keepAliveScheduler = Executors.newSingleThreadScheduledExecutor();

    public McpController(RouteService routeService, ProductTools productTools) {
        this.routeService = routeService;
        this.productTools = productTools;

        keepAliveScheduler.scheduleAtFixedRate(() -> {
            for (SseEmitter em : emitters) {
                try {
                    em.send(SseEmitter.event().name("ping").data(Instant.now().toString()));
                } catch (Exception e) {
                    emitters.remove(em);
                    try { em.complete(); } catch (Exception ignore) {}
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        keepAliveScheduler.shutdownNow();
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "component", "mcp-server"));
    }

    // ✅ Passthrough: Accept-Language'ı upstream'e forward et
    // ✅ Redis cache header'larını da response'a forward et
    @PostMapping(
            path = "/api/product/chat-ai",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> routeProductSearch(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        try {
            String lang = normalizeLang(acceptLanguage);

            ResponseEntity<Object> resp = routeService.routeProductSearch(payload, lang);
            MediaType ct = resp.getHeaders().getContentType();
            if (ct == null) ct = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(ct);

            copyIfPresent(resp.getHeaders(), headers, "X-MCP-Redis-Cache");
            copyIfPresent(resp.getHeaders(), headers, HttpHeaders.CACHE_CONTROL);
            copyIfPresent(resp.getHeaders(), headers, HttpHeaders.ETAG);
            copyIfPresent(resp.getHeaders(), headers, HttpHeaders.LAST_MODIFIED);

            return new ResponseEntity<>(resp.getBody(), headers, resp.getStatusCode());

        } catch (Exception ex) {
            log.error("Routing failed", ex);
            return ResponseEntity.status(502).body(Map.of(
                    "error", "Bad gateway to product-service",
                    "detail", ex.getMessage()
            ));
        }
    }

    @GetMapping(path = {"", "/"}, produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE })
    public ResponseEntity<?> mcpRoot(@RequestHeader(value = "Accept", required = false) String accept) {
        final String acc = accept != null ? accept.toLowerCase() : "";
        if (acc.contains("text/event-stream")) {
            SseEmitter emitter = createSseEmitter();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .header(HttpHeaders.CONNECTION, "keep-alive")
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(emitter);
        }

        return ResponseEntity.ok(Map.of(
                "name", "opendart-mcp",
                "version", "1.0.0",
                "transport", "sse",
                "endpoints", Map.of(
                        "sse", "/mcp/sse",
                        "message", "/mcp/message"
                )
        ));
    }

    @PostMapping(path = {"", "/"}, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> mcpRootPost(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        log.debug("MCP ROOT POST payload={}", payload);
        return mcpMessage(payload, acceptLanguage);
    }

    @GetMapping(path = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> mcpSse() {
        SseEmitter emitter = createSseEmitter();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    // ✅ JSON-RPC Entry: Accept-Language al
    @PostMapping(path = "/message", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> mcpMessage(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage
    ) {
        final Object id = payload.get("id");
        final Object methodObj = payload.get("method");
        final String method = methodObj != null ? String.valueOf(methodObj) : null;

        String lang = normalizeLang(acceptLanguage);
        log.debug("MCP /message method={} lang={}", method, lang);

        try {
            if (id == null && method != null && method.startsWith("notifications/")) {
                return ResponseEntity.noContent().build();
            }

            if ("initialize".equals(method)) {
                Map<String, Object> result = Map.of(
                        "protocolVersion", "2025-03-26",
                        "capabilities", Map.of(
                                "tools", Map.of("listChanged", true)
                        ),
                        "serverInfo", Map.of("name", "opendart-mcp", "version", "1.0.0")
                );
                return okJsonRpc(id, result);
            }

            if ("tools/list".equals(method)) {
                Map<String, Object> result = Map.of(
                        "tools", new String[]{ "product.search" }
                );
                return okJsonRpc(id, result);
            }

            if ("tools/call".equals(method)) {
                Map<String, Object> params = getMap(payload.get("params"));
                String toolName = String.valueOf(params.get("name"));
                Map<String, Object> arguments = getMap(params.get("arguments"));

                // ✅ opsiyonel: payload.lang'e de yaz (bazı upstream'ler body'den okuyabilir)
                if (lang != null && !lang.isBlank() && !arguments.containsKey("lang")) {
                    arguments.put("lang", lang);
                }

                if ("product.search".equals(toolName)) {
                    String query = String.valueOf(arguments.getOrDefault("query", ""));
                    Integer topK = null;
                    Object topKObj = arguments.get("topK");
                    if (topKObj instanceof Number n) topK = n.intValue();

                    Object toolResult = productTools.searchProducts(query, topK, lang);
                    return okJsonRpc(id, toolResult);
                }

                return badRequestJsonRpc(id, -32601, "Unknown tool: " + toolName);
            }

            if (method != null && method.startsWith("notifications/")) {
                return ResponseEntity.noContent().build();
            }

            return badRequestJsonRpc(id, -32601, "Method not found");

        } catch (IllegalArgumentException iae) {
            return badRequestJsonRpc(id, -32602, iae.getMessage());
        } catch (Exception ex) {
            log.error("MCP /message failed", ex);
            return errorJsonRpc(id, -32603, "Internal error: " + ex.getMessage());
        }
    }

    // ---------- Helpers ----------
    private SseEmitter createSseEmitter() {
        final SseEmitter emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> { emitters.remove(emitter); emitter.complete(); });
        emitter.onError(ex -> { emitters.remove(emitter); emitter.complete(); });

        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }
        emitters.add(emitter);
        return emitter;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Object obj) {
        if (obj == null) return new java.util.HashMap<>();
        if (obj instanceof Map) return (Map<String, Object>) obj;
        throw new IllegalArgumentException("Expected JSON object, got " + obj.getClass().getSimpleName());
    }

    private ResponseEntity<Map<String, Object>> okJsonRpc(Object id, Object result) {
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .body(Map.of("jsonrpc", "2.0", "id", id, "result", result));
    }

    private ResponseEntity<Map<String, Object>> badRequestJsonRpc(Object id, int code, String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .body(Map.of("jsonrpc", "2.0", "id", id, "error", Map.of("code", code, "message", msg)));
    }

    private ResponseEntity<Map<String, Object>> errorJsonRpc(Object id, int code, String msg) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                .body(Map.of("jsonrpc", "2.0", "id", id, "error", Map.of("code", code, "message", msg)));
    }

    private String normalizeLang(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isBlank()) return null;
        s = s.split(",")[0].trim(); // "en-US,en;q=0.9" -> "en-US"
        if (s.length() >= 2) return s.substring(0, 2).toLowerCase(java.util.Locale.ROOT); // "en-US" -> "en"
        return s.toLowerCase(java.util.Locale.ROOT);
    }

    private static void copyIfPresent(HttpHeaders src, HttpHeaders dst, String key) {
        List<String> vals = src.get(key);
        if (vals != null && !vals.isEmpty()) {
            dst.put(key, vals);
        }
    }
}
