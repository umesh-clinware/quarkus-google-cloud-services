package io.quarkiverse.googlecloudservices.vertexai.deployment;

import io.quarkiverse.googlecloudservices.vertexai.runtime.CachedContentManager;
import io.quarkiverse.googlecloudservices.vertexai.runtime.GeminiModelBuilder;
import io.quarkiverse.googlecloudservices.vertexai.runtime.VertexAIProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

/**
 * Build steps for the Vertex AI extension with full Gemini 3 support.
 *
 * <p>
 * This includes:
 * <ul>
 * <li>Feature registration</li>
 * <li>CDI bean registration</li>
 * <li>Native image reflection configuration for all Gemini features</li>
 * </ul>
 *
 * <p>
 * Supported features:
 * <ul>
 * <li>Context caching</li>
 * <li>Multi-region support</li>
 * <li>Function calling / tool use</li>
 * <li>Grounding with Google Search</li>
 * <li>Structured output (JSON schema)</li>
 * <li>Code execution</li>
 * <li>Safety settings</li>
 * </ul>
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
                        CachedContentManager.class,
                        GeminiModelBuilder.class)
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
                "com.google.cloud.vertexai.generativeai.PartMaker",
                "com.google.cloud.vertexai.generativeai.FunctionDeclarationMaker",
                "com.google.cloud.vertexai.generativeai.SchemaMaker",
                "com.google.cloud.vertexai.generativeai.AutomaticFunctionCallingResponder").methods().fields().build());

        // API/Protobuf classes for content and configuration
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.Content",
                "com.google.cloud.vertexai.api.Content$Builder",
                "com.google.cloud.vertexai.api.Part",
                "com.google.cloud.vertexai.api.Part$Builder",
                "com.google.cloud.vertexai.api.Blob",
                "com.google.cloud.vertexai.api.Blob$Builder",
                "com.google.cloud.vertexai.api.FileData",
                "com.google.cloud.vertexai.api.FileData$Builder").methods().fields().build());

        // Function calling classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.FunctionCall",
                "com.google.cloud.vertexai.api.FunctionCall$Builder",
                "com.google.cloud.vertexai.api.FunctionResponse",
                "com.google.cloud.vertexai.api.FunctionResponse$Builder",
                "com.google.cloud.vertexai.api.FunctionDeclaration",
                "com.google.cloud.vertexai.api.FunctionDeclaration$Builder",
                "com.google.cloud.vertexai.api.FunctionCallingConfig",
                "com.google.cloud.vertexai.api.FunctionCallingConfig$Builder",
                "com.google.cloud.vertexai.api.FunctionCallingConfig$Mode").methods().fields().build());

        // Tool classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.Tool",
                "com.google.cloud.vertexai.api.Tool$Builder",
                "com.google.cloud.vertexai.api.ToolConfig",
                "com.google.cloud.vertexai.api.ToolConfig$Builder",
                "com.google.cloud.vertexai.api.Tool$CodeExecution",
                "com.google.cloud.vertexai.api.Tool$CodeExecution$Builder").methods().fields().build());

        // Generation configuration classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.GenerationConfig",
                "com.google.cloud.vertexai.api.GenerationConfig$Builder",
                "com.google.cloud.vertexai.api.SafetySetting",
                "com.google.cloud.vertexai.api.SafetySetting$Builder",
                "com.google.cloud.vertexai.api.HarmCategory",
                "com.google.cloud.vertexai.api.SafetySetting$HarmBlockThreshold",
                "com.google.cloud.vertexai.api.SafetyRating",
                "com.google.cloud.vertexai.api.SafetyRating$Builder").methods().fields().build());

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
                "com.google.cloud.vertexai.api.CountTokensResponse$Builder",
                "com.google.cloud.vertexai.api.ComputeTokensRequest",
                "com.google.cloud.vertexai.api.ComputeTokensRequest$Builder",
                "com.google.cloud.vertexai.api.ComputeTokensResponse",
                "com.google.cloud.vertexai.api.ComputeTokensResponse$Builder").methods().fields().build());

        // Schema classes for structured output
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.Schema",
                "com.google.cloud.vertexai.api.Schema$Builder",
                "com.google.cloud.vertexai.api.Type").methods().fields().build());

        // Grounding and Google Search retrieval classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.GoogleSearchRetrieval",
                "com.google.cloud.vertexai.api.GoogleSearchRetrieval$Builder",
                "com.google.cloud.vertexai.api.DynamicRetrievalConfig",
                "com.google.cloud.vertexai.api.DynamicRetrievalConfig$Builder",
                "com.google.cloud.vertexai.api.DynamicRetrievalConfig$Mode",
                "com.google.cloud.vertexai.api.GroundingMetadata",
                "com.google.cloud.vertexai.api.GroundingMetadata$Builder",
                "com.google.cloud.vertexai.api.SearchEntryPoint",
                "com.google.cloud.vertexai.api.SearchEntryPoint$Builder",
                "com.google.cloud.vertexai.api.GroundingChunk",
                "com.google.cloud.vertexai.api.GroundingChunk$Builder",
                "com.google.cloud.vertexai.api.GroundingSupport",
                "com.google.cloud.vertexai.api.GroundingSupport$Builder").methods().fields().build());

        // Retrieval and RAG classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.Retrieval",
                "com.google.cloud.vertexai.api.Retrieval$Builder",
                "com.google.cloud.vertexai.api.VertexAISearch",
                "com.google.cloud.vertexai.api.VertexAISearch$Builder",
                "com.google.cloud.vertexai.api.RetrievalConfig",
                "com.google.cloud.vertexai.api.RetrievalConfig$Builder",
                "com.google.cloud.vertexai.api.RagRetrievalConfig",
                "com.google.cloud.vertexai.api.RagRetrievalConfig$Builder",
                "com.google.cloud.vertexai.api.RetrievalMetadata",
                "com.google.cloud.vertexai.api.RetrievalMetadata$Builder").methods().fields().build());

        // Code execution classes
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.ExecutableCode",
                "com.google.cloud.vertexai.api.ExecutableCode$Builder",
                "com.google.cloud.vertexai.api.CodeExecutionResult",
                "com.google.cloud.vertexai.api.CodeExecutionResult$Builder").methods().fields().build());

        // Enterprise web search
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.cloud.vertexai.api.EnterpriseWebSearch",
                "com.google.cloud.vertexai.api.EnterpriseWebSearch$Builder").methods().fields().build());

        // Protobuf utility classes commonly used
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(
                "com.google.protobuf.Struct",
                "com.google.protobuf.Struct$Builder",
                "com.google.protobuf.Value",
                "com.google.protobuf.Value$Builder",
                "com.google.protobuf.ListValue",
                "com.google.protobuf.ListValue$Builder",
                "com.google.protobuf.Duration",
                "com.google.protobuf.Duration$Builder").methods().fields().build());
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
