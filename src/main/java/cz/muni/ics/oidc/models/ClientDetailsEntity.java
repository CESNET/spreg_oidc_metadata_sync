package cz.muni.ics.oidc.models;


import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWT;
import cz.muni.ics.oidc.models.converters.JWEAlgorithmStringConverter;
import cz.muni.ics.oidc.models.converters.JWEEncryptionMethodStringConverter;
import cz.muni.ics.oidc.models.converters.JWKSetStringConverter;
import cz.muni.ics.oidc.models.converters.JWSAlgorithmStringConverter;
import cz.muni.ics.oidc.models.converters.JWTStringConverter;
import cz.muni.ics.oidc.models.converters.PKCEAlgorithmStringConverter;
import cz.muni.ics.oidc.models.converters.SimpleGrantedAuthorityStringConverter;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "client_details")
@NamedQueries({
        @NamedQuery(name = ClientDetailsEntity.QUERY_ALL,
                query = "SELECT c FROM ClientDetailsEntity c"),
        @NamedQuery(name = ClientDetailsEntity.QUERY_BY_CLIENT_ID,
                query = "SELECT c FROM ClientDetailsEntity c " +
                        "WHERE c.clientId = :" + ClientDetailsEntity.PARAM_CLIENT_ID)
})
public class ClientDetailsEntity implements ClientDetails {

    public static final String QUERY_BY_CLIENT_ID = "ClientDetailsEntity.getByClientId";
    public static final String QUERY_ALL = "ClientDetailsEntity.findAll";

    public static final String PARAM_CLIENT_ID = "clientId";

    private static final int DEFAULT_ID_TOKEN_VALIDITY_SECONDS = 600;
    private static final long serialVersionUID = -1617727085733786296L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name="client_id")
    private String clientId = null;

    @Column(name="client_secret")
    private String clientSecret = null;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="client_redirect_uri", joinColumns=@JoinColumn(name="owner_id"))
    @Column(name="redirect_uri")
    private Set<String> redirectUris = new HashSet<>();

    @Column(name="client_name")
    private String clientName;

    @Column(name="client_uri")
    private String clientUri;

    @Column(name="logo_uri")
    private final String logoUri = null;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="client_contact", joinColumns=@JoinColumn(name="owner_id"))
    @Column(name="contact")
    private Set<String> contacts = new HashSet<>();

    @Column(name="tos_uri")
    private String tosUri = null;

    @Enumerated(EnumType.STRING)
    @Column(name="token_endpoint_auth_method")
    private AuthMethod tokenEndpointAuthMethod = AuthMethod.SECRET_BASIC;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="client_scope", joinColumns=@JoinColumn(name="owner_id"))
    @Column(name="scope")
    private Set<String> scope = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="client_grant_type", joinColumns=@JoinColumn(name="owner_id"))
    @Column(name="grant_type")
    private Set<String> grantTypes = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="client_response_type", joinColumns=@JoinColumn(name="owner_id"))
    @Column(name="response_type")
    private Set<String> responseTypes = new HashSet<>();

    @Column(name="policy_uri")
    private String policyUri;

    @Column(name="jwks_uri")
    private String jwksUri = null;

    @Column(name="jwks")
    @Convert(converter = JWKSetStringConverter.class)
    private JWKSet jwks = null;

    @Column(name="software_id")
    private String softwareId = null;

    @Column(name="software_version")
    private String softwareVersion = null;

    @Enumerated(EnumType.STRING)
    @Column(name="application_type")
    private AppType applicationType = null;

    @Column(name="sector_identifier_uri")
    private String sectorIdentifierUri = null;

    @Enumerated(EnumType.STRING)
    @Column(name="subject_type")
    private SubjectType subjectType = SubjectType.PUBLIC; // subject_type

    @Column(name = "request_object_signing_alg")
    @Convert(converter = JWSAlgorithmStringConverter.class)
    private JWSAlgorithm requestObjectSigningAlg = null; // request_object_signing_alg

    @Column(name = "user_info_signed_response_alg")
    @Convert(converter = JWSAlgorithmStringConverter.class)
    private JWSAlgorithm userInfoSignedResponseAlg = null; // user_info_signed_response_alg

    @Column(name = "user_info_encrypted_response_alg")
    @Convert(converter = JWEAlgorithmStringConverter.class)
    private JWEAlgorithm userInfoEncryptedResponseAlg = null; // user_info_encrypted_response_alg

    @Column(name = "user_info_encrypted_response_enc")
    @Convert(converter = JWEEncryptionMethodStringConverter.class)
    private EncryptionMethod userInfoEncryptedResponseEnc = null; // user_info_encrypted_response_enc

    @Column(name="id_token_signed_response_alg")
    @Convert(converter = JWSAlgorithmStringConverter.class)
    private JWSAlgorithm idTokenSignedResponseAlg = null; // id_token_signed_response_alg

    @Column(name = "id_token_encrypted_response_alg")
    @Convert(converter = JWEAlgorithmStringConverter.class)
    private JWEAlgorithm idTokenEncryptedResponseAlg = null; // id_token_encrypted_response_alg

    @Column(name = "id_token_encrypted_response_enc")
    @Convert(converter = JWEEncryptionMethodStringConverter.class)
    private EncryptionMethod idTokenEncryptedResponseEnc = null; // id_token_encrypted_response_enc

    @Column(name="token_endpoint_auth_signing_alg")
    @Convert(converter = JWSAlgorithmStringConverter.class)
    private JWSAlgorithm tokenEndpointAuthSigningAlg = null; // token_endpoint_auth_signing_alg

    @Column(name="default_max_age")
    private Integer defaultMaxAge = 60000; // default_max_age

    @Column(name="require_auth_time")
    private Boolean requireAuthTime = true; // require_auth_time

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="client_default_acr_value", joinColumns=@JoinColumn(name="owner_id"))
    @Column(name="default_acr_value")
    private Set<String> defaultACRvalues; // default_acr_values

    @Column(name="initiate_login_uri")
    private String initiateLoginUri; // initiate_login_uri

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable( name="client_post_logout_redirect_uri", joinColumns=@JoinColumn(name="owner_id"))
    @Column(name="post_logout_redirect_uri")
    private Set<String> postLogoutRedirectUris; // post_logout_redirect_uris

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable( name="client_request_uri", joinColumns=@JoinColumn(name="owner_id"))
    @Column(name="request_uri")
    private Set<String> requestUris; // request_uris

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="client_authority", joinColumns=@JoinColumn(name="owner_id"))
    @Convert(converter = SimpleGrantedAuthorityStringConverter.class)
    @Column(name="authority")
    private Set<GrantedAuthority> authorities = new HashSet<>();

    @Column(name="access_token_validity_seconds")
    private Integer accessTokenValiditySeconds = 3600; // in seconds

    @Column(name="refresh_token_validity_seconds")
    private Integer refreshTokenValiditySeconds = 0; // in seconds

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="client_resource", joinColumns=@JoinColumn(name="owner_id"))
    @Column(name="resource_id")
    private Set<String> resourceIds = new HashSet<>();

    @Column(name="client_description")
    private String clientDescription = "";

    @Column(name="reuse_refresh_tokens")
    private boolean reuseRefreshToken = true;

    @Column(name="dynamically_registered")
    private boolean dynamicallyRegistered = false; // was this client dynamically registered?

    @Column(name="allow_introspection")
    private boolean allowIntrospection = false; // do we let this client call the introspection endpoint?

    @Column(name="id_token_validity_seconds")
    private Integer idTokenValiditySeconds = DEFAULT_ID_TOKEN_VALIDITY_SECONDS;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="created_at")
    private Date createdAt = new Date(); // time the client was created

    @Column(name = "clear_access_tokens_on_refresh")
    private boolean clearAccessTokensOnRefresh = true; // do we clear access tokens on refresh?

    @Column(name="device_code_validity_seconds")
    private Integer deviceCodeValiditySeconds = 1800;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable( name="client_claims_redirect_uri", joinColumns=@JoinColumn(name="owner_id"))
    @Column(name="redirect_uri")
    private Set<String> claimsRedirectUris;

    @Column(name = "software_statement")
    @Convert(converter = JWTStringConverter.class)
    private JWT softwareStatement;

    @Column(name = "code_challenge_method")
    @Convert(converter = PKCEAlgorithmStringConverter.class)
    private PKCEAlgorithm codeChallengeMethod;

    @PrePersist
    @PreUpdate
    private void prePersist() {
        if (getIdTokenValiditySeconds() == null) {
            setIdTokenValiditySeconds(DEFAULT_ID_TOKEN_VALIDITY_SECONDS);
        }
    }

    @Override
    public boolean isSecretRequired() {
        return false;
    }

    @Override
    public boolean isScoped() {
        return false;
    }

    @Override
    public Set<String> getAuthorizedGrantTypes() {
        return null;
    }

    @Override
    public Set<String> getRegisteredRedirectUri() {
        return null;
    }

    @Override
    public boolean isAutoApprove(String scope) {
        return false;
    }

    @Override
    public Map<String, Object> getAdditionalInformation() {
        return null;
    }

    public enum AuthMethod {
        SECRET_POST("client_secret_post"),
        SECRET_BASIC("client_secret_basic"),
        SECRET_JWT("client_secret_jwt"),
        PRIVATE_KEY("private_key_jwt"),
        NONE("none");

        private final String value;

        // map to aid reverse lookup
        private static final Map<String, AuthMethod> lookup = new HashMap<>();
        static {
            for (AuthMethod a : AuthMethod.values()) {
                lookup.put(a.getValue(), a);
            }
        }

        AuthMethod(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static AuthMethod getByValue(String value) {
            return lookup.get(value);
        }
    }

    public enum AppType {
        WEB("web"), NATIVE("native");

        private final String value;

        // map to aid reverse lookup
        private static final Map<String, AppType> lookup = new HashMap<>();
        static {
            for (AppType a : AppType.values()) {
                lookup.put(a.getValue(), a);
            }
        }

        AppType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static AppType getByValue(String value) {
            return lookup.get(value);
        }
    }

    public enum SubjectType {
        PAIRWISE("pairwise"), PUBLIC("public");

        private final String value;

        // map to aid reverse lookup
        private static final Map<String, SubjectType> lookup = new HashMap<>();
        static {
            for (SubjectType u : SubjectType.values()) {
                lookup.put(u.getValue(), u);
            }
        }

        SubjectType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static SubjectType getByValue(String value) {
            return lookup.get(value);
        }
    }
}
