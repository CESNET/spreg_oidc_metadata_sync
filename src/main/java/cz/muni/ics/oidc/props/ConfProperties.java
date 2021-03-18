package cz.muni.ics.oidc.props;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@ConfigurationProperties(prefix = "conf")
@Configuration
@Slf4j
public class ConfProperties {

    private Set<String> langs;
    private String encryptionSecret;
    private String proxyIdentifierValue;
    private Long managersGroupVoId;
    private Long managersGroupParentGroupId;
    private String managersGroupParentGroupName;

    @PostConstruct
    public void init() {
        log.info("Initialized CONF properties");
        log.debug("{}", this.toString());
    }

    @Override
    public String toString() {
        return "ConfProperties{" +
                "langs=" + langs +
                ", encryptionSecret='**************'" +
                ", proxyIdentifierValue='" + proxyIdentifierValue + '\'' +
                ", managersGroupVoId=" + managersGroupVoId +
                ", managersGroupParentGroupId=" + managersGroupParentGroupId +
                ", managersGroupParentGroupName='" + managersGroupParentGroupName + '\'' +
                '}';
    }
}
