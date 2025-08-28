package com.vso.DaddyJohn.Dto;

import com.vso.DaddyJohn.Entity.Message;
import lombok.Data;

import java.time.LocalDateTime;

// Used to send message data to the client
@Data
public class MessageDto {
    private String id;
    private Message.Role role;
    private String content;
    private Integer tokenCount;
    private LocalDateTime createdAt;
}

