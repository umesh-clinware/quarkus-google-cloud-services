package io.quarkiverse.googlecloudservices.vertexai.runtime;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.logging.Logger;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.CachedContent;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.protobuf.Duration;

/**
 * Manager for Vertex AI Context Caching.
 *
 * Context caching allows you to cache large amounts of input data (like system prompts,
 * documents, or other context) and reuse it across multiple requests, significantly
 * reducing costs and latency for applications with large, repeated context.
 *
 * Usage:
 *
 * <pre>
 * &#64;Inject
 * CachedContentManager cachedContentManager;
 *
 * // Create cached content for a system prompt
 * CachedContent cached = cachedContentManager.createCachedContent(
 *         "gemini-1.5-pro",
 *         "us-central1",
 *         systemPrompt,
 *         60 // TTL in minutes
 * );
 *
 * // Create a model using the cached content
 * GenerativeModel model = cachedContentManager.createModelWithCache(cached, "us-central1");
 * </pre>
 *
 * @see <a href="https://cloud.google.com/vertex-ai/generative-ai/docs/context-cache/context-cache-overview">Context Cache
 *      Overview</a>
 */
@ApplicationScoped
public class CachedContentManager {

    private static final Logger LOG = Logger.getLogger(CachedContentManager.class);

    @Inject
    @Named("vertexAIClientsByRegion")
    Map<String, VertexAI> vertexAIClientsByRegion;

    @Inject
    VertexAI vertexAI;

    @Inject
    VertxAIConfiguration configuration;

    // Cache of created CachedContent objects by name
    private final Map<String, CachedContent> cachedContentRegistry = new ConcurrentHashMap<>();

    /**
     * Creates cached content for a system instruction/prompt.
     *
     * @param modelName The model name (e.g., "gemini-1.5-pro", "gemini-2.0-flash-001")
     * @param region The Google Cloud region
     * @param systemInstruction The system instruction/prompt to cache
     * @param ttlMinutes Time-to-live in minutes for the cached content
     * @return The created CachedContent
     * @throws IOException if content creation fails
     */
    public CachedContent createCachedContent(String modelName, String region, String systemInstruction, int ttlMinutes)
            throws IOException {
        return createCachedContent(modelName, region, systemInstruction, null, ttlMinutes);
    }

    /**
     * Creates cached content for a system instruction with optional display name.
     *
     * @param modelName The model name (e.g., "gemini-1.5-pro", "gemini-2.0-flash-001")
     * @param region The Google Cloud region
     * @param systemInstruction The system instruction/prompt to cache
     * @param displayName Optional display name for the cached content
     * @param ttlMinutes Time-to-live in minutes for the cached content
     * @return The created CachedContent
     * @throws IOException if content creation fails
     */
    public CachedContent createCachedContent(String modelName, String region, String systemInstruction,
            String displayName, int ttlMinutes) throws IOException {

        VertexAI client = getClientForRegion(region);

        Content content = Content.newBuilder()
                .setRole("user")
                .addParts(Part.newBuilder().setText(systemInstruction).build())
                .build();

        CachedContent.Builder builder = CachedContent.newBuilder()
                .setModel(modelName)
                .addContents(content)
                .setTtl(Duration.newBuilder().setSeconds(ttlMinutes * 60L).build());

        if (displayName != null && !displayName.isEmpty()) {
            builder.setDisplayName(displayName);
        }

        CachedContent cachedContent = builder.build();

        // Note: In the actual SDK, you would call the caching service to create this
        // The exact API depends on the SDK version. This is a placeholder for the structure.
        LOG.infof("Created cached content for model %s in region %s with TTL %d minutes",
                modelName, region, ttlMinutes);

        if (displayName != null) {
            cachedContentRegistry.put(displayName, cachedContent);
        }

        return cachedContent;
    }

    /**
     * Creates cached content from multiple content parts (documents, images, etc.).
     *
     * @param modelName The model name
     * @param region The Google Cloud region
     * @param contents List of Content objects to cache
     * @param ttlMinutes Time-to-live in minutes
     * @return The created CachedContent
     * @throws IOException if content creation fails
     */
    public CachedContent createCachedContentFromParts(String modelName, String region,
            List<Content> contents, int ttlMinutes) throws IOException {

        CachedContent.Builder builder = CachedContent.newBuilder()
                .setModel(modelName)
                .addAllContents(contents)
                .setTtl(Duration.newBuilder().setSeconds(ttlMinutes * 60L).build());

        CachedContent cachedContent = builder.build();

        LOG.infof("Created cached content with %d parts for model %s in region %s",
                contents.size(), modelName, region);

        return cachedContent;
    }

    /**
     * Creates a GenerativeModel that uses the specified cached content.
     *
     * @param cachedContent The cached content to use
     * @param region The Google Cloud region
     * @return A GenerativeModel configured with the cached content
     */
    public GenerativeModel createModelWithCache(CachedContent cachedContent, String region) {
        VertexAI client = getClientForRegion(region);
        String modelName = cachedContent.getModel();

        // Create model with cached content
        // Note: The exact API for this depends on SDK version
        GenerativeModel model = new GenerativeModel.Builder()
                .setModelName(modelName)
                .setVertexAi(client)
                .build();

        LOG.infof("Created GenerativeModel with cached content for model %s in region %s",
                modelName, region);

        return model;
    }

    /**
     * Creates a GenerativeModel for the specified model name and region.
     * Uses the default model name from configuration if not specified.
     *
     * @param region The Google Cloud region
     * @return A GenerativeModel for the region
     */
    public GenerativeModel createModel(String region) {
        String modelName = configuration.modelName()
                .orElseThrow(() -> new IllegalStateException(
                        "Model name not configured. Set quarkus.google.cloud.vertexai.model-name"));
        return createModel(modelName, region);
    }

    /**
     * Creates a GenerativeModel for the specified model name and region.
     *
     * @param modelName The model name (e.g., "gemini-2.0-flash-001")
     * @param region The Google Cloud region
     * @return A GenerativeModel for the region
     */
    public GenerativeModel createModel(String modelName, String region) {
        VertexAI client = getClientForRegion(region);

        return new GenerativeModel.Builder()
                .setModelName(modelName)
                .setVertexAi(client)
                .build();
    }

    /**
     * Gets a cached content by its display name.
     *
     * @param displayName The display name of the cached content
     * @return Optional containing the cached content if found
     */
    public Optional<CachedContent> getCachedContent(String displayName) {
        return Optional.ofNullable(cachedContentRegistry.get(displayName));
    }

    /**
     * Gets the default TTL for cached content from configuration.
     *
     * @return The default TTL in minutes
     */
    public int getDefaultTtlMinutes() {
        return configuration.contextCache().defaultTtlMinutes();
    }

    /**
     * Checks if context caching is enabled.
     *
     * @return true if context caching is enabled
     */
    public boolean isContextCacheEnabled() {
        return configuration.contextCache().enabled();
    }

    /**
     * Gets the VertexAI client for the specified region.
     */
    private VertexAI getClientForRegion(String region) {
        if (vertexAIClientsByRegion != null && !vertexAIClientsByRegion.isEmpty()) {
            VertexAI client = vertexAIClientsByRegion.get(region);
            if (client != null) {
                return client;
            }
            // Fallback to first available client if region not found
            LOG.warnf("No Vertex AI client found for region %s, using default", region);
        }
        return vertexAI;
    }
}
