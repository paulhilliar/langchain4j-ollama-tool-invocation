package com.paulhilliar;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;

@Slf4j
public class ChatbotApp {

    // Define an interface for the AI Agent
    // Langchain4j will generate an implementation based on the model and tools
    interface Assistant {
        @SystemMessage("""
            You are a helpful operations assistant.
            Answer the user's questions politely and use the available tools when necessary.
            If the user asks for the time in a country, use the 'TimeTool'.
            If the user asks for the temperature in a country, use the 'TemperatureTool'.
            
            If the user asks for the time without specifying a country, ask the user which country they want the time for.
            
            Provide clear and concise answers.
            If a tool returns an error, inform the user politely.
            """)
        String chat(String userMessage);
    }

    public static void main(String[] args) {
        // --- 1. Configure the LLM (Ollama) ---
        ChatLanguageModel model = OllamaChatModel.builder()
            .baseUrl("http://localhost:11434/")         // Default Ollama URL
            .modelName("mistral-small:latest")          // The model you pulled
            .temperature(0.0)                           // Controls creativity (0.0 to 1.0).  1 is most creative.
            .build();

        // --- 2. Create the Tool instance ---
        TimeTool timeTool = new TimeTool();
        TemperatureTool temperatureTool = new TemperatureTool();

        // --- 3. Build the AI Agent ---
        Assistant assistant = AiServices.builder(Assistant.class)
            .chatLanguageModel(model)
            .chatMemory(MessageWindowChatMemory.withMaxMessages(10)) // Simple chat memory
            .tools(timeTool, temperatureTool)
            .build();

        // --- 4. Start the Chat Loop ---
        Scanner scanner = new Scanner(System.in);

        System.out.println("Chatbot started. Type 'exit' to quit.");
        System.out.println("Ask me about the time/weather in a country (e.g., 'What time is it in the UK?', 'Current time in France?', 'What is the time and weather in England?').");

        while (true) {
            System.out.print("\nUser: ");
            String userMessage = scanner.nextLine();

            if ("exit".equalsIgnoreCase(userMessage)) {
                break;
            }

            // Get response from the agent
            String agentResponse = assistant.chat(userMessage);

            // Print the agent's response
            System.out.println("Bot: " + agentResponse);
        }

        scanner.close();
        System.out.println("Chatbot stopped.");
    }
}
