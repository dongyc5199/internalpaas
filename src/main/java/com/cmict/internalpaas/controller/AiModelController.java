package com.cmict.internalpaas.controller;

import com.cmict.internalpaas.ai.moonshot.MoonshotProperties;
import com.cmict.internalpaas.ai.ollama.OllamaProperties;
import com.cmict.internalpaas.ai.client.AiClientRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/ai")
public class AiModelController {

    private final MoonshotProperties moonshotProperties;
    private final OllamaProperties ollamaProperties;
    private final AiClientRegistry clientRegistry;

    public AiModelController(MoonshotProperties moonshotProperties,
                             OllamaProperties ollamaProperties,
                             AiClientRegistry clientRegistry) {
        this.moonshotProperties = moonshotProperties;
        this.ollamaProperties = ollamaProperties;
        this.clientRegistry = clientRegistry;
    }

    @GetMapping("/models")
    public ResponseEntity<List<String>> models() {
        // Use LinkedHashSet to preserve insertion order and deduplicate
        Set<String> set = new LinkedHashSet<>();

        // Prefer Moonshot model if API key is present
        if (moonshotProperties != null && moonshotProperties.getApiKey() != null && !moonshotProperties.getApiKey().isBlank()) {
            String m = moonshotProperties.getModel();
            if (m != null && !m.isBlank()) set.add(m);
        }

        // Ollama if enabled
        if (ollamaProperties != null && ollamaProperties.isEnabled()) {
            String o = ollamaProperties.getModel();
            if (o != null && !o.isBlank()) set.add(o);
        }

        // Also include registered AI client ids (these are client identifiers, not necessarily model ids)
        try {
            clientRegistry.defaultClient(); // ensure registry initialized
            clientRegistry.pickWeightedClient();
            clientRegistry.get(clientRegistry.defaultClient().id()).ifPresent(c -> set.add(c.id()));
        } catch (Exception ignored) {
            // ignore any registry issues
        }

        // Always include a safe fallback
        set.add("echo");

        return ResponseEntity.ok(new ArrayList<>(set));
    }
}
