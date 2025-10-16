package com.cmict.internalpaas.dto.agent;

/**
 * Agent部署结果DTO
 */
public class DeployResult {
    private boolean success;
    private String message;
    private String errorMessage;
    private Long deploymentId;

    public DeployResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static DeployResult success(String message) {
        return new DeployResult(true, message);
    }

    public static DeployResult failure(String errorMessage) {
        DeployResult result = new DeployResult(false, "部署失败");
        result.setErrorMessage(errorMessage);
        return result;
    }

    // Getters and Setters

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(Long deploymentId) {
        this.deploymentId = deploymentId;
    }

    @Override
    public String toString() {
        return "DeployResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", deploymentId=" + deploymentId +
                '}';
    }
}
