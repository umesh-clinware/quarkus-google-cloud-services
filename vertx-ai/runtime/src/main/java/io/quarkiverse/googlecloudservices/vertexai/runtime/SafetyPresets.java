package io.quarkiverse.googlecloudservices.vertexai.runtime;

import java.util.Arrays;
import java.util.List;

import com.google.cloud.vertexai.api.HarmCategory;
import com.google.cloud.vertexai.api.SafetySetting;

/**
 * Predefined safety setting configurations for different use cases.
 *
 * <p>
 * Safety settings control how the model handles potentially harmful content.
 * Different applications require different safety thresholds - for example,
 * healthcare applications may need to allow medical discussions that could
 * otherwise be blocked.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 * GenerativeModel model = geminiBuilder.newBuilder()
 *         .model("gemini-2.5-flash-preview-05-20")
 *         .safetySettings(SafetyPresets.HEALTHCARE)
 *         .build();
 * </pre>
 *
 * @see GeminiModelBuilder
 */
public final class SafetyPresets {

    private SafetyPresets() {
        // Utility class
    }

    /**
     * Default safety settings - balanced protection.
     *
     * <p>
     * Blocks medium and high probability harmful content across all categories.
     */
    public static final List<SafetySetting> DEFAULT = Arrays.asList(
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                    .build());

    /**
     * Healthcare-optimized safety settings.
     *
     * <p>
     * Allows medical discussions that might involve sensitive topics like:
     * <ul>
     * <li>Medical procedures and treatments</li>
     * <li>Drug dosages and side effects</li>
     * <li>Mental health discussions</li>
     * <li>Anatomical descriptions</li>
     * </ul>
     *
     * <p>
     * Still blocks clearly harmful content (high probability only).
     */
    public static final List<SafetySetting> HEALTHCARE = Arrays.asList(
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH)
                    .build());

    /**
     * Strict safety settings for child-safe applications.
     *
     * <p>
     * Blocks any content that might be inappropriate for minors.
     */
    public static final List<SafetySetting> STRICT = Arrays.asList(
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_LOW_AND_ABOVE)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_LOW_AND_ABOVE)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_LOW_AND_ABOVE)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_LOW_AND_ABOVE)
                    .build());

    /**
     * Permissive settings for internal/research use only.
     *
     * <p>
     * <strong>WARNING:</strong> Only use in controlled environments.
     * Allows most content through for analysis or research purposes.
     */
    public static final List<SafetySetting> PERMISSIVE = Arrays.asList(
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_ONLY_HIGH)
                    .build());

    /**
     * No blocking - for testing only.
     *
     * <p>
     * <strong>WARNING:</strong> Do not use in production.
     * All content passes through without filtering.
     */
    public static final List<SafetySetting> NONE = Arrays.asList(
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_HATE_SPEECH)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_NONE)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_NONE)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_NONE)
                    .build(),
            SafetySetting.newBuilder()
                    .setCategory(HarmCategory.HARM_CATEGORY_HARASSMENT)
                    .setThreshold(SafetySetting.HarmBlockThreshold.BLOCK_NONE)
                    .build());
}
