package cz.muni.ics.oidc.props;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@ConfigurationProperties(prefix = "actions")
@Configuration
public class ActionsProperties {

    private boolean create = true;
    private boolean update = true;
    private boolean delete = false;
    private final Set<String> protectedClientIds = new HashSet<>();

    public void clientIds(Set<String> protectedClientIds    ) {
        if (protectedClientIds != null) {
            this.protectedClientIds.addAll(protectedClientIds);
        }
    }

}
