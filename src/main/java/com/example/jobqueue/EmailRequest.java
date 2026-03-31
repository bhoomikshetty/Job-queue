package com.example.jobqueue;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class EmailRequest {

    @JsonProperty("from")
    @NotBlank @Email
    String from;

    @JsonProperty("to")
    @NotBlank @Email
    String to;

    @JsonProperty("attachment_url")
    String attachmentUrl;

    @JsonProperty("email_title")
    String title;

    @JsonProperty("email_subject")
    String subject;
}
