package com.orderly.api.email.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "orderly.email")
public class EmailProperties {

    private String fromAddress = "support@orderlymyapp.com";
    private String fromName = "Orderly";
    private String appBaseUrl = "http://localhost:4200";
    private boolean enabled = true;

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }

    public String getAppBaseUrl() { return appBaseUrl; }
    public void setAppBaseUrl(String appBaseUrl) { this.appBaseUrl = appBaseUrl; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
