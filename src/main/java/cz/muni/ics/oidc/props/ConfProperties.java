package cz.muni.ics.oidc.props;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;

import javax.annotation.PostConstruct;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@ConfigurationProperties(prefix = "conf")
@Configuration
@Slf4j
@Validated
public class ConfProperties {

    @NotEmpty private Set<String> langs = new HashSet<>();
    @NotBlank private String encryptionSecret;
    @NotBlank private String proxyIdentifierValue;
    @Nullable private Long accessTokenTimeout;
    @Nullable private Long idTokenTimeout;
    @Nullable private Long refreshTokenTimeout;
    @Min(1) private Long managersGroupVoId;
    @Min(1) private long managersGroupParentGroupId;
    @NotBlank private String managersGroupParentGroupName;
    @Nullable private String probeOutputFileLocation;

    @PostConstruct
    public void init() {
        log.info("Initialized CONF properties");
        log.debug("{}", this);
    }

    public void setLangs(Set<String> langs) {
        this.langs.add("en");
        if (langs != null) {
            this.langs.addAll(langs.stream().map(String::toLowerCase).collect(Collectors.toList()));
        }
    }

    @Override
    public String toString() {
        return "ConfProperties{" +
                "langs=" + langs +
                ", encryptionSecret=[PROTECTED]" +
                ", proxyIdentifierValue='" + proxyIdentifierValue + '\'' +
                ", accessTokenTimeout=" + accessTokenTimeout +
                ", idTokenTimeout=" + idTokenTimeout +
                ", refreshTokenTimeout=" + refreshTokenTimeout +
                ", managersGroupVoId=" + managersGroupVoId +
                ", managersGroupParentGroupId=" + managersGroupParentGroupId +
                ", managersGroupParentGroupName='" + managersGroupParentGroupName + '\'' +
                ", probeOutputFileLocation='" + probeOutputFileLocation + '\'' +
                '}';
    }
}
