package cz.muni.ics.oidc;

import cz.muni.ics.oidc.data.ClientRepository;
import cz.muni.ics.oidc.exception.PerunConnectionException;
import cz.muni.ics.oidc.exception.PerunUnknownException;
import cz.muni.ics.oidc.models.MitreidClient;
import cz.muni.ics.oidc.props.AttrsMapping;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.rpc.PerunAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class Synchronizer {

    private final PerunAdapter perunAdapter;
    private final ClientRepository clientRepository;
    private final AttrsMapping perunAttrNames;

    @Value("${encryption.secret}")
    private String secret;

    @Value("${proxy_identifier.identifier}")
    private String proxyIdentifier;

    @Value("${proxy_identifier.value}")
    private String proxyIdentifierValue;

    @Value("${protected_client_ids}")
    private final Set<String> skippedClientIds = new HashSet<>();

    public static final String CIPHER_PARAMS = "AES/ECB/PKCS5PADDING";
    public final Cipher cipher;
    private final SecretKeySpec secretKeySpec;

    @Autowired
    public Synchronizer(PerunAdapter perunAdapter, ClientRepository clientRepository, AttrsMapping perunAttrNames)
            throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        this.perunAdapter = perunAdapter;
        this.clientRepository = clientRepository;
        this.perunAttrNames = perunAttrNames;
        this.secretKeySpec = this.generateSecretKeySpec(secret);
        cipher = Cipher.getInstance(CIPHER_PARAMS);
    }

    public void setSkippedClientIds(Set<String> skippedClientIds) {
        if (skippedClientIds != null) {
            this.skippedClientIds.addAll(skippedClientIds);
        }
    }

    public void sync() {
        log.info("Started synchronization");
        int created = 0;
        int updated = 0;
        int deleted = 0;
        int errors = 0;
        Set<Facility> facilities;
        try {
            facilities = new HashSet<>(perunAdapter.getFacilitiesByAttribute(
                    proxyIdentifier, proxyIdentifierValue));
        } catch (PerunConnectionException | PerunUnknownException e) {
            log.error("Caught exception when fetching facilities by attr {} with value {}",
                    proxyIdentifier, proxyIdentifierValue, e);
            return;
        }
        Set<String> foundClientIds = new HashSet<>();
        for (Facility f : facilities) {
            try {
                log.debug("Processing facility {}", f);
                Map<String, PerunAttributeValue> attrsFromPerun = perunAdapter.getAttributesValues(
                        f.getId(), perunAttrNames.getNames());
                log.debug("Got facility attributes");
                log.trace("{}", attrsFromPerun);
                String clientId = attrsFromPerun.get(perunAttrNames.getClientId()).valueAsString();
                if (clientId == null) {
                    log.debug("ClientID is null, facility is probably not OIDC, skip it.");
                    continue;
                }
                foundClientIds.add(clientId);
                MitreidClient mitreClient = clientRepository.getClientByClientId(clientId);
                log.debug("Got MitreID client");
                log.trace("{}", mitreClient);
                if (mitreClient == null) {
                    this.createClient(attrsFromPerun);
                    created++;
                } else {
                    this.updateClient(mitreClient, attrsFromPerun);
                    updated++;
                }
            } catch (Exception e) {
                log.warn("Caught exception when syncing facility {}", f, e);
                errors++;
            }
        }
        try {
            deleted = deleteClients(this.getClientIdsToDelete(foundClientIds));
        } catch (Exception e) {
            log.warn("Caught exception when deleting unused clients", e);
            errors++;
        }
        log.info("Finished syncing:\n Created {}, Updated: {}, Deleted {}, errors: {}",
                created, updated, deleted, errors);
    }

    private int deleteClients(Set<String> clientIds) {
        log.debug("Deleting clients");
        int deleted = 0;
        if (!clientIds.isEmpty()) {
            deleted += clientRepository.deleteByClientIds(clientIds);
        }
        log.debug("Deleted {} clients", deleted);
        return deleted;
    }

    private Set<String> getClientIdsToDelete(Collection<String> foundClientIds) {
        Set<String> ids = clientRepository.getAllClientIds();
        ids.removeAll(foundClientIds);
        ids.removeAll(skippedClientIds);
        return ids;
    }

    private void createClient(Map<String, PerunAttributeValue> attrs)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        log.debug("No client found in DB, create one");
        MitreidClient c = new MitreidClient();
        c.setClientId(attrs.get(perunAttrNames.getClientId()).valueAsString());
        this.setClientFields(c, attrs);
        c.setCreatedAt(new Date());
        clientRepository.saveClient(c);
        log.debug("Client created");
    }

    private void updateClient(MitreidClient c, Map<String, PerunAttributeValue> attrs)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        log.debug("Client found, update configuration");
        this.setClientFields(c, attrs);
        clientRepository.updateClient(c.getId(), c);
        log.debug("Client updated");
    }

    private void setClientFields(MitreidClient c, Map<String, PerunAttributeValue> attrs)
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
