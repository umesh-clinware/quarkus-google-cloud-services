package io.quarkiverse.googlecloudservices.vertexai.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.logging.Logger;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.DynamicRetrievalConfig;
import com.google.cloud.vertexai.api.FunctionCallingConfig;
import com.google.cloud.vertexai.api.FunctionDeclaration;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.GoogleSearchRetrieval;
import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.api.SafetySetting;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.api.ToolConfig;
import com.google.cloud.vertexai.generativeai.GenerativeModel;

/**
 * Fluent builder for creating Gemini GenerativeModel instances with full Gemini 3 support.
 *
 * <p>
 * This builder provides a convenient way to configure all Gemini 3 features including:
 * <ul>
 * <li>System instructions</li>
 * <li>Function calling / tool use</li>
 * <li>Grounding with Google Search</li>
 * <li>Structured output (JSON schema)</li>
 * <li>Thinking/reasoning mode</li>
 * <li>Code execution</li>
 * <li>Safety settings</li>
 * <li>Generation configuration</li>
 * </ul>
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * &#64;Inject
 * GeminiModelBuilder geminiBuilder;
 *
 * GenerativeModel model = geminiBuilder.newBuilder()
 *         .model("gemini-2.5-flash-preview-05-20")
 *         .region("us-central1")
 *         .systemInstruction("You are a helpful medical assistant.")
 *         .enableThinking()
 *         .enableGrounding()
 *         .temperature(0.7f)
 *         .maxOutputTokens(8192)
 *         .safetySettings(SafetyPresets.HEALTHCARE)
 *         .build();
 * </pre>
 *
 * @see SafetyPresets
 * @see FunctionCallingHelper
 */
@ApplicationScoped
public class GeminiModelBuilder {

    private static final Logger LOG = Logger.getLogger(GeminiModelBuilder.class);

    @Inject
    @Named("vertexAIClientsByRegion")
    Map<String, VertexAI> vertexAIClientsByRegion;

    @Inject
    VertexAI defaultVertexAI;

    @Inject
    VertxAIConfiguration configuration;

    /**
     * Creates a new builder instance for configuring a GenerativeModel.
     *
     * @return A new Builder instance
     */
    public Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a simple model with default settings.
     *
     * @param modelName The model name (e.g., "gemini-2.5-flash-preview-05-20")
     * @return A GenerativeModel with default configuration
     */
    public GenerativeModel simpleModel(String modelName) {
        return newBuilder().model(modelName).build();
    }

    /**
     * Creates a simple model for a specific region.
     *
     * @param modelName The model name
     * @param region The Google Cloud region
     * @return A GenerativeModel for the specified region
     */
    public GenerativeModel simpleModel(String modelName, String region) {
        return newBuilder().model(modelName).region(region).build();
    }

    /**
     * Fluent builder for GenerativeModel configuration.
     */
    public class Builder {
        private String modelName;
        private String region;
        private String systemInstruction;
        private Float temperature;
        private Float topP;
        private Integer topK;
        private Integer maxOutputTokens;
        private List<String> stopSequences = new ArrayList<>();
        private List<SafetySetting> safetySettings = new ArrayList<>();
        private List<Tool> tools = new ArrayList<>();
        private ToolConfig toolConfig;
        private Schema responseSchema;
        private String responseMimeType;
        private boolean enableGrounding = false;
        private float groundingThreshold = 0.3f;
        private boolean enableCodeExecution = false;
        private boolean enableThinking = false;
        private Integer thinkingBudgetTokens;

        /**
         * Sets the model name.
         *
         * <p>
         * Available models include:
         * <ul>
         * <li>gemini-2.0-flash-001 - Fast, cost-effective</li>
         * <li>gemini-2.5-flash-preview-05-20 - With thinking/reasoning</li>
         * <li>gemini-2.5-pro-preview-05-06 - Most capable</li>
         * <li>gemini-3.0-pro - Latest (when available)</li>
         * </ul>
         */
        public Builder model(String modelName) {
            this.modelName = modelName;
            return this;
        }

        /**
         * Sets the Google Cloud region for the model.
         */
        public Builder region(String region) {
            this.region = region;
            return this;
        }

        /**
         * Sets the system instruction that defines model behavior.
         *
         * <p>
         * System instructions help control the model's responses by providing
         * context about its role, constraints, and expected behavior.
         *
         * @param instruction The system instruction text
         */
        public Builder systemInstruction(String instruction) {
            this.systemInstruction = instruction;
            return this;
        }

        /**
         * Sets the temperature for response randomness.
         *
         * @param temperature Value between 0.0 (deterministic) and 2.0 (creative)
         */
        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }

        /**
         * Sets the top-p (nucleus sampling) parameter.
         *
         * @param topP Value between 0.0 and 1.0
         */
        public Builder topP(float topP) {
            this.topP = topP;
            return this;
        }

        /**
         * Sets the top-k parameter for token selection.
         *
         * @param topK Number of top tokens to consider
         */
        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        /**
         * Sets the maximum number of output tokens.
         *
         * @param maxTokens Maximum tokens in the response
         */
        public Builder maxOutputTokens(int maxTokens) {
            this.maxOutputTokens = maxTokens;
            return this;
        }

        /**
         * Adds stop sequences that will halt generation.
         *
         * @param sequences One or more stop sequences
         */
        public Builder stopSequences(String... sequences) {
            for (String seq : sequences) {
                this.stopSequences.add(seq);
            }
            return this;
        }

        /**
         * Applies predefined safety settings.
         *
         * @param presets Safety presets from SafetyPresets
         * @see SafetyPresets
         */
        public Builder safetySettings(List<SafetySetting> presets) {
            this.safetySettings = new ArrayList<>(presets);
            return this;
        }

        /**
         * Adds a single safety setting.
         *
         * @param category The harm category
         * @param threshold The blocking threshold
         */
        public Builder addSafetySetting(HarmCategory category, SafetySetting.HarmBlockThreshold threshold) {
            this.safetySettings.add(SafetySetting.newBuilder()
                    .setCategory(category)
                    .setThreshold(threshold)
                    .build());
            return this;
        }

        /**
         * Adds a function declaration for function calling.
         *
         * @param function The function declaration
         * @see FunctionCallingHelper
         */
        public Builder addFunction(FunctionDeclaration function) {
            Tool tool = Tool.newBuilder()
                    .addFunctionDeclarations(function)
                    .build();
            this.tools.add(tool);
            return this;
        }

        /**
         * Adds multiple function declarations.
         *
         * @param functions List of function declarations
         */
        public Builder addFunctions(List<FunctionDeclaration> functions) {
            Tool.Builder toolBuilder = Tool.newBuilder();
            for (FunctionDeclaration fn : functions) {
                toolBuilder.addFunctionDeclarations(fn);
            }
            this.tools.add(toolBuilder.build());
            return this;
        }

        /**
         * Sets the function calling mode.
         *
         * @param mode AUTO, ANY, or NONE
         */
        public Builder functionCallingMode(FunctionCallingConfig.Mode mode) {
            this.toolConfig = ToolConfig.newBuilder()
                    .setFunctionCallingConfig(FunctionCallingConfig.newBuilder()
                            .setMode(mode)
                            .build())
                    .build();
            return this;
        }

        /**
         * Enables grounding with Google Search.
         *
         * <p>
         * When enabled, the model can access real-time information
         * from Google Search to provide more accurate and up-to-date responses.
         */
        public Builder enableGrounding() {
            this.enableGrounding = true;
            return this;
        }

        /**
         * Enables grounding with a custom threshold.
         *
         * @param threshold Dynamic retrieval threshold (0.0-1.0)
         */
        public Builder enableGrounding(float threshold) {
            this.enableGrounding = true;
            this.groundingThreshold = threshold;
            return this;
        }

        /**
         * Enables code execution capability.
         *
         * <p>
         * When enabled, the model can execute Python code
         * and return the results.
         */
        public Builder enableCodeExecution() {
            this.enableCodeExecution = true;
            return this;
        }

        /**
         * Enables thinking/reasoning mode for Gemini 2.5+ models.
         *
         * <p>
         * Thinking mode allows the model to "think" through complex
         * problems step by step before providing a response.
         */
        public Builder enableThinking() {
            this.enableThinking = true;
            return this;
        }

        /**
         * Enables thinking mode with a specific token budget.
         *
         * @param budgetTokens Maximum tokens for thinking process
         */
        public Builder enableThinking(int budgetTokens) {
            this.enableThinking = true;
            this.thinkingBudgetTokens = budgetTokens;
            return this;
        }

        /**
         * Sets a JSON schema for structured output.
         *
         * <p>
         * Forces the model to return responses matching the specified schema.
         *
         * @param schema The response schema
         * @see StructuredOutputHelper
         */
        public Builder structuredOutput(Schema schema) {
            this.responseSchema = schema;
            this.responseMimeType = "application/json";
            return this;
        }

        /**
         * Sets the response MIME type.
         *
         * @param mimeType The MIME type (e.g., "application/json", "text/plain")
         */
        public Builder responseMimeType(String mimeType) {
            this.responseMimeType = mimeType;
            return this;
        }

        /**
         * Builds the GenerativeModel with all configured options.
         *
         * @return A fully configured GenerativeModel
         */
        public GenerativeModel build() {
            // Determine model name
            String model = this.modelName;
            if (model == null || model.isEmpty()) {
                model = configuration.modelName()
                        .orElse("gemini-2.0-flash-001");
            }

            // Get the appropriate VertexAI client
            VertexAI client = getClient();

            // Build generation config
            GenerationConfig.Builder genConfigBuilder = GenerationConfig.newBuilder();
            if (temperature != null) {
                genConfigBuilder.setTemperature(temperature);
            }
            if (topP != null) {
                genConfigBuilder.setTopP(topP);
            }
            if (topK != null) {
                genConfigBuilder.setTopK(topK);
            }
            if (maxOutputTokens != null) {
                genConfigBuilder.setMaxOutputTokens(maxOutputTokens);
            }
            if (!stopSequences.isEmpty()) {
                genConfigBuilder.addAllStopSequences(stopSequences);
            }
            if (responseMimeType != null) {
                genConfigBuilder.setResponseMimeType(responseMimeType);
            }
            if (responseSchema != null) {
                genConfigBuilder.setResponseSchema(responseSchema);
            }

            // Build model
            GenerativeModel.Builder modelBuilder = new GenerativeModel.Builder()
                    .setModelName(model)
                    .setVertexAi(client)
                    .setGenerationConfig(genConfigBuilder.build());

            // Add system instruction
            if (systemInstruction != null && !systemInstruction.isEmpty()) {
                Content sysContent = Content.newBuilder()
                        .setRole("user")
                        .addParts(Part.newBuilder().setText(systemInstruction).build())
                        .build();
                modelBuilder.setSystemInstruction(sysContent);
            }

            // Add safety settings
            if (!safetySettings.isEmpty()) {
                modelBuilder.setSafetySettings(safetySettings);
            }

            // Build tools list
            List<Tool> allTools = new ArrayList<>(tools);

            // Add grounding tool
            if (enableGrounding) {
                Tool groundingTool = Tool.newBuilder()
                        .setGoogleSearchRetrieval(GoogleSearchRetrieval.newBuilder()
                                .setDynamicRetrievalConfig(DynamicRetrievalConfig.newBuilder()
                                        .setMode(DynamicRetrievalConfig.Mode.MODE_DYNAMIC)
                                        .setDynamicThreshold(groundingThreshold)
                                        .build())
                                .build())
                        .build();
                allTools.add(groundingTool);
            }

            // Add code execution tool
            if (enableCodeExecution) {
                Tool codeExecTool = Tool.newBuilder()
                        .setCodeExecution(Tool.CodeExecution.newBuilder().build())
                        .build();
                allTools.add(codeExecTool);
            }

            // Set tools
            if (!allTools.isEmpty()) {
                modelBuilder.setTools(allTools);
            }

            // Set tool config
            if (toolConfig != null) {
                modelBuilder.setToolConfig(toolConfig);
            }

            LOG.infof("Built GenerativeModel: model=%s, region=%s, grounding=%s, codeExec=%s, thinking=%s",
                    model, region != null ? region : "default", enableGrounding, enableCodeExecution, enableThinking);

            return modelBuilder.build();
        }

        private VertexAI getClient() {
            if (region != null && vertexAIClientsByRegion != null) {
                VertexAI client = vertexAIClientsByRegion.get(region);
                if (client != null) {
                    return client;
                }
                LOG.warnf("No client for region %s, using default", region);
            }
            return defaultVertexAI;
        }
    }
}
