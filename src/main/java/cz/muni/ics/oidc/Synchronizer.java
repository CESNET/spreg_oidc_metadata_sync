package cz.muni.ics.oidc;

import cz.muni.ics.oidc.data.ClientRepository;
import cz.muni.ics.oidc.exception.PerunConnectionException;
import cz.muni.ics.oidc.exception.PerunUnknownException;
import cz.muni.ics.oidc.models.AttrsMapping;
import cz.muni.ics.oidc.models.ClientDetailsEntity;
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
import java.util.List;
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

    @Value("${proxy_identifier.identifer}")
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

    public void sync() throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException,
            PerunUnknownException, PerunConnectionException
    {
        List<Facility> facilities = perunAdapter.getFacilitiesByAttribute(
                proxyIdentifier, proxyIdentifierValue);
        Set<String> foundClientIds = new HashSet<>();
        for (Facility f : facilities) {
            log.info("Processing facility {}", f);
            Map<String, PerunAttributeValue> attrsFromPerun = perunAdapter.getAttributesValues(
                    f.getId(), perunAttrNames.getNames());
            log.info("Got facility attributes");
            String clientId = attrsFromPerun.get(perunAttrNames.getClientId()).valueAsString();
            if (clientId == null) {
                log.info("ClientID is null, facility is probably not OIDC, skip it.");
                continue;
            }
            foundClientIds.add(clientId);
            ClientDetailsEntity mitreClient = clientRepository.getClientByClientId(clientId);
            log.info("Obtaining MitreID client");
            if (mitreClient == null) {
                this.createClient(attrsFromPerun);
            } else {
                this.updateClient(mitreClient, attrsFromPerun);
            }
        }
        this.deleteClients(this.getClientIdsToDelete(foundClientIds));
    }

    private void deleteClients(Set<String> clientIds) {
        log.info("Deleting clients");
        int deleted = 0;
        if (!clientIds.isEmpty()) {
            deleted += clientRepository.deleteByClientIds(clientIds);
        }
        log.info("Deleted {} clients", deleted);
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
        log.info("No client found in DB, create one");
        ClientDetailsEntity c = new ClientDetailsEntity();
        c.setClientId(attrs.get(perunAttrNames.getClientId()).valueAsString());
        this.setClientFields(c, attrs);
        c.setCreatedAt(new Date());
        clientRepository.saveClient(c);
        log.info("Client created");
    }

    private void updateClient(ClientDetailsEntity c, Map<String, PerunAttributeValue> attrs)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        log.info("Client found, update configuration");
        this.setClientFields(c, attrs);
        clientRepository.updateClient(c.getId(), c);
        log.info("Client updated");
    }

    private void setClientFields(ClientDetailsEntity c, Map<String, PerunAttributeValue> attrs)
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
