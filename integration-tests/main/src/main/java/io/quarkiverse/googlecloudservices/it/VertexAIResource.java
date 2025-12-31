package io.quarkiverse.googlecloudservices.it;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

import io.quarkiverse.googlecloudservices.vertexai.runtime.CachedContentManager;

/**
 * REST resource for testing Vertex AI integration.
 * 
 * Demonstrates:
 * - Single VertexAI client injection
 * - Multi-region client map injection
 * - CachedContentManager usage
 */
@Path("/vertexai")
public class VertexAIResource {
    
    @Inject
    VertexAI vertexAI;

    @Inject
    @Named("vertexAIClientsByRegion")
    Map<String, VertexAI> vertexAIClientsByRegion;

    @Inject
    CachedContentManager cachedContentManager;

    /**
     * Generate content using the default (single) VertexAI client.
     */
    @GET
    public String predict(@QueryParam("prompt") String prompt) throws IOException {
        var model = new GenerativeModel("gemini-2.0-flash-001", vertexAI);
        var response = model.generateContent(prompt);
        return response.toString();
    }

    /**
     * Generate content using a specific region.
     */
    @GET
    @Path("/region/{region}")
    public String predictInRegion(@PathParam("region") String region, 
                                   @QueryParam("prompt") String prompt) throws IOException {
        VertexAI client = vertexAIClientsByRegion.get(region);
        if (client == null) {
            return "Region not found: " + region + ". Available regions: " + 
                    vertexAIClientsByRegion.keySet();
        }
        
        var model = new GenerativeModel("gemini-2.0-flash-001", client);
        var response = model.generateContent(prompt);
        return response.toString();
    }

    /**
     * List available regions.
     */
    @GET
    @Path("/regions")
    public String listRegions() {
        if (vertexAIClientsByRegion == null || vertexAIClientsByRegion.isEmpty()) {
            return "No multi-region configuration. Set quarkus.google.cloud.vertexai.locations";
        }
        return vertexAIClientsByRegion.keySet().stream()
                .collect(Collectors.joining(", "));
    }

    /**
     * Check if context caching is enabled.
     */
    @GET
    @Path("/cache/status")
    public String cacheStatus() {
        return "Context caching enabled: " + cachedContentManager.isContextCacheEnabled() + 
               ", Default TTL: " + cachedContentManager.getDefaultTtlMinutes() + " minutes";
    }

    /**
     * Health check endpoint.
     */
    @GET
    @Path("/health")
    public String health() {
        StringBuilder sb = new StringBuilder();
        sb.append("Vertex AI Extension Health:\n");
        sb.append("- Default client: ").append(vertexAI != null ? "OK" : "NOT CONFIGURED").append("\n");
        sb.append("- Multi-region clients: ").append(
                vertexAIClientsByRegion != null ? vertexAIClientsByRegion.size() + " regions" : "NOT CONFIGURED"
        ).append("\n");
        sb.append("- Context caching: ").append(
                cachedContentManager.isContextCacheEnabled() ? "ENABLED" : "DISABLED"
        ).append("\n");
        return sb.toString();
    }
}
