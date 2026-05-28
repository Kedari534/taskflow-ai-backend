package com.aitaskmanager.controller;

import com.aitaskmanager.dto.AIRequest;
import com.aitaskmanager.service.GeminiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/ai")
public class AIController {
    @Autowired
    GeminiService geminiService;

    @PostMapping("/generate")
    public Map<String, Object> generateTask(@RequestBody AIRequest request) {
        return geminiService.generateTaskDetails(request.getTitle());
    }

    @PostMapping("/chat")
    public Map<String, Object> chatWithAI(@RequestBody Map<String, Object> requestBody) {
        String message = (String) requestBody.get("message");
        List<Map<String, String>> history = (List<Map<String, String>>) requestBody.get("history");
        return geminiService.chatWithAI(message, history);
    }

    @GetMapping("/models")
    public String getModels() {
        return geminiService.listModels();
    }
}
