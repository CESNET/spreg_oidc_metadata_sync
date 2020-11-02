package cz.muni.ics.oidc.props;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;

@Component
@ConfigurationProperties(prefix = "rpc", ignoreUnknownFields = true)
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Slf4j
public class ConnectorProperties {

    @NonNull private String perunUrl = "https://perun-dev.cesnet.cz/ba/rpc";
    @NonNull private String perunUser;
    @NonNull private String perunPassword;
    @NonNull private String serializer = "json";
    private boolean enabled = true;
    private int requestTimeout = 30000;
    private int connectTimeout = 30000;
    private int socketTimeout = 60000;
    private int maxConnections = 20;
    private int maxConnectionsPerRoute = 18;

    public void setPerunUrl(@NonNull String perunUrl) {
        if (perunUrl.endsWith("/")) {
            perunUrl = perunUrl.substring(0, perunUrl.length() - 1);
        }

        this.perunUrl = perunUrl;
    }

    public void setSerializer(@NonNull String serializer) {
        if (!StringUtils.hasText(serializer)) {
            throw new IllegalArgumentException("Serializer cannot be null nor empty");
        }
        serializer = serializer.replaceAll("/", "");

        this.serializer = serializer;
    }

    @PostConstruct
    public void postInit() {
        log.info("Initialized RPC Connector properties");
        log.debug("{}", this);
    }

    @Override
    public String toString() {
        return "ConnectorProperties{" +
                "perunUrl='" + perunUrl + '\'' +
                ", perunUser='" + perunUser + '\'' +
                ", perunPassword='**************'" +
                ", serializer='" + serializer + '\'' +
                ", enabled=" + enabled +
                ", requestTimeout=" + requestTimeout +
                ", connectTimeout=" + connectTimeout +
                ", socketTimeout=" + socketTimeout +
                ", maxConnections=" + maxConnections +
                ", maxConnectionsPerRoute=" + maxConnectionsPerRoute +
                '}';
    }

}
