package cz.muni.ics.oidc;

import cz.muni.ics.oidc.data.ClientRepository;
import cz.muni.ics.oidc.exception.PerunConnectionException;
import cz.muni.ics.oidc.exception.PerunUnknownException;
import cz.muni.ics.oidc.models.MitreidClient;
import cz.muni.ics.oidc.models.ResultCounter;
import cz.muni.ics.oidc.props.AttrsMapping;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.props.ActionsProperties;
import cz.muni.ics.oidc.rpc.PerunAdapter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    private final String proxyIdentifier;
    private final String proxyIdentifierValue;
    private final ActionsProperties actionsProperties;

    public static final String CIPHER_PARAMS = "AES/ECB/PKCS5PADDING";
    public final Cipher cipher;
    private final SecretKeySpec secretKeySpec;

    @Autowired
    public Synchronizer(@NonNull PerunAdapter perunAdapter,
                        @NonNull ClientRepository clientRepository,
                        @NonNull AttrsMapping perunAttrNames,
                        @NonNull @Value("${proxy_identifier.attr}") String proxyIdentifier,
                        @NonNull @Value("${proxy_identifier.value}") String proxyIdentifierValue,
                        @NonNull @Value("${encryption_secret}") String secret,
                        @NonNull ActionsProperties actionsProperties)
            throws NoSuchAlgorithmException, NoSuchPaddingException
    {
        this.perunAdapter = perunAdapter;
        this.clientRepository = clientRepository;
        this.perunAttrNames = perunAttrNames;
        this.proxyIdentifier = proxyIdentifier;
        this.proxyIdentifierValue = proxyIdentifierValue;
        this.actionsProperties = actionsProperties;
        this.secretKeySpec = this.generateSecretKeySpec(secret);
        cipher = Cipher.getInstance(CIPHER_PARAMS);
    }

    public void sync() {
        log.info("Started synchronization");
        ResultCounter res = new ResultCounter();
        Set<Facility> facilities;
        try {
            facilities = new HashSet<>(perunAdapter.getFacilitiesByAttribute(
                    proxyIdentifier, proxyIdentifierValue));
        } catch (PerunConnectionException | PerunUnknownException e) {
            log.error("Caught exception when fetching facilities by attr '{}' with value '{}'",
                    proxyIdentifier, proxyIdentifierValue, e);
            return;
        }
        log.info("Processing facilities");
        Set<String> foundClientIds = new HashSet<>();
        for (Facility f : facilities) {
            processFacility(f, foundClientIds, res);
        }
        log.info("Removing old clients");
        deleteClients(foundClientIds, res);
        log.info("Finished syncing - {}", res);
    }

    private void processFacility(Facility f, Set<String> foundClientIds, ResultCounter res) {
        try {
            log.debug("Processing facility '{}'", f);
            if (f == null || f.getId() == null) {
                log.warn("NULL facility or no ID for facility: '{}'", f);
                res.incErrors();
                return;
            }
            Map<String, PerunAttributeValue> attrsFromPerun = getAttrsFromPerun(f.getId());
            String clientId = attrsFromPerun.get(perunAttrNames.getClientId()).valueAsString();
            if (!StringUtils.hasText(clientId)) {
                log.debug("ClientID is null, facility is probably not OIDC, skip it.");
                return;
            } else if (actionsProperties.getProtectedClientIds().contains(clientId)) {
                log.debug("ClientID is marked as protected in configuration, skip it.");
                return;
            }
            foundClientIds.add(clientId);
            MitreidClient mitreClient = getMitreClient(clientId);
            if (mitreClient == null) {
                log.info("No client found for client_id '{}' - create new", clientId);
                createClient(attrsFromPerun, res);
            } else {
                log.info("Existing client found for client_id '{}' - update it", clientId);
                updateClient(mitreClient, attrsFromPerun, res);
            }
            log.info("Client with id '{}' processed", clientId);
        } catch (Exception e) {
            log.warn("Caught exception when syncing facility {}", f, e);
            res.incErrors();
        }
    }

    private MitreidClient getMitreClient(String clientId) {
        MitreidClient mitreClient = clientRepository.getClientByClientId(clientId);
        log.debug("Got MitreID client");
        log.trace("{}", mitreClient);
        return mitreClient;
    }

    private Map<String, PerunAttributeValue> getAttrsFromPerun(Long facilityId)
            throws PerunUnknownException, PerunConnectionException
    {
        Map<String, PerunAttributeValue> attrsFromPerun = perunAdapter.getAttributesValues(
                facilityId, perunAttrNames.getNames());
        log.debug("Got facility attributes");
        log.trace("{}", attrsFromPerun);
        return attrsFromPerun;
    }

    private Set<String> getClientIdsToDelete(Collection<String> foundClientIds) {
        Set<String> ids = clientRepository.getAllClientIds();
        ids.removeAll(foundClientIds);
        ids.removeAll(actionsProperties.getProtectedClientIds());
        return ids;
    }

    private void createClient(Map<String, PerunAttributeValue> attrs, ResultCounter res)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        if (actionsProperties.isCreate()) {
            MitreidClient c = new MitreidClient();
            setClientFields(c, attrs);
            c.setCreatedAt(new Date());
            clientRepository.saveClient(c);
            log.debug("Client created");
            res.incCreated();
        } else {
            log.warn("Creating clients is disabled, skip creation");
        }
    }

    private void updateClient(MitreidClient c, Map<String, PerunAttributeValue> attrs, ResultCounter res)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        if (actionsProperties.isUpdate()) {
            this.setClientFields(c, attrs);
            clientRepository.updateClient(c.getId(), c);
            log.debug("Client updated");
            res.incUpdated();
        } else {
            log.warn("Updating clients is disabled, skip update");
        }
    }

    private void deleteClients(Set<String> foundClientIds, ResultCounter res) {
        Set<String> clientsToDelete = getClientIdsToDelete(foundClientIds);
        if (actionsProperties.isDelete()) {
            try {
                log.debug("Deleting clients with ids {}", clientsToDelete);
                int deleted = 0;
                if (!clientsToDelete.isEmpty()) {
                    deleted += clientRepository.deleteByClientIds(clientsToDelete);
                }
                log.debug("Deleted {} clients", deleted);
                res.incDeleted(deleted);
            } catch (Exception e) {
                log.warn("Caught exception when deleting unused clients", e);
                res.incErrors();
            }
        } else {
            log.warn("Deleting of clients is disabled. Following clientIDs would be deleted: {}", clientsToDelete);
        }
    }

    private void setClientFields(MitreidClient c, Map<String, PerunAttributeValue> attrs)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        c.setClientId(attrs.get(perunAttrNames.getClientId()).valueAsString());
        c.setClientSecret(decrypt(attrs.get(perunAttrNames.getClientSecret()).valueAsString()));
        c.setClientName(attrs.get(perunAttrNames.getName()).valueAsMap().get("en"));
        c.setClientDescription(attrs.get(perunAttrNames.getDescription()).valueAsMap().get("en"));
        c.setPolicyUri(attrs.get(perunAttrNames.getPrivacyPolicy()).valueAsString());
        c.setContacts(Collections.singleton(attrs.get(perunAttrNames.getContacts()).valueAsString()));
        Set<String> scopes = new HashSet<>(attrs.get(perunAttrNames.getScopes()).valueAsList());
        if (attrs.containsKey(perunAttrNames.getIssueRefreshTokens())
                && attrs.get(perunAttrNames.getIssueRefreshTokens()).valueAsBoolean()) {
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
