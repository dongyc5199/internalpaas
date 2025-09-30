package com.cmict.internalpaas.dto;

import java.util.List;

/**
 * Batch Action Request DTO
 * Used for batch operations on servers
 */
public class BatchActionRequest {

    private String action; // refresh, restart, stop
    private List<Long> serverIds;

    public BatchActionRequest() {
    }

    public BatchActionRequest(String action, List<Long> serverIds) {
        this.action = action;
        this.serverIds = serverIds;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public List<Long> getServerIds() {
        return serverIds;
    }

    public void setServerIds(List<Long> serverIds) {
        this.serverIds = serverIds;
    }
}