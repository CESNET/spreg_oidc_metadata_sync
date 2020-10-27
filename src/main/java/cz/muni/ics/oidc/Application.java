package cz.muni.ics.oidc;

import cz.muni.ics.oidc.models.AttrsMapping;
import cz.muni.ics.oidc.models.ClientDetailsEntity;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.rpc.PerunAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {

    private final PerunAdapter perunAdapter;
    private final ClientRepository clientRepository;
    private final AttrsMapping perunAttrNames;

    @Value("${encryption.secret}")
    private String secret;

    @Value("${proxy_identifier}")
    private String proxyIdentifier;

    @Value("${proxy_identifier_value}")
    private String proxyIdentifierValue;

    private SecretKeySpec secretKeySpec;

    public static final String CIPHER_PARAMS = "AES/ECB/PKCS5PADDING";
    public static Cipher cipher;
    static {
        try {
            cipher = Cipher.getInstance(CIPHER_PARAMS);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }

    @Autowired
    public Application(PerunAdapter adapter, ClientRepository repository, AttrsMapping attributes) {
        this.perunAdapter = adapter;
        this.clientRepository = repository;
        this.perunAttrNames = attributes;
    }

    public static void main(String[] args) {
        log.info("Starting application");
        new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.SERVLET)
                .run(args);
        log.info("Closing application");
    }

    @Override
    public void run(String... args) throws Exception {
        this.secretKeySpec = this.generateSecretKeySpec(secret);
        List<Facility> facilities = perunAdapter.getFacilitiesByAttribute(proxyIdentifier, proxyIdentifierValue);
        for (Facility f: facilities) {
            log.info("Processing facility {}", f);
            Map<String, PerunAttributeValue> attrsFromPerun = perunAdapter.getAttributesValues(f.getId(), perunAttrNames.getNames());
            log.info("Got facility attributes");
            String clientId = attrsFromPerun.get(perunAttrNames.getClientId()).valueAsString();
            if (clientId == null) {
                log.info("ClientID is null, facility is probably not OIDC, skip it.");
                continue;
            }
            ClientDetailsEntity mitreClient = clientRepository.getClientByClientId(clientId);
            log.info("Obtaining MitreID client");
            if (mitreClient == null) {
                log.info("No client found, create one");
                this.createClient(attrsFromPerun);
            } else {
                log.info("Client found, update configuration");
                this.updateClient(mitreClient, attrsFromPerun);
            }
            //TODO: figure out delete
        }
    }

    private void createClient(Map<String, PerunAttributeValue> attrs)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        ClientDetailsEntity c = new ClientDetailsEntity();
        c.setClientId(attrs.get(perunAttrNames.getClientId()).valueAsString());
        this.setFields(c, attrs);
        c.setCreatedAt(new Date());
        clientRepository.saveClient(c);
        log.info("Client created");
    }

    private void updateClient(ClientDetailsEntity c, Map<String, PerunAttributeValue> attrs)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        this.setFields(c, attrs);
        clientRepository.updateClient(c.getId(), c);
        log.info("Client updated");
    }

    private void setFields(ClientDetailsEntity c, Map<String, PerunAttributeValue> attrs)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        c.setClientSecret(decrypt(attrs.get(perunAttrNames.getClientSecret()).valueAsString()));
        c.setClientName(attrs.get(perunAttrNames.getName()).valueAsMap().get("en"));
        c.setClientDescription(attrs.get(perunAttrNames.getDescription()).valueAsMap().get("en"));
        c.setPolicyUri(attrs.get(perunAttrNames.getPrivacyPolicy()).valueAsString());
        c.setContacts(Collections.singleton(attrs.get(perunAttrNames.getContacts()).valueAsString()));
        Set<String> scopes = new HashSet<>(attrs.get(perunAttrNames.getScopes()).valueAsList());
        if (attrs.get(perunAttrNames.getIssueRefreshTokens()).valueAsBoolean()) {
            scopes.add("offline_access");
        }
        c.setScope(scopes);
        c.setGrantTypes(new HashSet<>(attrs.get(perunAttrNames.getGrantTypes()).valueAsList()));
        c.setResponseTypes(new HashSet<>(attrs.get(perunAttrNames.getResponseTypes()).valueAsList()));
        c.setAllowIntrospection(attrs.get(perunAttrNames.getIntrospection()).valueAsBoolean());
        c.setPostLogoutRedirectUris(new HashSet<>(attrs.get(perunAttrNames.getPostLogoutRedirectUris()).valueAsList()));
    }

    private String decrypt(String strToDecrypt)
            throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException
    {
        if (strToDecrypt == null || strToDecrypt.equalsIgnoreCase("null")) {
            return null;
        }

        Base64.Decoder b64dec = Base64.getUrlDecoder();
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        return new String(cipher.doFinal(b64dec.decode(strToDecrypt)));
    }

    private SecretKeySpec generateSecretKeySpec(String secret) throws NoSuchAlgorithmException {
        secret = fixSecret(secret);
        MessageDigest sha;
        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16);
        return new SecretKeySpec(key, "AES");
    }

    private String fixSecret(String s) {
        if (s.length() < 32) {
            int missingLength = 32 - s.length();
            StringBuilder sBuilder = new StringBuilder(s);
            for (int i = 0; i < missingLength; i++) {
                sBuilder.append('A');
            }
            s = sBuilder.toString();
        }
        return s.substring(0, 32);
    }

}
