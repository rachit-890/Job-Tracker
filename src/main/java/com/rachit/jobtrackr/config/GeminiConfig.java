package com.rachit.jobtrackr.config;

import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${jobtrackr.gemini.api-key}")
    private String apiKey;

    @Value("${jobtrackr.gemini.chat-model}")
    private String chatModelName;

    @Value("${jobtrackr.gemini.embedding-model}")
    private String embeddingModelName;

    @Bean
    public GoogleAiGeminiChatModel geminiChatModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModelName)
                .build();
    }

    // FIX: correct class is GoogleAiEmbeddingModel, not GoogleAiGeminiEmbeddingModel
    @Bean
    public GoogleAiEmbeddingModel geminiEmbeddingModel() {
        return GoogleAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName(embeddingModelName)
                .build();
    }
}