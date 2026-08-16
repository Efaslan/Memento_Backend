package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.response.BasicStringResponse;
import com.emiraslan.memento.entity.user.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiService {

    private final ChatClient chatClient;

    // reading the prompts from StringTemplate files
    @Value("classpath:prompts/daily-log-formatter.st")
    private Resource dailyLogFormatterPrompt;

    @Value("classpath:prompts/memento-chatbot.st")
    private Resource chatbotPrompt;

    public AiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // Formatting long DailyLog descriptions from user voices into concise summaries
    public String formatDailyLog(String descriptionFromSpeech, User user) {
        log.info("Sending UserId: {}'s raw DailyLog description to Groq for formatting: {}", user.getUserId(), descriptionFromSpeech);

        try {
            return chatClient.prompt()
                    // System Prompt is our daily-log-formatter.st prompt
                    .system(dailyLogFormatterPrompt)
                    // User Prompt is what the user's original DailyLog description is
                    .user(descriptionFromSpeech)
                    .call()
                    .content(); // taking only the returned string from AI
        } catch (Exception e) {
            log.error("Failed to format text with Groq AI: {}", e.getMessage());
            // Returning the original Text-To-Speech description if the AI fails to format it.
            throw new RuntimeException("AI_DAILY_LOG_FORMATTING_FAILED", e);
        }
    }

    // AI Assistant helps users with their questions about the app
    public BasicStringResponse chatWithMementoAssistant(String question, User user) {
        log.info("UserId: {} asked Memento Assistant: {}", user.getUserId(), question);

        try {
            return new BasicStringResponse(chatClient.prompt()
                    .system(chatbotPrompt)
                    .user(question)
                    .call()
                    .content());
        } catch (Exception e) {
            log.error("Failed to get response from Groq AI: {}", e.getMessage());
            return new BasicStringResponse("AI_ASSISTANT_FAILED_TO_RESPOND");
        }
    }
}