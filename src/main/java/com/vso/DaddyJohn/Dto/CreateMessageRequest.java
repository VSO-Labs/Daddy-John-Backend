package com.vso.DaddyJohn.Dto;

import lombok.Data;

@Data
public class CreateMessageRequest {
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
