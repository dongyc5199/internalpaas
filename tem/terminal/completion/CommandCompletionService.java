package com.waveterm.demo.terminal.completion;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class CommandCompletionService {

    private static final int MAX_SUGGESTIONS = 20;

    private final TerminalCompletionProperties properties;
    private final Map<String, Double> catalog = new TreeMap<>();
    private final Map<String, CachedCompletion> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private long cacheTtlMillis = 30000;

    public CommandCompletionService(TerminalCompletionProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void loadResources() throws IOException {
        catalog.clear();
        loadResourceLines(properties.getResource(), 1.0);
        loadResourceLines(properties.getVariantsResource(), 0.8);
        if (properties.getExtras() != null) {
            for (String extra : properties.getExtras()) {
                if (extra != null && !extra.isBlank()) {
                    catalog.put(extra.trim(), 0.9);
                }
            }
        }
        cacheTtlMillis = properties.getCacheTtl() != null ? properties.getCacheTtl().toMillis() : 30000;
    }

    private void loadResourceLines(org.springframework.core.io.Resource resource, double baseScore) throws IOException {
        if (resource == null || !resource.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                catalog.putIfAbsent(trimmed, baseScore);
            }
        }
    }

    public List<CompletionSuggestion> complete(String prompt, List<String> recentCommands) {
        if (prompt == null) {
            prompt = "";
        }
        String normalized = prompt.trim().toLowerCase(Locale.ROOT);
        List<String> normalizedHistory = recentCommands == null ? List.of() : recentCommands.stream()
                .filter(cmd -> cmd != null && !cmd.isBlank())
                .map(cmd -> cmd.trim())
                .toList();
        String cacheKey = buildCacheKey(normalized, normalizedHistory);
        CachedCompletion cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheTtlMillis)) {
            return cached.response().suggestions();
        }
        if (normalized.isEmpty()) {
            List<CompletionSuggestion> top = catalog.entrySet().stream()
                    .limit(MAX_SUGGESTIONS)
                    .map(e -> new CompletionSuggestion(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
            cache.put(cacheKey, new CachedCompletion(new CompletionResponse(top), System.currentTimeMillis()));
            return top;
        }
        List<CompletionSuggestion> suggestions = new ArrayList<>();
        for (Map.Entry<String, Double> entry : catalog.entrySet()) {
            if (entry.getKey().toLowerCase(Locale.ROOT).startsWith(normalized)) {
                suggestions.add(new CompletionSuggestion(entry.getKey(), entry.getValue()));
                if (suggestions.size() >= MAX_SUGGESTIONS) {
                    break;
                }
            }
        }
        if (suggestions.isEmpty()) {
            suggestions = catalog.entrySet().stream()
                    .filter(e -> e.getKey().toLowerCase(Locale.ROOT).contains(normalized))
                    .sorted(Comparator.comparingDouble((Map.Entry<String, Double> e) -> -e.getValue()))
                    .limit(MAX_SUGGESTIONS)
                    .map(e -> new CompletionSuggestion(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        }
        if (!normalizedHistory.isEmpty()) {
            suggestions.addAll(normalizedHistory.stream()
                    .filter(cmd -> cmd.toLowerCase(Locale.ROOT).startsWith(normalized))
                    .map(cmd -> new CompletionSuggestion(cmd, 0.95))
                    .limit(MAX_SUGGESTIONS)
                    .toList());
        }
        List<CompletionSuggestion> finalResult = suggestions.stream()
                .distinct()
                .limit(MAX_SUGGESTIONS)
                .collect(Collectors.toList());
        cache.put(cacheKey, new CachedCompletion(new CompletionResponse(finalResult), System.currentTimeMillis()));
        return finalResult;
    }

    private String buildCacheKey(String prompt, List<String> history) {
        if (history.isEmpty()) {
            return prompt;
        }
        return prompt + "::" + String.join("|", history);
    }
}
