package io.quarkiverse.googlecloudservices.vertexai.runtime;

import com.google.cloud.vertexai.api.Schema;
import com.google.cloud.vertexai.api.Type;

/**
 * Helper class for creating JSON schemas for structured output.
 *
 * <p>
 * Structured output forces Gemini to return responses in a specific JSON format,
 * making it easier to parse and process the results programmatically.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * // Create a schema for a patient note
 * Schema patientNoteSchema = StructuredOutputHelper.object()
 *         .description("A clinical patient note")
 *         .addProperty("patientId", StructuredOutputHelper.string("Patient ID"))
 *         .addProperty("chiefComplaint", StructuredOutputHelper.string("Chief complaint"))
 *         .addProperty("diagnosis", StructuredOutputHelper.array("List of diagnoses",
 *                 StructuredOutputHelper.object()
 *                         .addProperty("code", StructuredOutputHelper.string("ICD-10 code"))
 *                         .addProperty("description", StructuredOutputHelper.string("Diagnosis description"))
 *                         .build()))
 *         .addProperty("medications", StructuredOutputHelper.array("Prescribed medications",
 *                 StructuredOutputHelper.string("Medication name")))
 *         .addRequired("patientId", "chiefComplaint")
 *         .build();
 *
 * // Use in model builder
 * GenerativeModel model = geminiBuilder.newBuilder()
 *         .model("gemini-2.5-flash-preview-05-20")
 *         .structuredOutput(patientNoteSchema)
 *         .build();
 * </pre>
 *
 * @see GeminiModelBuilder
 */
public final class StructuredOutputHelper {

    private StructuredOutputHelper() {
        // Utility class
    }

    /**
     * Creates a string schema.
     *
     * @param description Description of the string field
     * @return A STRING type schema
     */
    public static Schema string(String description) {
        return Schema.newBuilder()
                .setType(Type.STRING)
                .setDescription(description)
                .build();
    }

    /**
     * Creates a string schema without description.
     *
     * @return A STRING type schema
     */
    public static Schema string() {
        return Schema.newBuilder()
                .setType(Type.STRING)
                .build();
    }

    /**
     * Creates an integer schema.
     *
     * @param description Description of the integer field
     * @return An INTEGER type schema
     */
    public static Schema integer(String description) {
        return Schema.newBuilder()
                .setType(Type.INTEGER)
                .setDescription(description)
                .build();
    }

    /**
     * Creates a number (float/double) schema.
     *
     * @param description Description of the number field
     * @return A NUMBER type schema
     */
    public static Schema number(String description) {
        return Schema.newBuilder()
                .setType(Type.NUMBER)
                .setDescription(description)
                .build();
    }

    /**
     * Creates a boolean schema.
     *
     * @param description Description of the boolean field
     * @return A BOOLEAN type schema
     */
    public static Schema bool(String description) {
        return Schema.newBuilder()
                .setType(Type.BOOLEAN)
                .setDescription(description)
                .build();
    }

    /**
     * Creates an enum schema with allowed values.
     *
     * @param description Description of the enum field
     * @param values Allowed string values
     * @return A STRING type schema with enum values
     */
    public static Schema enumOf(String description, String... values) {
        Schema.Builder builder = Schema.newBuilder()
                .setType(Type.STRING)
                .setDescription(description);
        for (String val : values) {
            builder.addEnum(val);
        }
        return builder.build();
    }

    /**
     * Creates an array schema with items of a specific type.
     *
     * @param description Description of the array
     * @param itemSchema Schema for array items
     * @return An ARRAY type schema
     */
    public static Schema array(String description, Schema itemSchema) {
        return Schema.newBuilder()
                .setType(Type.ARRAY)
                .setDescription(description)
                .setItems(itemSchema)
                .build();
    }

    /**
     * Creates an array of strings.
     *
     * @param description Description of the array
     * @return An ARRAY type schema with STRING items
     */
    public static Schema stringArray(String description) {
        return array(description, string());
    }

    /**
     * Creates an object schema builder.
     *
     * @return A new ObjectSchemaBuilder
     */
    public static ObjectSchemaBuilder object() {
        return new ObjectSchemaBuilder();
    }

    /**
     * Builder for creating object schemas.
     */
    public static class ObjectSchemaBuilder {
        private final Schema.Builder builder;

        ObjectSchemaBuilder() {
            this.builder = Schema.newBuilder().setType(Type.OBJECT);
        }

        /**
         * Sets the object description.
         */
        public ObjectSchemaBuilder description(String description) {
            builder.setDescription(description);
            return this;
        }

        /**
         * Adds a property to the object.
         *
         * @param name Property name
         * @param schema Property schema
         */
        public ObjectSchemaBuilder addProperty(String name, Schema schema) {
            builder.putProperties(name, schema);
            return this;
        }

        /**
         * Adds a required string property.
         *
         * @param name Property name
         * @param description Property description
         */
        public ObjectSchemaBuilder addString(String name, String description) {
            builder.putProperties(name, string(description));
            return this;
        }

        /**
         * Adds a required integer property.
         *
         * @param name Property name
         * @param description Property description
         */
        public ObjectSchemaBuilder addInteger(String name, String description) {
            builder.putProperties(name, integer(description));
            return this;
        }

        /**
         * Adds a required number property.
         *
         * @param name Property name
         * @param description Property description
         */
        public ObjectSchemaBuilder addNumber(String name, String description) {
            builder.putProperties(name, number(description));
            return this;
        }

        /**
         * Adds a required boolean property.
         *
         * @param name Property name
         * @param description Property description
         */
        public ObjectSchemaBuilder addBoolean(String name, String description) {
            builder.putProperties(name, bool(description));
            return this;
        }

        /**
         * Adds an enum property.
         *
         * @param name Property name
         * @param description Property description
         * @param values Allowed values
         */
        public ObjectSchemaBuilder addEnum(String name, String description, String... values) {
            builder.putProperties(name, enumOf(description, values));
            return this;
        }

        /**
         * Adds an array property.
         *
         * @param name Property name
         * @param description Array description
         * @param itemSchema Schema for array items
         */
        public ObjectSchemaBuilder addArray(String name, String description, Schema itemSchema) {
            builder.putProperties(name, array(description, itemSchema));
            return this;
        }

        /**
         * Adds a nested object property.
         *
         * @param name Property name
         * @param objectSchema The nested object schema
         */
        public ObjectSchemaBuilder addObject(String name, Schema objectSchema) {
            builder.putProperties(name, objectSchema);
            return this;
        }

        /**
         * Marks properties as required.
         *
         * @param names Names of required properties
         */
        public ObjectSchemaBuilder addRequired(String... names) {
            for (String name : names) {
                builder.addRequired(name);
            }
            return this;
        }

        /**
         * Builds the schema.
         *
         * @return The completed Schema
         */
        public Schema build() {
            return builder.build();
        }
    }

    // ========== Common Healthcare Schemas ==========

    /**
     * Creates a schema for a patient identifier.
     */
    public static Schema patientId() {
        return object()
                .description("Patient identifier")
                .addString("mrn", "Medical Record Number")
                .addString("system", "Identifier system (e.g., hospital name)")
                .addRequired("mrn")
                .build();
    }

    /**
     * Creates a schema for an ICD-10 diagnosis.
     */
    public static Schema diagnosis() {
        return object()
                .description("Clinical diagnosis")
                .addString("code", "ICD-10 diagnosis code")
                .addString("description", "Human-readable diagnosis description")
                .addEnum("status", "Diagnosis status", "active", "resolved", "inactive")
                .addRequired("code", "description")
                .build();
    }

    /**
     * Creates a schema for a medication.
     */
    public static Schema medication() {
        return object()
                .description("Medication prescription")
                .addString("name", "Medication name")
                .addString("dosage", "Dosage amount and unit")
                .addString("frequency", "How often to take (e.g., 'twice daily')")
                .addString("route", "Administration route (e.g., 'oral', 'IV')")
                .addString("instructions", "Additional instructions")
                .addRequired("name", "dosage", "frequency")
                .build();
    }

    /**
     * Creates a schema for a clinical note summary.
     */
    public static Schema clinicalNoteSummary() {
        return object()
                .description("Summary of a clinical note")
                .addString("chiefComplaint", "Patient's main complaint")
                .addString("historyOfPresentIllness", "Description of current illness")
                .addArray("diagnoses", "List of diagnoses", diagnosis())
                .addArray("medications", "List of medications", medication())
                .addString("plan", "Treatment plan")
                .addString("followUp", "Follow-up instructions")
                .addRequired("chiefComplaint", "diagnoses", "plan")
                .build();
    }

    /**
     * Creates a schema for vital signs.
     */
    public static Schema vitalSigns() {
        return object()
                .description("Patient vital signs")
                .addNumber("temperature", "Body temperature in Fahrenheit")
                .addInteger("heartRate", "Heart rate in beats per minute")
                .addInteger("systolicBP", "Systolic blood pressure in mmHg")
                .addInteger("diastolicBP", "Diastolic blood pressure in mmHg")
                .addInteger("respiratoryRate", "Respiratory rate per minute")
                .addInteger("oxygenSaturation", "Oxygen saturation percentage")
                .build();
    }
}
