package com.waveterm.demo.terminal.completion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "terminal.completions")
public class TerminalCompletionProperties {

    private Resource resource;
    private Resource variantsResource;
    private final List<String> extras = new ArrayList<>();
    private java.time.Duration cacheTtl = java.time.Duration.ofSeconds(30);

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Resource getVariantsResource() {
        return variantsResource;
    }

    public void setVariantsResource(Resource variantsResource) {
        this.variantsResource = variantsResource;
    }

    public List<String> getExtras() {
        return extras;
    }

    public java.time.Duration getCacheTtl() {
        return cacheTtl;
    }

    public void setCacheTtl(java.time.Duration cacheTtl) {
        if (cacheTtl != null && !cacheTtl.isNegative()) {
            this.cacheTtl = cacheTtl;
        }
    }
}
