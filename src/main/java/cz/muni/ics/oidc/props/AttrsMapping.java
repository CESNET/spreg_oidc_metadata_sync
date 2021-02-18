package cz.muni.ics.oidc.props;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
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
    private String contacts2;
    private String scopes;
    private String grantTypes;
    private String responseTypes;
    private String introspection;
    private String postLogoutRedirectUris;
    private String issueRefreshTokens;

    public List<String> getNames() {
        return Arrays.asList(clientId, clientSecret, name, description, redirectUris, privacyPolicy, contacts, contacts2, scopes,
                grantTypes, responseTypes, introspection, postLogoutRedirectUris, issueRefreshTokens);
    }

    @PostConstruct
    public void postInit() {
        log.info("Initialized Attribute names properties");
        log.debug("{}", this);
    }

}
