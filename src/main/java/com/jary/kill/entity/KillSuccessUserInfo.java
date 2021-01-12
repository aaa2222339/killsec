package com.jary.kill.entity;

// 订单的详细信息
public class KillSuccessUserInfo extends ItemKillSuccess{
    private String userName;
    private String phone;
    private String email;
    private String itemName;

    public String toString() {
        return super.toString()+"\nKillSuccessUserInfo{" +
                "userName='" + userName + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", itemName='" + itemName + '\'' +
                '}';
    }
}