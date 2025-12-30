package io.quarkiverse.googlecloudservices.vertexai.runtime;

import java.util.Map;
import java.util.function.Function;

import com.google.cloud.vertexai.api.FunctionCall;
import com.google.cloud.vertexai.api.FunctionDeclaration;
import com.google.cloud.vertexai.api.FunctionResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Type;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Helper class for creating and handling function calls with Gemini.
 *
 * <p>
 * Function calling allows Gemini to invoke external functions/APIs and use
 * their results in generating responses. This is useful for:
 * <ul>
 * <li>Fetching real-time data (e.g., patient records, lab results)</li>
 * <li>Performing actions (e.g., scheduling appointments)</li>
 * <li>Accessing external systems (e.g., EHR, pharmacy systems)</li>
 * </ul>
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * // Define a function
 * FunctionDeclaration getPatientFn = FunctionCallingHelper.function("getPatient")
 *         .description("Retrieves patient information by ID")
 *         .addStringParam("patientId", "The patient's unique identifier", true)
 *         .build();
 *
 * // Add to model
 * GenerativeModel model = geminiBuilder.newBuilder()
 *         .model("gemini-2.5-flash-preview-05-20")
 *         .addFunction(getPatientFn)
 *         .build();
 *
 * // Handle function call response
 * if (FunctionCallingHelper.hasFunctionCall(response)) {
 *     FunctionCall call = FunctionCallingHelper.getFunctionCall(response);
 *     Map&lt;String, Object&gt; args = FunctionCallingHelper.getArguments(call);
 *
 *     // Execute your function
 *     Patient patient = patientService.getPatient((String) args.get("patientId"));
 *
 *     // Create response
 *     FunctionResponse fnResponse = FunctionCallingHelper.response(call.getName())
 *             .addResult("name", patient.getName())
 *             .addResult("dob", patient.getDateOfBirth())
 *             .build();
 * }
 * </pre>
 */
public final class FunctionCallingHelper {

    private FunctionCallingHelper() {
        // Utility class
    }

    /**
     * Creates a new function declaration builder.
     *
     * @param name The function name (should be descriptive and use camelCase)
     * @return A new FunctionBuilder
     */
    public static FunctionBuilder function(String name) {
        return new FunctionBuilder(name);
    }

    /**
     * Creates a function response builder.
     *
     * @param functionName The name of the function being responded to
     * @return A new ResponseBuilder
     */
    public static ResponseBuilder response(String functionName) {
        return new ResponseBuilder(functionName);
    }

    /**
     * Checks if a response contains a function call.
     *
     * @param response The generate content response
     * @return true if a function call is present
     */
    public static boolean hasFunctionCall(com.google.cloud.vertexai.api.GenerateContentResponse response) {
        if (response == null || response.getCandidatesList().isEmpty()) {
            return false;
        }
        var candidate = response.getCandidates(0);
        if (candidate.getContent() == null || candidate.getContent().getPartsList().isEmpty()) {
            return false;
        }
        return candidate.getContent().getParts(0).hasFunctionCall();
    }

    /**
     * Extracts the function call from a response.
     *
     * @param response The generate content response
     * @return The FunctionCall, or null if none present
     */
    public static FunctionCall getFunctionCall(com.google.cloud.vertexai.api.GenerateContentResponse response) {
        if (!hasFunctionCall(response)) {
            return null;
        }
        return response.getCandidates(0).getContent().getParts(0).getFunctionCall();
    }

    /**
     * Extracts arguments from a function call as a Map.
     *
     * @param call The function call
     * @return Map of argument names to values
     */
    public static Map<String, Object> getArguments(FunctionCall call) {
        if (call == null || call.getArgs() == null) {
            return Map.of();
        }
        return structToMap(call.getArgs());
    }

    /**
     * Creates a Part containing a function response.
     *
     * @param functionResponse The function response
     * @return A Part for including in content
     */
    public static Part toPart(FunctionResponse functionResponse) {
        return Part.newBuilder()
                .setFunctionResponse(functionResponse)
                .build();
    }

    private static Map<String, Object> structToMap(Struct struct) {
        java.util.HashMap<String, Object> map = new java.util.HashMap<>();
        for (Map.Entry<String, Value> entry : struct.getFieldsMap().entrySet()) {
            map.put(entry.getKey(), valueToObject(entry.getValue()));
        }
        return map;
    }

    private static Object valueToObject(Value value) {
        switch (value.getKindCase()) {
            case STRING_VALUE:
                return value.getStringValue();
            case NUMBER_VALUE:
                return value.getNumberValue();
            case BOOL_VALUE:
                return value.getBoolValue();
            case STRUCT_VALUE:
                return structToMap(value.getStructValue());
            case LIST_VALUE:
                return value.getListValue().getValuesList().stream()
                        .map(FunctionCallingHelper::valueToObject)
                        .toList();
            case NULL_VALUE:
            default:
                return null;
        }
    }

    /**
     * Builder for creating function declarations.
     */
    public static class FunctionBuilder {
        private final String name;
        private String description;
        private final Schema.Builder parametersBuilder;

        FunctionBuilder(String name) {
            this.name = name;
            this.parametersBuilder = Schema.newBuilder().setType(Type.OBJECT);
        }

        /**
         * Sets the function description.
         *
         * @param description Human-readable description of what the function does
         */
        public FunctionBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Adds a string parameter.
         *
         * @param name Parameter name
         * @param description Parameter description
         * @param required Whether the parameter is required
         */
        public FunctionBuilder addStringParam(String name, String description, boolean required) {
            Schema paramSchema = Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription(description)
                    .build();
            parametersBuilder.putProperties(name, paramSchema);
            if (required) {
                parametersBuilder.addRequired(name);
            }
            return this;
        }

        /**
         * Adds an integer parameter.
         *
         * @param name Parameter name
         * @param description Parameter description
         * @param required Whether the parameter is required
         */
        public FunctionBuilder addIntParam(String name, String description, boolean required) {
            Schema paramSchema = Schema.newBuilder()
                    .setType(Type.INTEGER)
                    .setDescription(description)
                    .build();
            parametersBuilder.putProperties(name, paramSchema);
            if (required) {
                parametersBuilder.addRequired(name);
            }
            return this;
        }

        /**
         * Adds a number (float/double) parameter.
         *
         * @param name Parameter name
         * @param description Parameter description
         * @param required Whether the parameter is required
         */
        public FunctionBuilder addNumberParam(String name, String description, boolean required) {
            Schema paramSchema = Schema.newBuilder()
                    .setType(Type.NUMBER)
                    .setDescription(description)
                    .build();
            parametersBuilder.putProperties(name, paramSchema);
            if (required) {
                parametersBuilder.addRequired(name);
            }
            return this;
        }

        /**
         * Adds a boolean parameter.
         *
         * @param name Parameter name
         * @param description Parameter description
         * @param required Whether the parameter is required
         */
        public FunctionBuilder addBoolParam(String name, String description, boolean required) {
            Schema paramSchema = Schema.newBuilder()
                    .setType(Type.BOOLEAN)
                    .setDescription(description)
                    .build();
            parametersBuilder.putProperties(name, paramSchema);
            if (required) {
                parametersBuilder.addRequired(name);
            }
            return this;
        }

        /**
         * Adds an enum parameter with allowed values.
         *
         * @param name Parameter name
         * @param description Parameter description
         * @param required Whether the parameter is required
         * @param values Allowed enum values
         */
        public FunctionBuilder addEnumParam(String name, String description, boolean required, String... values) {
            Schema.Builder paramBuilder = Schema.newBuilder()
                    .setType(Type.STRING)
                    .setDescription(description);
            for (String val : values) {
                paramBuilder.addEnum(val);
            }
            parametersBuilder.putProperties(name, paramBuilder.build());
            if (required) {
                parametersBuilder.addRequired(name);
            }
            return this;
        }

        /**
         * Adds an array parameter.
         *
         * @param name Parameter name
         * @param description Parameter description
         * @param itemType Type of array items (STRING, INTEGER, NUMBER, BOOLEAN)
         * @param required Whether the parameter is required
         */
        public FunctionBuilder addArrayParam(String name, String description, Type itemType, boolean required) {
            Schema itemSchema = Schema.newBuilder().setType(itemType).build();
            Schema paramSchema = Schema.newBuilder()
                    .setType(Type.ARRAY)
                    .setDescription(description)
                    .setItems(itemSchema)
                    .build();
            parametersBuilder.putProperties(name, paramSchema);
            if (required) {
                parametersBuilder.addRequired(name);
            }
            return this;
        }

        /**
         * Adds a custom schema parameter.
         *
         * @param name Parameter name
         * @param schema The parameter schema
         * @param required Whether the parameter is required
         */
        public FunctionBuilder addParam(String name, Schema schema, boolean required) {
            parametersBuilder.putProperties(name, schema);
            if (required) {
                parametersBuilder.addRequired(name);
            }
            return this;
        }

        /**
         * Builds the function declaration.
         *
         * @return The FunctionDeclaration
         */
        public FunctionDeclaration build() {
            FunctionDeclaration.Builder builder = FunctionDeclaration.newBuilder()
                    .setName(name);
            if (description != null) {
                builder.setDescription(description);
            }
            if (!parametersBuilder.getPropertiesMap().isEmpty()) {
                builder.setParameters(parametersBuilder.build());
            }
            return builder.build();
        }
    }

    /**
     * Builder for creating function responses.
     */
    public static class ResponseBuilder {
        private final String functionName;
        private final Struct.Builder structBuilder;

        ResponseBuilder(String functionName) {
            this.functionName = functionName;
            this.structBuilder = Struct.newBuilder();
        }

        /**
         * Adds a string result.
         */
        public ResponseBuilder addResult(String key, String value) {
            structBuilder.putFields(key, Value.newBuilder().setStringValue(value).build());
            return this;
        }

        /**
         * Adds a number result.
         */
        public ResponseBuilder addResult(String key, double value) {
            structBuilder.putFields(key, Value.newBuilder().setNumberValue(value).build());
            return this;
        }

        /**
         * Adds a boolean result.
         */
        public ResponseBuilder addResult(String key, boolean value) {
            structBuilder.putFields(key, Value.newBuilder().setBoolValue(value).build());
            return this;
        }

        /**
         * Adds an error result.
         */
        public ResponseBuilder addError(String errorMessage) {
            structBuilder.putFields("error", Value.newBuilder().setStringValue(errorMessage).build());
            return this;
        }

        /**
         * Builds the function response.
         *
         * @return The FunctionResponse
         */
        public FunctionResponse build() {
            return FunctionResponse.newBuilder()
                    .setName(functionName)
                    .setResponse(structBuilder.build())
                    .build();
        }
    }
}
