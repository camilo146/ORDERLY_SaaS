package com.orderly.api.email.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class EmailTemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateRenderer.class);

    public String render(String templateName, Map<String, String> vars) {
        try {
            ClassPathResource resource = new ClassPathResource("email-templates/" + templateName + ".html");
            String html = resource.getContentAsString(StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                html = html.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
            }
            return html;
        } catch (IOException e) {
            log.error("Email template '{}' not found: {}", templateName, e.getMessage());
            return "<p>Error rendering email template.</p>";
        }
    }
}
