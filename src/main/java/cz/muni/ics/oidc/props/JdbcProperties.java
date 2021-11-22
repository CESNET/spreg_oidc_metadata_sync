package cz.muni.ics.oidc.props;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.persistence.platform.database.MySQLPlatform;
import org.eclipse.persistence.platform.database.PostgreSQLPlatform;
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

    public static final String PLATFORM_MYSQL = MySQLPlatform.class.getName();
    public static final String PLATFORM_PSQL = PostgreSQLPlatform.class.getName();

    @NotBlank private String driverClassName;
    @NotBlank private String platform = PLATFORM_MYSQL;
    @NotBlank private String url;
    @NotBlank private String username;
    @NotBlank private String password;

    @PostConstruct
    public void postInit() {
        if (!platform.equals(PLATFORM_MYSQL) && !platform.equals(PLATFORM_PSQL)) {
            throw new IllegalArgumentException("Unrecognized JDBC platform '" + platform + "'!");
        }

        log.info("Initialized JDBC properties");
        log.debug("{}", this);
    }

    @Override
    public String toString() {
        return "JdbcProperties{" +
                "driverClassName='" + driverClassName + '\'' +
                ", platform='" + platform + '\'' +
                ", url='" + url + '\'' +
                ", username='" + username + '\'' +
                ", password=[PROTECTED]" +
                '}';
    }
}
