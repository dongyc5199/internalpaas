package com.cmict.internalpaas.dto;

/**
 * 服务器进程终止操作返回结果
 */
public class ServerProcessKillResponse {

    private boolean success;
    private String message;
    private String pid;

    public ServerProcessKillResponse() {
    }

    public ServerProcessKillResponse(boolean success, String message, String pid) {
        this.success = success;
        this.message = message;
        this.pid = pid;
    }

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

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }
}
