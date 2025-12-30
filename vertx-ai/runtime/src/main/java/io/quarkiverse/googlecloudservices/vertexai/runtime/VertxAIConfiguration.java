package io.quarkiverse.googlecloudservices.vertexai.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "quarkus.google.cloud.vertexai")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface VertxAIConfiguration {
    /**
     * Google Cloud region (single region mode).
     * Use this for single-region deployments.
     * For multi-region, use 'locations' instead.
     */
    Optional<String> location();

    /**
     * List of Google Cloud regions for multi-region support.
     * The first region in the list is considered the primary region.
     * Example: us-central1,us-east5,us-west1
     */
    Optional<List<String>> locations();

    /**
     * Vertex AI API endpoint.
     * If not specified, the default endpoint for each region will be used.
     */
    Optional<String> apiEndpoint();

    /**
     * Default model name to use for GenerativeModel creation.
     * Example: gemini-2.0-flash-001, gemini-1.5-pro
     */
    Optional<String> modelName();

    /**
     * Context caching configuration.
     */
    ContextCacheConfig contextCache();

    /**
     * Context caching configuration options.
     */
    interface ContextCacheConfig {
        /**
         * Enable context caching support.
         */
        @WithDefault("true")
        boolean enabled();

        /**
         * Default TTL (time-to-live) for cached content in minutes.
         */
        @WithDefault("60")
        int defaultTtlMinutes();
    }
}
