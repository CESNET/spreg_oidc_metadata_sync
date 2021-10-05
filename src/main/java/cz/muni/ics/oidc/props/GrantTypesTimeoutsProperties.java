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

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@ConfigurationProperties(prefix = "tokens", ignoreInvalidFields = true)
@Configuration
@Slf4j
public class GrantTypesTimeoutsProperties {

    @NotNull private GrantType authorizationCode = new GrantType(3600, 3600, 7200, 0);
    @NotNull private GrantType implicit = new GrantType(14400, 14400, 0, 0);
    @NotNull private GrantType hybrid = new GrantType(14400, 14400, 28800, 0);
    @NotNull private GrantType device = new GrantType(3600, 3600, 7200, 600);

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrantType {
        private Integer accessToken;
        private Integer idToken;
        private Integer refreshToken;
        private Integer deviceCode;
    }

}
