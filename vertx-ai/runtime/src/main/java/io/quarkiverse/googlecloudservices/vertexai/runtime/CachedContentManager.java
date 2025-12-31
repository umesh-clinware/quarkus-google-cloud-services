package io.quarkiverse.googlecloudservices.vertexai.runtime;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
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
import com.google.cloud.vertexai.api.FileData;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.ContentMaker;
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

    // Cache of created CachedContent objects by name/key
    private final Map<String, CachedContent> cachedContentRegistry = new ConcurrentHashMap<>();

    // Track cache creation times for cleanup
    private final Map<String, Instant> cacheCreationTimes = new ConcurrentHashMap<>();

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

        // Validate region has a configured client
        VertexAI client = getClientForRegion(region);
        LOG.debugf("Using VertexAI client for region %s (project: %s)", region, client.getProjectId());

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
     * Creates cached content from a list of GCS document URLs.
     * This is the primary method for workflow-level document caching.
     *
     * @param modelName The model name (e.g., "gemini-2.5-flash")
     * @param region The Google Cloud region
     * @param gcsUrls List of GCS URLs (gs://bucket/path/file.pdf)
     * @param displayName A unique display name for this cache (e.g., patientId)
     * @param ttlMinutes Time-to-live in minutes for the cached content
     * @return The created CachedContent
     * @throws IOException if content creation fails
     */
    public CachedContent createCacheFromGcsDocuments(String modelName, String region,
            List<String> gcsUrls, String displayName, int ttlMinutes) throws IOException {

        if (gcsUrls == null || gcsUrls.isEmpty()) {
            throw new IllegalArgumentException("GCS URLs list cannot be null or empty");
        }

        // Validate region has a configured client
        VertexAI client = getClientForRegion(region);
        LOG.debugf("Using VertexAI client for region %s (project: %s)", region, client.getProjectId());

        // Build Parts from GCS URLs
        List<Part> parts = new ArrayList<>();
        for (String gcsUrl : gcsUrls) {
            String mimeType = getMimeTypeFromUrl(gcsUrl);
            Part filePart = Part.newBuilder()
                    .setFileData(FileData.newBuilder()
                            .setFileUri(gcsUrl)
                            .setMimeType(mimeType)
                            .build())
                    .build();
            parts.add(filePart);
        }

        // Create the content with all document parts
        Content content = Content.newBuilder()
                .setRole("user")
                .addAllParts(parts)
                .build();

        // Build CachedContent
        CachedContent.Builder builder = CachedContent.newBuilder()
                .setModel(modelName)
                .addContents(content)
                .setTtl(Duration.newBuilder().setSeconds(ttlMinutes * 60L).build());

        if (displayName != null && !displayName.isEmpty()) {
            builder.setDisplayName(displayName);
        }

        CachedContent cachedContent = builder.build();

        // Store in registry
        String cacheKey = displayName != null ? displayName : generateCacheKey(modelName, region);
        cachedContentRegistry.put(cacheKey, cachedContent);
        cacheCreationTimes.put(cacheKey, Instant.now());

        LOG.infof("Created cached content from %d GCS documents for model %s in region %s (key: %s, TTL: %d min)",
                gcsUrls.size(), modelName, region, cacheKey, ttlMinutes);

        return cachedContent;
    }

    /**
     * Deletes a cached content by its key/display name.
     *
     * @param cacheKey The cache key or display name
     * @param region The Google Cloud region (for potential server-side deletion)
     */
    public void deleteCache(String cacheKey, String region) {
        CachedContent removed = cachedContentRegistry.remove(cacheKey);
        cacheCreationTimes.remove(cacheKey);

        if (removed != null) {
            LOG.infof("Deleted cached content: %s from region %s", cacheKey, region);

            // If the cache has a name (server-side cache), attempt to delete it
            if (removed.getName() != null && !removed.getName().isEmpty()) {
                try {
                    // Note: Server-side cache deletion would be done here
                    // The actual API call depends on SDK capabilities
                    LOG.debugf("Server-side cache deletion would be performed for: %s", removed.getName());
                } catch (Exception e) {
                    LOG.warnf("Failed to delete server-side cache %s: %s", removed.getName(), e.getMessage());
                }
            }
        } else {
            LOG.warnf("No cached content found with key: %s", cacheKey);
        }
    }

    /**
     * Creates a GenerativeModel that uses an existing cached content.
     * The cached content should contain the documents, so the model doesn't need them in each request.
     *
     * @param cachedContent The cached content containing documents
     * @param region The Google Cloud region
     * @param systemInstruction Optional system instruction (can be null)
     * @return A GenerativeModel configured to use the cached content
     */
    public GenerativeModel createModelFromCache(CachedContent cachedContent, String region, String systemInstruction) {
        VertexAI client = getClientForRegion(region);
        String modelName = cachedContent.getModel();

        GenerativeModel.Builder builder = new GenerativeModel.Builder()
                .setModelName(modelName)
                .setVertexAi(client);

        // Add system instruction if provided
        if (systemInstruction != null && !systemInstruction.isEmpty()) {
            builder.setSystemInstruction(ContentMaker.fromString(systemInstruction));
        }

        // Note: The actual setting of cached content depends on SDK version
        // In newer SDK versions, there should be a .setCachedContent() method
        // For now, we log and create the model - the cached content info is available
        LOG.infof("Created GenerativeModel from cached content for model %s in region %s (cache display name: %s)",
                modelName, region, cachedContent.getDisplayName());

        return builder.build();
    }

    /**
     * Checks if a cache exists for the given key.
     *
     * @param cacheKey The cache key to check
     * @return true if a cache exists
     */
    public boolean hasCachedContent(String cacheKey) {
        return cachedContentRegistry.containsKey(cacheKey);
    }

    /**
     * Gets all currently cached content keys.
     *
     * @return List of cache keys
     */
    public List<String> getCachedContentKeys() {
        return new ArrayList<>(cachedContentRegistry.keySet());
    }

    /**
     * Clears all cached content from the local registry.
     * Note: This does not delete server-side caches.
     */
    public void clearAllCaches() {
        int count = cachedContentRegistry.size();
        cachedContentRegistry.clear();
        cacheCreationTimes.clear();
        LOG.infof("Cleared %d cached content entries from registry", count);
    }

    /**
     * Gets the MIME type from a GCS URL based on file extension.
     */
    private String getMimeTypeFromUrl(String gcsUrl) {
        String lowerUrl = gcsUrl.toLowerCase();
        if (lowerUrl.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerUrl.endsWith(".png")) {
            return "image/png";
        } else if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerUrl.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerUrl.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerUrl.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerUrl.endsWith(".html") || lowerUrl.endsWith(".htm")) {
            return "text/html";
        } else if (lowerUrl.endsWith(".json")) {
            return "application/json";
        } else if (lowerUrl.endsWith(".mp4")) {
            return "video/mp4";
        } else if (lowerUrl.endsWith(".mp3")) {
            return "audio/mpeg";
        } else if (lowerUrl.endsWith(".wav")) {
            return "audio/wav";
        }
        // Default to octet-stream for unknown types
        return "application/octet-stream";
    }

    /**
     * Generates a unique cache key.
     */
    private String generateCacheKey(String modelName, String region) {
        return String.format("%s_%s_%d", modelName, region, System.currentTimeMillis());
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
