package cz.muni.ics.oidc.props;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
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

    @NonNull private String clientId;
    @NonNull private String clientSecret;
    @NonNull private String name;
    @NonNull private String description;
    @NonNull private String redirectUris;
    @NonNull private String privacyPolicy;
    @NonNull private List<String> contacts;
    @NonNull private String scopes;
    @NonNull private String grantTypes;
    @NonNull private String responseTypes;
    @NonNull private String introspection;
    @NonNull private String postLogoutRedirectUris;
    @NonNull private String issueRefreshTokens;
    @NonNull private String masterProxyIdentifier;
    @NonNull private String proxyIdentifier;
    @NonNull private String isTestSp;
    @NonNull private String isOidc;
    @NonNull private String managersGroupId;

    public List<String> getNames() {
        List<String> attrNames = new ArrayList<>(
                Arrays.asList(clientId, clientSecret, name, description, redirectUris, privacyPolicy,
                        scopes, grantTypes, responseTypes, introspection, postLogoutRedirectUris, issueRefreshTokens,
                        masterProxyIdentifier, proxyIdentifier, isTestSp, isOidc, managersGroupId)
        );
        attrNames.addAll(contacts);
        return attrNames;
    }

    @PostConstruct
    public void postInit() {
        log.info("Initialized Attribute names properties");
        log.debug("{}", this);
    }

}
