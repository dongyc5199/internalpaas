package com.cmict.internalpaas.ai.service;

import com.cmict.internalpaas.ai.model.ModelConfig;
import com.cmict.internalpaas.ai.moonshot.MoonshotProperties;
import com.cmict.internalpaas.ai.ollama.OllamaProperties;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiModelRegistry {
    private final MoonshotProperties moonshotProperties;
    private final OllamaProperties ollamaProperties;

    // in-memory added models (for display)
    private final Map<String, ModelConfig> added = new ConcurrentHashMap<>();

    public AiModelRegistry(MoonshotProperties moonshotProperties, OllamaProperties ollamaProperties) {
        this.moonshotProperties = moonshotProperties;
        this.ollamaProperties = ollamaProperties;
    }

    public List<ModelConfig> listDetailed() {
        List<ModelConfig> list = new ArrayList<>();
        if (moonshotProperties != null && moonshotProperties.getApiKey() != null && !moonshotProperties.getApiKey().isBlank()) {
            String model = Optional.ofNullable(moonshotProperties.getModel()).orElse("kimi-k2-0905-preview");
            list.add(new ModelConfig("moonshot-default", "moonshot", "moonshot", model, mask(moonshotProperties.getApiKey())));
        }
        if (ollamaProperties != null && ollamaProperties.isEnabled()) {
            String model = Optional.ofNullable(ollamaProperties.getModel()).orElse("gpt-oss:20b");
            list.add(new ModelConfig("ollama-default", "ollama", "ollama", model, null));
        }
        list.addAll(added.values());
        if (list.stream().noneMatch(c -> "echo".equals(c.getClientId()))) {
            list.add(new ModelConfig("echo", "echo", "echo", "echo", null));
        }
        return list;
    }

    public ModelConfig add(String provider, String model, String apiKey) {
        String id = provider + ":" + model;
        String clientId = normalizeClient(provider);
        ModelConfig cfg = new ModelConfig(id, provider, clientId, model, mask(apiKey));
        // Apply to properties when applicable
        if ("moonshot".equals(clientId)) {
            if (apiKey != null && !apiKey.isBlank()) {
                moonshotProperties.setApiKey(apiKey);
            }
            if (model != null && !model.isBlank()) {
                moonshotProperties.setModel(model);
            }
        } else if ("ollama".equals(clientId)) {
            if (model != null && !model.isBlank()) {
                ollamaProperties.setModel(model);
            }
        }
        added.put(cfg.getId(), cfg);
        return cfg;
    }

    private static String mask(String key) {
        if (key == null || key.isBlank()) return null;
        int keep = Math.min(4, key.length());
        return "*".repeat(Math.max(0, key.length() - keep)) + key.substring(key.length() - keep);
    }

    private static String normalizeClient(String provider) {
        String p = Optional.ofNullable(provider).orElse("").toLowerCase();
        if (p.contains("moon") || p.contains("kimi")) return "moonshot";
        if (p.contains("ollama") || p.contains("local")) return "ollama";
        if (p.contains("echo")) return "echo";
        // treat unknown as echo fallback
        return "echo";
    }
}

