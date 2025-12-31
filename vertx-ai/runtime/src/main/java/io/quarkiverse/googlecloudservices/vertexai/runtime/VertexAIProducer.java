package io.quarkiverse.googlecloudservices.vertexai.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import com.google.auth.Credentials;
import com.google.cloud.vertexai.VertexAI;

import io.quarkiverse.googlecloudservices.common.GcpBootstrapConfiguration;
import io.quarkiverse.googlecloudservices.common.GcpConfigHolder;

/**
 * CDI Producer for Vertex AI clients.
 *
 * Supports both single-region and multi-region configurations:
 * - Single region: Inject {@code VertexAI} directly
 * - Multi-region: Inject {@code Map<String, VertexAI>} where keys are region names
 *
 * The first region in the list (or the single configured region) is used as the default.
 */
@ApplicationScoped
public class VertexAIProducer {

    private static final Logger LOG = Logger.getLogger(VertexAIProducer.class);

    @Inject
    Credentials googleCredentials;

    @Inject
    GcpConfigHolder gcpConfigHolder;

    @Inject
    VertxAIConfiguration vertxAIConfiguration;

    /**
     * Produces a map of VertexAI clients keyed by region.
     * This enables multi-region support for load balancing and failover.
     *
     * Usage:
     *
     * <pre>
     * &#64;Inject
     * Map&lt;String, VertexAI&gt; vertexAiClientsByRegion;
     * </pre>
     */
    @Produces
    @Singleton
    @Named("vertexAIClientsByRegion")
    public Map<String, VertexAI> vertexAIClientsByRegion() {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        String projectId = gcpConfiguration.projectId().orElse(null);

        List<String> regions = resolveRegions();
        if (regions.isEmpty()) {
            LOG.warn(
                    "No Vertex AI regions configured. Set quarkus.google.cloud.vertexai.location or quarkus.google.cloud.vertexai.locations");
            return Collections.emptyMap();
        }

        Map<String, VertexAI> clients = new LinkedHashMap<>();
        for (String region : regions) {
            VertexAI client = createVertexAI(projectId, region);
            clients.put(region, client);
            LOG.infof("Created Vertex AI client for region: %s", region);
        }

        return Collections.unmodifiableMap(clients);
    }

    /**
     * Produces a single VertexAI client for the primary (first) region.
     * This maintains backward compatibility with single-region usage.
     *
     * Usage:
     *
     * <pre>
     * &#64;Inject
     * VertexAI vertexAI;
     * </pre>
     */
    @Produces
    @Singleton
    @Default
    public VertexAI vertexAI() {
        GcpBootstrapConfiguration gcpConfiguration = gcpConfigHolder.getBootstrapConfig();
        String projectId = gcpConfiguration.projectId().orElse(null);

        List<String> regions = resolveRegions();
        String primaryRegion = regions.isEmpty() ? null : regions.get(0);

        return createVertexAI(projectId, primaryRegion);
    }

    /**
     * Resolves the list of regions from configuration.
     * Prefers 'locations' (multi-region) over 'location' (single region).
     */
    private List<String> resolveRegions() {
        // Prefer multi-region configuration
        if (vertxAIConfiguration.locations().isPresent() && !vertxAIConfiguration.locations().get().isEmpty()) {
            return vertxAIConfiguration.locations().get();
        }
        // Fall back to single region
        if (vertxAIConfiguration.location().isPresent()) {
            return List.of(vertxAIConfiguration.location().get());
        }
        return Collections.emptyList();
    }

    /**
     * Creates a VertexAI client for the specified project and region.
     */
    private VertexAI createVertexAI(String projectId, String region) {
        var builder = new VertexAI.Builder()
                .setCredentials(googleCredentials);

        if (projectId != null) {
            builder.setProjectId(projectId);
        }
        if (region != null) {
            builder.setLocation(region);
        }
        vertxAIConfiguration.apiEndpoint().ifPresent(builder::setApiEndpoint);

        return builder.build();
    }

    /**
     * Disposes the single VertexAI client.
     */
    public void closeVertexAI(@Disposes VertexAI vertexAI) {
        if (vertexAI != null) {
            vertexAI.close();
            LOG.debug("Closed Vertex AI client");
        }
    }

    /**
     * Disposes all VertexAI clients in the multi-region map.
     */
    public void closeVertexAIClients(@Disposes @Named("vertexAIClientsByRegion") Map<String, VertexAI> clients) {
        if (clients != null) {
            clients.forEach((region, client) -> {
                client.close();
                LOG.debugf("Closed Vertex AI client for region: %s", region);
            });
        }
    }
}
