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
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@ConfigurationProperties(prefix = "actions")
@Configuration
@Slf4j
public class ActionsProperties {

    private Actions toOidc;
    private Actions toPerun;
    private final Set<String> protectedClientIds = new HashSet<>();

    @PostConstruct
    public void postInit() {
        log.info("Initialized ACTIONS properties");
        log.debug("{}", this);
    }

    public void clientIds(Set<String> protectedClientIds) {
        if (protectedClientIds != null) {
            this.protectedClientIds.addAll(protectedClientIds);
        }
    }

    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Actions {
        private boolean create = true;
        private boolean update = true;
        private boolean delete = false;
    }

}
