package com.emiraslan.memento.controller;

import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.service.AiService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assistant")
@RequiredArgsConstructor
@Tag(name = "13 - AI Assistant")
@SecurityRequirement(name = "bearerAuth")
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ResponseEntity<String> chatWithAssistant(
            @RequestBody String question,
            @AuthenticationPrincipal User user) {

        String answer = aiService.chatWithMementoAssistant(question, user);
        return ResponseEntity.ok(answer);
    }
}
