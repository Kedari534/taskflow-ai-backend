package com.aitaskmanager.service;

import com.aitaskmanager.model.AILog;
import com.aitaskmanager.model.User;
import com.aitaskmanager.repository.AILogRepository;
import com.aitaskmanager.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${aitask.gemini.api-key}")
    private String apiKey;

    @Value("${aitask.gemini.url}")
    private String apiUrl;

    @Autowired
    private AILogRepository aiLogRepository;

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> generateTaskDetails(String title) {

        String prompt = """
                Generate task details for this task title: "%s"

                Return ONLY valid JSON with no extra text, no markdown, no backticks.

                Format:
                {
                  "description": "string",
                  "priority": "HIGH or MEDIUM or LOW",
                  "estimatedTime": "string like 2 hours or 30 minutes"
                }
                """.formatted(title);

        try {
            System.out.println("Calling Groq API: " + apiUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Groq uses Bearer token authentication
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", List.of(
                    Map.of("role", "user", "content", prompt)
                ),
                "max_tokens", 300,
                "temperature", 0.7
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                apiUrl, entity, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {

                JsonNode root = objectMapper.readTree(response.getBody());

                // Groq uses OpenAI-style response: choices[0].message.content
                String aiText = root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

                // Clean any accidental markdown
                aiText = aiText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

                // Extract JSON object only
                int start = aiText.indexOf("{");
                int end = aiText.lastIndexOf("}");
                if (start != -1 && end != -1) {
                    aiText = aiText.substring(start, end + 1);
                }

                Map<String, Object> result = objectMapper.readValue(aiText, Map.class);
                logAI(title, aiText);
                return result;
            }

        } catch (Exception e) {
            System.err.println("Groq API Error: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback response
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("description", "AI service unavailable. Please fill manually.");
        fallback.put("priority", "MEDIUM");
        fallback.put("estimatedTime", "1 hour");
        return fallback;
    }

    public String listModels() {
        return "Using Groq API with model: llama3-8b-8192";
    }

    public Map<String, Object> chatWithAI(String message, List<Map<String, String>> history) {
        String systemPrompt = """
                You are TaskFlow AI Copilot, a premium, professional productivity and task management assistant.
                You help users plan, organize, and complete their workload.
                
                You can help users break down tasks or suggest tasks.
                If the user wants to add a task, you can help them draft it. Always format your suggestions nicely.
                If they explicitly request to create or add a task (e.g. "Add a high priority task: Complete tax filing by tomorrow" or "Create a task for learning React Native"), you should include a special JSON command at the very end of your response so the frontend can offer a one-click button to create it!
                
                Format the JSON command EXACTLY like this on its own line:
                [CMD_CREATE_TASK]:{"title": "Task title", "description": "AI-suggested description", "priority": "HIGH/MEDIUM/LOW", "estimatedTime": "duration"}
                
                Keep your main response friendly, professional, structured in markdown, and short.
                """;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            List<Map<String, String>> messages = new java.util.ArrayList<>();
            messages.add(Map.of("role", "system", "content", systemPrompt));

            if (history != null) {
                for (Map<String, String> entry : history) {
                    messages.add(Map.of(
                        "role", entry.get("role"),
                        "content", entry.get("content")
                    ));
                }
            }
            messages.add(Map.of("role", "user", "content", message));

            Map<String, Object> requestBody = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", messages,
                "max_tokens", 800,
                "temperature", 0.7
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                apiUrl, entity, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String aiText = root
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

                logAI(message, aiText);
                return Map.of("response", aiText);
            }
        } catch (Exception e) {
            System.err.println("Groq Chat Error: " + e.getMessage());
            e.printStackTrace();
        }

        return Map.of("response", "I'm sorry, I encountered an error while reaching my AI services. Please try again in a moment.");
    }

    private void logAI(String prompt, String response) {
        try {
            Object principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

            if (principal instanceof UserDetails userDetails) {
                User user = userRepository
                    .findByUsername(userDetails.getUsername())
                    .orElse(null);

                AILog log = AILog.builder()
                    .user(user)
                    .prompt(prompt)
                    .response(response)
                    .build();

                aiLogRepository.save(log);
            }
        } catch (Exception e) {
            System.out.println("AI Log Save Failed: " + e.getMessage());
        }
    }
}
