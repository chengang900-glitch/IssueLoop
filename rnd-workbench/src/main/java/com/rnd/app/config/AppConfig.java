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
    private ExternalAuthConfig externalAuth = new ExternalAuthConfig();
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

    @Data
    public static class ExternalAuthConfig {
        private String publicBaseUrl = "http://127.0.0.1:3002";
        private ProviderConfig keycloak = new ProviderConfig();
        private ProviderConfig feishu = new ProviderConfig();
        private ProviderConfig dingtalk = new ProviderConfig();
        private ProviderConfig wecom = new ProviderConfig();
    }

    @Data
    public static class ProviderConfig {
        private boolean enabled = false;
        private String authorizationUri;
        private String tokenUri;
        private String appTokenUri;
        private String userInfoUri;
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String scope = "openid profile email";
        private boolean usePkce = true;
        private String tokenRequestMode = "form";
    }
}
