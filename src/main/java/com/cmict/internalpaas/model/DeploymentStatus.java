package com.cmict.internalpaas.model;

/**
 * Agent部署状态枚举
 */
public enum DeploymentStatus {
    PENDING("待部署"),
    PRE_CHECK("预检中"),
    UPLOADING("上传中"),
    INSTALLING("安装中"),
    HEALTH_CHECK("健康检查中"),
    SUCCESS("部署成功"),
    FAILED("部署失败"),
    RETRYING("重试中"),
    ROLLED_BACK("已回滚");

    private final String description;

    DeploymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 是否为最终状态（不再变化）
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == ROLLED_BACK;
    }

    /**
     * 是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 是否为失败状态
     */
    public boolean isFailed() {
        return this == FAILED || this == ROLLED_BACK;
    }
}
