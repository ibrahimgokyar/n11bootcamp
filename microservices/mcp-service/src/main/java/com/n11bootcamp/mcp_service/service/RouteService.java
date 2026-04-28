package com.n11bootcamp.mcp_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);
    private static final String PRODUCT_CHAT_AI_PATH = "/api/product/chat-ai";

    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final String productServiceBaseUrl;
    private final boolean redisCacheEnabled;
    private final long redisCacheTtlSeconds;

    public RouteService(RestTemplate restTemplate,
                        RedisTemplate<String, Object> redisTemplate,
                        @Value("${mcp.product-service.url}") String productServiceBaseUrl,
                        @Value("${mcp.redis.cache.enabled:true}") boolean redisCacheEnabled,
                        @Value("${mcp.redis.cache.ttl-seconds:300}") long redisCacheTtlSeconds) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
        this.productServiceBaseUrl = productServiceBaseUrl;
        this.redisCacheEnabled = redisCacheEnabled;
        this.redisCacheTtlSeconds = redisCacheTtlSeconds;
    }

    public ResponseEntity<Object> routeProductSearch(Map<String, Object> payload, String acceptLanguage) {
        final String targetUrl = productServiceBaseUrl + PRODUCT_CHAT_AI_PATH;
        Map<String, Object> safePayload = (payload == null) ? new HashMap<>() : new HashMap<>(payload);

        String normalizedLang = normalizeLang(acceptLanguage);
        String cacheKey = buildCacheKey(safePayload, normalizedLang);

        log.debug("Routing to {} with payload: {}", targetUrl, safePayload);

        if (redisCacheEnabled) {
            ResponseEntity<Object> cached = readFromCache(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));

        if (normalizedLang != null && !normalizedLang.isBlank()) {
            headers.set(HttpHeaders.ACCEPT_LANGUAGE, normalizedLang);
            safePayload.putIfAbsent("lang", normalizedLang);
        }

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(safePayload, headers);

        try {
            ResponseEntity<Object> upstream = restTemplate.exchange(
                    targetUrl, HttpMethod.POST, req, Object.class
            );

            if (upstream.getStatusCode().value() == 204 || upstream.getBody() == null) {
                return ResponseEntity.noContent().build();
            }

            MediaType ct = upstream.getHeaders().getContentType();
            if (ct == null) ct = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);

            HttpHeaders out = new HttpHeaders();
            out.setContentType(ct);
            copyIfPresent(upstream.getHeaders(), out, HttpHeaders.CACHE_CONTROL);
            copyIfPresent(upstream.getHeaders(), out, HttpHeaders.ETAG);
            copyIfPresent(upstream.getHeaders(), out, HttpHeaders.LAST_MODIFIED);
            out.set("X-MCP-Redis-Cache", "MISS");

            if (redisCacheEnabled) {
                writeToCache(cacheKey, upstream.getBody());
            }

            log.debug("Response from ProductService [{}]", upstream.getStatusCode());
            return new ResponseEntity<>(upstream.getBody(), out, upstream.getStatusCode());

        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            log.error("Error from ProductService: {}", ex.getMessage());
            MediaType ct = new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8);
            String body = ex.getResponseBodyAsString();
            return ResponseEntity.status(ex.getStatusCode())
                    .contentType(ct)
                    .body(Map.of(
                            "error", "ProductService error",
                            "status", ex.getStatusCode().value(),
                            "detail", body
                    ));
        } catch (Exception ex) {
            log.error("Routing failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(502).body(Map.of(
                    "error", "Bad gateway to ProductService",
                    "detail", ex.getMessage()
            ));
        }
    }

    private ResponseEntity<Object> readFromCache(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached == null) {
                log.debug("Redis cache MISS key={}", cacheKey);
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
            headers.set("X-MCP-Redis-Cache", "HIT");
            log.debug("Redis cache HIT key={}", cacheKey);
            return new ResponseEntity<>(cached, headers, HttpStatus.OK);
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            log.warn("Redis unavailable during read, continuing without cache: {}", ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("Redis read failed, continuing without cache: {}", ex.getMessage());
            return null;
        }
    }

    private void writeToCache(String cacheKey, Object body) {
        try {
            redisTemplate.opsForValue().set(cacheKey, body, Duration.ofSeconds(redisCacheTtlSeconds));
            log.debug("Redis cache WRITE key={} ttl={}s", cacheKey, redisCacheTtlSeconds);
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            log.warn("Redis unavailable during write, continuing without cache: {}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("Redis write failed, continuing without cache: {}", ex.getMessage());
        }
    }

    private String buildCacheKey(Map<String, Object> payload, String acceptLanguage) {
        String query = String.valueOf(payload.getOrDefault("query", "")).trim().toLowerCase(Locale.ROOT);
        String topK = String.valueOf(payload.getOrDefault("topK", "20")).trim();
        String lang = acceptLanguage == null || acceptLanguage.isBlank() ? "default" : acceptLanguage.toLowerCase(Locale.ROOT);
        return "mcp:product.search:" + lang + ":" + topK + ":" + query;
    }

    private String normalizeLang(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isBlank()) return null;
        s = s.split(",")[0].trim();
        if (s.length() >= 2) return s.substring(0, 2).toLowerCase(Locale.ROOT);
        return s.toLowerCase(Locale.ROOT);
    }

    private static void copyIfPresent(HttpHeaders src, HttpHeaders dst, String key) {
        List<String> vals = src.get(key);
        if (vals != null && !vals.isEmpty()) dst.put(key, vals);
    }
}
