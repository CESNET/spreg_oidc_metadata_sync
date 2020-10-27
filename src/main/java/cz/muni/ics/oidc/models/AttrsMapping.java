package cz.muni.ics.oidc.models;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("attributes")
@Configuration
public class AttrsMapping {

    private String clientId;
    private String clientSecret;
    private String name;
    private String description;
    private String redirectUris;
    private String privacyPolicy;
    private String contacts;
    private String scopes;
    private String grantTypes;
    private String responseTypes;
    private String introspection;
    private String postLogoutRedirectUris;
    private String issueRefreshTokens;

    public List<String> getNames() {
        return Arrays.asList(clientId, clientSecret, name, description, redirectUris, privacyPolicy, contacts, scopes,
                grantTypes, responseTypes, introspection, postLogoutRedirectUris, issueRefreshTokens);
    }

}
