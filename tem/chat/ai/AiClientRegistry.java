package com.waveterm.demo.chat.ai;

import com.waveterm.demo.chat.ChatProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class AiClientRegistry {

    private static final Logger log = LoggerFactory.getLogger(AiClientRegistry.class);

    private final Map<String, AiClient> clients;
    private final AiClient defaultClient;
    private final Random random = new Random();
    private final ChatProperties properties;
    private final List<WeightedClient> weightedClients;

    public AiClientRegistry(java.util.List<AiClient> clients,
                            ChatProperties properties) {
        this.clients = clients.stream().collect(Collectors.toMap(AiClient::id, client -> client));
        this.properties = properties;
        this.defaultClient = resolveDefaultClient();
        this.weightedClients = buildWeightedClients();
        log.info("AI client registry initialized with {} clients, default={}",
                this.clients.size(), this.defaultClient.id());
    }

    public Optional<AiClient> get(String id) {
        if (id == null || id.isBlank()) {
            return Optional.of(defaultClient);
        }
        return Optional.ofNullable(clients.get(id));
    }

    public AiClient pickWeightedClient() {
        if (weightedClients.isEmpty()) {
            return defaultClient;
        }
        int totalWeight = weightedClients.stream().mapToInt(WeightedClient::weight).sum();
        int choice = random.nextInt(totalWeight);
        int cumulative = 0;
        for (WeightedClient candidate : weightedClients) {
            cumulative += candidate.weight();
            if (choice < cumulative) {
                return candidate.client();
            }
        }
        return defaultClient;
    }

    public AiClient defaultClient() {
        return defaultClient;
    }

    private AiClient resolveDefaultClient() {
        String defaultId = properties.getDefaultClient();
        AiClient client = clients.get(defaultId);
        if (client != null) {
            return client;
        }
        return clients.values().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No AI clients registered"));
    }

    private List<WeightedClient> buildWeightedClients() {
        List<WeightedClient> list = new ArrayList<>();
        for (Map.Entry<String, ChatProperties.ClientConfig> entry : properties.getClients().entrySet()) {
            AiClient client = clients.get(entry.getKey());
            if (client == null) {
                continue;
            }
            int weight = entry.getValue().weight();
            list.add(new WeightedClient(client, weight, entry.getValue().timeout()));
        }
        if (list.isEmpty() && !clients.isEmpty()) {
            for (AiClient client : clients.values()) {
                list.add(new WeightedClient(client, 1, client.timeoutHint()));
            }
        }
        return list;
    }

    public Duration timeoutFor(AiClient client) {
        return weightedClients.stream()
                .filter(c -> c.client().id().equals(client.id()))
                .map(WeightedClient::timeout)
                .findFirst()
                .orElse(client.timeoutHint());
    }

    private record WeightedClient(AiClient client, int weight, Duration timeout) {
    }
}
