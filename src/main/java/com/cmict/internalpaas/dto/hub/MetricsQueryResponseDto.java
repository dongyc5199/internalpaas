package com.cmict.internalpaas.dto.hub;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Hub模块的监控数据查询响应DTO
 */
public class MetricsQueryResponseDto {

    @JsonProperty("serverId")
    private Long serverId;

    @JsonProperty("from")
    private Long from;

    @JsonProperty("to")
    private Long to;

    @JsonProperty("step")
    private Integer step;

    @JsonProperty("fields")
    private String fields;

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("data")
    private List<MetricSampleDto> data;

    public MetricsQueryResponseDto() {
    }

    // Getters and Setters

    public Long getServerId() {
        return serverId;
    }

    public void setServerId(Long serverId) {
        this.serverId = serverId;
    }

    public Long getFrom() {
        return from;
    }

    public void setFrom(Long from) {
        this.from = from;
    }

    public Long getTo() {
        return to;
    }

    public void setTo(Long to) {
        this.to = to;
    }

    public Integer getStep() {
        return step;
    }

    public void setStep(Integer step) {
        this.step = step;
    }

    public String getFields() {
        return fields;
    }

    public void setFields(String fields) {
        this.fields = fields;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<MetricSampleDto> getData() {
        return data;
    }

    public void setData(List<MetricSampleDto> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "MetricsQueryResponseDto{" +
                "serverId=" + serverId +
                ", from=" + from +
                ", to=" + to +
                ", count=" + count +
                '}';
    }
}
