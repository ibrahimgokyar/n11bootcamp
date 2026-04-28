package com.n11bootcamp.mcp_service.mcp;

import com.n11bootcamp.mcp_service.service.RouteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ProductTools {

    private static final Logger log = LoggerFactory.getLogger(ProductTools.class);
    private final RouteService routeService;

    public ProductTools(RouteService routeService) {
        this.routeService = routeService;
    }

    @McpTool(
            name = "product.search",
            description = "Query products by natural language (e.g. 'pembe elbise'). Returns upstream JSON as-is."
    )
    public Object searchProducts(
            @McpToolParam(description = "User query text", required = true) String query,
            @McpToolParam(description = "Max items to return", required = false) Integer topK,
            String acceptLanguage
    ) {
        try {
            // ✅ IMMUTABLE FIX: HashMap kullan
            Map<String, Object> payload = new HashMap<>();
            payload.put("query", query);
            if (topK != null) payload.put("topK", topK);

            ResponseEntity<Object> resp = routeService.routeProductSearch(payload, acceptLanguage);
            return resp.getBody();

        } catch (Exception ex) {
            log.error("MCP tool 'product.search' failed: {}", ex.getMessage(), ex);
            return Map.of(
                    "error", "Bad gateway to ProductService",
                    "detail", ex.getMessage()
            );
        }
    }
}
