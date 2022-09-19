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
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
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
@Validated
public class AttrsMapping {

    @NotBlank private String clientId;
    @NotBlank private String clientSecret;
    @NotBlank private String name;
    @NotBlank private String description;
    @NotBlank private String redirectUris;
    @NotBlank private String privacyPolicy;
    @NotEmpty private List<String> contacts;
    @NotBlank private String scopes;
    @NotBlank private String grantTypes;
    @NotBlank private String codeChallengeType;
    @NotBlank private String introspection;
    @NotBlank private String postLogoutRedirectUris;
    @NotBlank private String issueRefreshTokens;

    @NotBlank private String tokenEndpointAuthenticationMethod;
    private List<String> homePageUris;
    private String tokenTimeouts;
    private String reuseRefreshTokens;

    // MitreID client non-related
    @NotBlank private String masterProxyIdentifier;
    @NotBlank private String proxyIdentifier;
    @NotBlank private String isTestSp;
    @NotBlank private String isOidc;
    @NotBlank private String managersGroupId;

    public List<String> getNames() {
        List<String> attrNames = new ArrayList<>(
                Arrays.asList(clientId,
                    clientSecret,
                    name,
                    description,
                    redirectUris,
                    privacyPolicy,
                    scopes,
                    grantTypes,
                    codeChallengeType,
                    introspection,
                    postLogoutRedirectUris,
                    issueRefreshTokens,
                    tokenEndpointAuthenticationMethod,
                    masterProxyIdentifier,
                    proxyIdentifier,
                    isTestSp,
                    isOidc,
                    managersGroupId
                )
        );
        attrNames.addAll(contacts);

        if (homePageUris != null) {
            attrNames.addAll(homePageUris);
        }

        if (StringUtils.hasText(tokenTimeouts)) {
            attrNames.add(tokenTimeouts);
        }

        if (StringUtils.hasText(reuseRefreshTokens)) {
            attrNames.add(reuseRefreshTokens);
        }

        return attrNames;
    }

    @PostConstruct
    public void postInit() {
        log.info("Initialized Attribute names properties");
        log.debug("{}", this);
    }

}
