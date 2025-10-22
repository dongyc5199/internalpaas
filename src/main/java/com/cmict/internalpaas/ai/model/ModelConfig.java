package com.cmict.internalpaas.ai.model;

public class ModelConfig {
    private String id;
    private String provider; // moonshot | ollama | echo
    private String clientId; // normalized client id
    private String model;    // model id or label
    private String apiKeyMasked; // optional masked key for display

    public ModelConfig() {}

    public ModelConfig(String id, String provider, String clientId, String model, String apiKeyMasked) {
        this.id = id;
        this.provider = provider;
        this.clientId = clientId;
        this.model = model;
        this.apiKeyMasked = apiKeyMasked;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKeyMasked() { return apiKeyMasked; }
    public void setApiKeyMasked(String apiKeyMasked) { this.apiKeyMasked = apiKeyMasked; }
}

