package cz.muni.ics.oidc.props;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Component
@ConfigurationProperties(prefix = "rpc", ignoreInvalidFields = true)
@Getter
@Setter
@EqualsAndHashCode
@Slf4j
@Validated
public class ConnectorProperties {

    @NotBlank private String perunUrl = "https://perun-dev.cesnet.cz/ba/rpc";
    @NotBlank private String perunUser;
    @NotBlank private String perunPassword;
    @NotBlank private String serializer = "json";
    @Min(1) private int requestTimeout = 30000;
    @Min(1) private int connectTimeout = 30000;
    @Min(1) private int socketTimeout = 60000;
    @Min(1) private int maxConnections = 20;
    @Min(1) private int maxConnectionsPerRoute = 18;

    public void setPerunUrl(String perunUrl) {
        if (!StringUtils.hasText(perunUrl)) {
            throw new IllegalArgumentException("PerunURL cannot be blank");
        }
        if (perunUrl.endsWith("/")) {
            perunUrl = perunUrl.substring(0, perunUrl.length() - 1);
        }

        this.perunUrl = perunUrl;
    }

    public void setSerializer(@NonNull String serializer) {
        if (!StringUtils.hasText(serializer)) {
            serializer = "json";
        } else {
            serializer = serializer.replaceAll("/", "");
        }
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
                ", perunPassword=[PROTECTED]" +
                ", serializer='" + serializer + '\'' +
                ", requestTimeout=" + requestTimeout +
                ", connectTimeout=" + connectTimeout +
                ", socketTimeout=" + socketTimeout +
                ", maxConnections=" + maxConnections +
                ", maxConnectionsPerRoute=" + maxConnectionsPerRoute +
                '}';
    }

}
