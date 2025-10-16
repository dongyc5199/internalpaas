package com.cmict.internalpaas.dto.hub;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Hub模块的MetricSample数据传输对象
 */
public class MetricSampleDto {

    private String name;
    private String type;
    private Double value;
    private String unit;
    private Long timestamp;

    @JsonProperty("labels")
    private Map<String, String> labels;

    // 可选字段（用于HISTOGRAM类型）
    private Long count;
    private Double sum;

    public MetricSampleDto() {
    }

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    public Double getSum() {
        return sum;
    }

    public void setSum(Double sum) {
        this.sum = sum;
    }

    @Override
    public String toString() {
        return "MetricSampleDto{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                ", timestamp=" + timestamp +
                ", labels=" + labels +
                '}';
    }
}
