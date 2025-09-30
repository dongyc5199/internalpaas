package com.cmict.internalpaas.dto;

import java.util.List;

/**
 * Application Distribution DTO
 * Contains application deployment distribution across servers
 */
public class AppDistributionDto {

    private List<String> servers;
    private List<AppTypeData> appTypes;

    public AppDistributionDto() {
    }

    public AppDistributionDto(List<String> servers, List<AppTypeData> appTypes) {
        this.servers = servers;
        this.appTypes = appTypes;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers = servers;
    }

    public List<AppTypeData> getAppTypes() {
        return appTypes;
    }

    public void setAppTypes(List<AppTypeData> appTypes) {
        this.appTypes = appTypes;
    }

    /**
     * Application type data
     */
    public static class AppTypeData {
        private String type; // web, api, database, cache, file
        private String label;
        private List<Integer> data; // count for each server
        private String color;

        public AppTypeData() {
        }

        public AppTypeData(String type, String label, List<Integer> data, String color) {
            this.type = type;
            this.label = label;
            this.data = data;
            this.color = color;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public List<Integer> getData() {
            return data;
        }

        public void setData(List<Integer> data) {
            this.data = data;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }
}
