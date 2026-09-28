package com.rnd.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private JwtConfig jwt;
    private StorageConfig storage;
    private WecomConfig wecom;
    private PreviewConfig preview;

    @Data
    public static class PreviewConfig {
        private String libreofficePath;
    }

    @Data
    public static class JwtConfig {
        private String secret;
        private String ttl = "8h";
    }

    @Data
    public static class StorageConfig {
        private String path = "./data/attachments";
    }

    @Data
    public static class WecomConfig {
        private boolean enabled = false;
        private String webhook;
    }
}