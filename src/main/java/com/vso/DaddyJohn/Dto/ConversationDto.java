package com.vso.DaddyJohn.Dto;

import lombok.Data;

import java.time.LocalDateTime;

// Used to send conversation data to the client
@Data
public class ConversationDto {
    private String id;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
