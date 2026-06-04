package com.orderly.api.email.domain.port;

public interface EmailService {
    void send(String to, String subject, String htmlBody);
}
