package cz.muni.ics.oidc.props;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@ConfigurationProperties("jdbc")
@Configuration
@Validated
public class JdbcProperties {

    @NotBlank private String driverClassName;
    @NotBlank private String url;
    @NotBlank private String username;
    @NotBlank private String password;

    @PostConstruct
    public void postInit() {
        log.info("Initialized JDBC properties");
        log.debug("{}", this);
    }

    @Override
    public String toString() {
        return "JdbcProperties{" +
                "driverClassName='" + driverClassName + '\'' +
                ", url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password=[PROTECTED]" +
                '}';
    }
}
