package com.vso.DaddyJohn.Dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
public class CreateMessageRequest {
    @NotBlank(message = "Content cannot be blank")
    @Size(min = 1, max = 10000, message = "Message must be between 1 and 10000 characters")
    private String content;
}