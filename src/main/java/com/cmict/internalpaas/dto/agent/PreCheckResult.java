package com.cmict.internalpaas.dto.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent部署预检查结果
 */
public class PreCheckResult {
    private boolean passed;
    private boolean sshConnectable;
    private boolean hasSudoPermission;
    private boolean hasEnoughDiskSpace;
    private boolean portsAvailable;

    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public PreCheckResult() {
    }

    /**
     * 添加错误信息
     */
    public void addError(String error) {
        this.errors.add(error);
        this.passed = false;
    }

    /**
     * 添加警告信息
     */
    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    /**
     * 检查是否通过
     */
    public boolean isPassed() {
        return passed && sshConnectable && hasSudoPermission
                && hasEnoughDiskSpace && portsAvailable;
    }

    /**
     * 获取错误摘要
     */
    public String getErrorSummary() {
        if (errors.isEmpty()) {
            return null;
        }
        return String.join("; ", errors);
    }

    // Getters and Setters

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public boolean isSshConnectable() {
        return sshConnectable;
    }

    public void setSshConnectable(boolean sshConnectable) {
        this.sshConnectable = sshConnectable;
    }

    public boolean isHasSudoPermission() {
        return hasSudoPermission;
    }

    public void setHasSudoPermission(boolean hasSudoPermission) {
        this.hasSudoPermission = hasSudoPermission;
    }

    public boolean isHasEnoughDiskSpace() {
        return hasEnoughDiskSpace;
    }

    public void setHasEnoughDiskSpace(boolean hasEnoughDiskSpace) {
        this.hasEnoughDiskSpace = hasEnoughDiskSpace;
    }

    public boolean isPortsAvailable() {
        return portsAvailable;
    }

    public void setPortsAvailable(boolean portsAvailable) {
        this.portsAvailable = portsAvailable;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    @Override
    public String toString() {
        return "PreCheckResult{" +
                "passed=" + isPassed() +
                ", sshConnectable=" + sshConnectable +
                ", hasSudoPermission=" + hasSudoPermission +
                ", hasEnoughDiskSpace=" + hasEnoughDiskSpace +
                ", portsAvailable=" + portsAvailable +
                ", errors=" + errors +
                ", warnings=" + warnings +
                '}';
    }
}
