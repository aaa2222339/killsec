package com.jary.kill.entity;

public class KillDto{
    private Integer killId;
    private Integer userId;

    @Override
    public String toString() {
        return "KillDto{" +
                "killId=" + killId +
                ", userId=" + userId +
                '}';
    }

    public Integer getKillId() {
        return killId;
    }

    public void setKillId(Integer killId) {
        this.killId = killId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
}