package cz.muni.ics.oidc.data;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@ConfigurationProperties("jdbc")
@Configuration
public class JdbcProperties {

    private String driverClassName;
    private String url;
    private String username;
    private String password;

}
