package io.quarkiverse.googlecloudservices.vertexai.deployment;

import io.quarkiverse.googlecloudservices.vertexai.runtime.CachedContentManager;
import io.quarkiverse.googlecloudservices.vertexai.runtime.VertexAIProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

/**
 * Build steps for the Vertex AI extension.
 *
 * This includes:
 * - Feature registration
 * - CDI bean registration
 * - Native image reflection configuration
 */
public class VertexAIBuildSteps {

    private static final String FEATURE = "google-cloud-vertex-ai";

    @BuildStep
    public FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        VertexAIProducer.class,
                        CachedContentManager.class)
                .setUnremovable()
                .build();
    }

    /**
     * Register Vertex AI SDK classes for reflection in native image.
     * This is critical for native compilation to work properly.
     */
    @BuildStep
    public void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // Core Vertex AI classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.VertexAI",
                "com.google.cloud.vertexai.VertexAI$Builder",
                "com.google.cloud.vertexai.generativeai.GenerativeModel",
                "com.google.cloud.vertexai.generativeai.GenerativeModel$Builder",
                "com.google.cloud.vertexai.generativeai.ChatSession",
                "com.google.cloud.vertexai.generativeai.ResponseHandler",
                "com.google.cloud.vertexai.generativeai.ResponseStream",
                "com.google.cloud.vertexai.generativeai.ContentMaker",
                "com.google.cloud.vertexai.generativeai.PartMaker").methods().fields().build());

        // API/Protobuf classes for content and configuration
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.Content",
                "com.google.cloud.vertexai.api.Content$Builder",
                "com.google.cloud.vertexai.api.Part",
                "com.google.cloud.vertexai.api.Part$Builder",
                "com.google.cloud.vertexai.api.Blob",
                "com.google.cloud.vertexai.api.Blob$Builder",
                "com.google.cloud.vertexai.api.FileData",
                "com.google.cloud.vertexai.api.FileData$Builder",
                "com.google.cloud.vertexai.api.FunctionCall",
                "com.google.cloud.vertexai.api.FunctionCall$Builder",
                "com.google.cloud.vertexai.api.FunctionResponse",
                "com.google.cloud.vertexai.api.FunctionResponse$Builder").methods().fields().build());

        // Generation configuration classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.GenerationConfig",
                "com.google.cloud.vertexai.api.GenerationConfig$Builder",
                "com.google.cloud.vertexai.api.SafetySetting",
                "com.google.cloud.vertexai.api.SafetySetting$Builder",
                "com.google.cloud.vertexai.api.HarmCategory",
                "com.google.cloud.vertexai.api.SafetySetting$HarmBlockThreshold",
                "com.google.cloud.vertexai.api.Tool",
                "com.google.cloud.vertexai.api.Tool$Builder",
                "com.google.cloud.vertexai.api.ToolConfig",
                "com.google.cloud.vertexai.api.ToolConfig$Builder").methods().fields().build());

        // Response classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.GenerateContentResponse",
                "com.google.cloud.vertexai.api.GenerateContentResponse$Builder",
                "com.google.cloud.vertexai.api.Candidate",
                "com.google.cloud.vertexai.api.Candidate$Builder",
                "com.google.cloud.vertexai.api.CitationMetadata",
                "com.google.cloud.vertexai.api.CitationMetadata$Builder",
                "com.google.cloud.vertexai.api.Citation",
                "com.google.cloud.vertexai.api.Citation$Builder",
                "com.google.cloud.vertexai.api.UsageMetadata",
                "com.google.cloud.vertexai.api.UsageMetadata$Builder",
                "com.google.cloud.vertexai.api.FinishReason").methods().fields().build());

        // Context caching classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.CachedContent",
                "com.google.cloud.vertexai.api.CachedContent$Builder",
                "com.google.cloud.vertexai.api.CreateCachedContentRequest",
                "com.google.cloud.vertexai.api.CreateCachedContentRequest$Builder",
                "com.google.cloud.vertexai.api.GetCachedContentRequest",
                "com.google.cloud.vertexai.api.GetCachedContentRequest$Builder",
                "com.google.cloud.vertexai.api.UpdateCachedContentRequest",
                "com.google.cloud.vertexai.api.UpdateCachedContentRequest$Builder",
                "com.google.cloud.vertexai.api.DeleteCachedContentRequest",
                "com.google.cloud.vertexai.api.DeleteCachedContentRequest$Builder",
                "com.google.cloud.vertexai.api.ListCachedContentsRequest",
                "com.google.cloud.vertexai.api.ListCachedContentsRequest$Builder",
                "com.google.cloud.vertexai.api.ListCachedContentsResponse",
                "com.google.cloud.vertexai.api.ListCachedContentsResponse$Builder").methods().fields().build());

        // Token counting classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.CountTokensRequest",
                "com.google.cloud.vertexai.api.CountTokensRequest$Builder",
                "com.google.cloud.vertexai.api.CountTokensResponse",
                "com.google.cloud.vertexai.api.CountTokensResponse$Builder").methods().fields().build());

        // Schema classes for structured output
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.Schema",
                "com.google.cloud.vertexai.api.Schema$Builder",
                "com.google.cloud.vertexai.api.Type").methods().fields().build());

        // Grounding and retrieval classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.GroundingMetadata",
                "com.google.cloud.vertexai.api.GroundingMetadata$Builder",
                "com.google.cloud.vertexai.api.SearchEntryPoint",
                "com.google.cloud.vertexai.api.SearchEntryPoint$Builder",
                "com.google.cloud.vertexai.api.GroundingChunk",
                "com.google.cloud.vertexai.api.GroundingChunk$Builder",
                "com.google.cloud.vertexai.api.GroundingSupport",
                "com.google.cloud.vertexai.api.GroundingSupport$Builder").methods().fields().build());
    }

    /**
     * Register classes that must be initialized at runtime.
     * This is important for classes that use randomness or have
     * static initializers that shouldn't run at build time.
     */
    @BuildStep
    public void runtimeInitialized(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit) {
        // Classes that need runtime initialization
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(
                "com.google.cloud.vertexai.generativeai.ResponseStream"));
    }
}
