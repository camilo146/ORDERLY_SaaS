package com.orderly.api.email.infrastructure;

import com.orderly.api.email.domain.port.EmailService;
import com.orderly.api.email.infrastructure.config.EmailProperties;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailAdapter implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailAdapter.class);

    private final JavaMailSender mailSender;
    private final EmailProperties props;

    public SmtpEmailAdapter(JavaMailSender mailSender, EmailProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Async("asyncExecutor")
    @Override
    public void send(String to, String subject, String htmlBody) {
        if (!props.isEnabled()) {
            log.info("Email disabled — skipping send to {} subject '{}'", to, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(props.getFromAddress(), props.getFromName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {} subject '{}'", to, subject);
        } catch (Exception e) {
            log.warn("Failed to send email to {} subject '{}': {}", to, subject, e.getMessage());
        }
    }
}
