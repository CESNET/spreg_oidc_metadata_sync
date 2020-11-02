package cz.muni.ics.oidc.props;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
@ConfigurationProperties("jdbc")
@Configuration
public class JdbcProperties {

    private String driverClassName;
    private String url;
    private String username;
    private String password;

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
                ", password='**************'" +
                '}';
    }
}
