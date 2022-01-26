package cz.muni.ics.oidc;

import cz.muni.ics.oidc.data.ClientRepository;
import cz.muni.ics.oidc.exception.PerunConnectionException;
import cz.muni.ics.oidc.exception.PerunUnknownException;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.MitreidClient;
import cz.muni.ics.oidc.models.PKCEAlgorithm;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.models.SyncResult;
import cz.muni.ics.oidc.props.ActionsProperties;
import cz.muni.ics.oidc.props.AttrsMapping;
import cz.muni.ics.oidc.props.ConfProperties;
import cz.muni.ics.oidc.props.GrantTypesTimeoutsProperties;
import cz.muni.ics.oidc.rpc.PerunAdapter;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static cz.muni.ics.oidc.Synchronizer.DO_YOU_WANT_TO_PROCEED;
import static cz.muni.ics.oidc.Synchronizer.SPACER;
import static cz.muni.ics.oidc.Synchronizer.Y;
import static cz.muni.ics.oidc.Synchronizer.YES;

@Component
@Slf4j
public class ToOidcSynchronizer {

    public static final String OFFLINE_ACCESS = "offline_access";

    // flow types
    public static final String AUTHORIZATION_CODE = "authorization code";
    public static final String DEVICE = "device";
    public static final String IMPLICIT = "implicit";
    public static final String HYBRID = "hybrid";

    // grant types
    public static final String GRANT_AUTHORIZATION_CODE = "authorization_code";
    public static final String GRANT_IMPLICIT = "implicit";
    public static final String GRANT_DEVICE = "urn:ietf:params:oauth:grant-type:device_code";
    public static final String GRANT_HYBRID = "hybrid";
    public static final String GRANT_REFRESH_TOKEN = "refresh_token";

    // timeouts
    public static final String ACCESS_TOKEN_TIMEOUT = "access_token";
    public static final String ID_TOKEN_TIMEOUT = "id_token";
    public static final String REFRESH_TOKEN_TIMEOUT = "refresh_token";
    public static final String DEVICE_CODE_TIMEOUT = "device_code";

    // response types
    public static final String RESPONSE_CODE = "code";
    public static final String RESPONSE_TOKEN = "token";
    public static final String RESPONSE_ID_TOKEN = "id_token";
    public static final String RESPONSE_TOKEN_ID_TOKEN = RESPONSE_TOKEN + " " + RESPONSE_ID_TOKEN;
    public static final String RESPONSE_ID_TOKEN_TOKEN = RESPONSE_ID_TOKEN + " " + RESPONSE_TOKEN;
    public static final String RESPONSE_CODE_ID_TOKEN = RESPONSE_CODE + " " + RESPONSE_ID_TOKEN;
    public static final String RESPONSE_CODE_TOKEN = RESPONSE_CODE + " " + RESPONSE_TOKEN;
    public static final String RESPONSE_CODE_TOKEN_ID_TOKEN = RESPONSE_CODE_TOKEN + " " + RESPONSE_ID_TOKEN;
    public static final String RESPONSE_CODE_ID_TOKEN_TOKEN = RESPONSE_CODE_ID_TOKEN + " " + RESPONSE_TOKEN;
//    public static final String RESPONSE_NONE = "none";

    // response types for grant types
    public static final String[] RESPONSE_TYPE_AUTH_CODE = { RESPONSE_CODE };
    public static final String[] RESPONSE_TYPE_IMPLICIT = {
            RESPONSE_ID_TOKEN, RESPONSE_TOKEN, RESPONSE_ID_TOKEN_TOKEN, RESPONSE_TOKEN_ID_TOKEN
    };
    public static final String[] RESPONSE_TYPE_HYBRID = {
            RESPONSE_CODE_TOKEN, RESPONSE_CODE_ID_TOKEN, RESPONSE_CODE_ID_TOKEN,
            RESPONSE_CODE_ID_TOKEN_TOKEN, RESPONSE_CODE_TOKEN_ID_TOKEN
    };

    public static final String PKCE_TYPE_NONE = "none";
    public static final String PKCE_TYPE_PLAIN = "plain code challenge";
    public static final String PKCE_TYPE_SHA256 = "SHA256 code challenge";

    private final PerunAdapter perunAdapter;
    private final String proxyIdentifier;
    private final String proxyIdentifierValue;
    private final AttrsMapping perunAttrNames;
    private final ClientRepository clientRepository;
    private final ActionsProperties actionsProperties;
    private final GrantTypesTimeoutsProperties grantTypesTimeoutsProperties;
    private final Cipher cipher;
    private final SecretKeySpec secretKeySpec;

    private final Scanner scanner = new Scanner(System.in);

    private boolean interactiveMode = false;
    private boolean proceedToDelete = true;

    @Autowired
    public ToOidcSynchronizer(@NonNull PerunAdapter perunAdapter,
                              @NonNull ConfProperties confProperties,
                              @NonNull AttrsMapping perunAttrNames,
                              @NonNull ClientRepository clientRepository,
                              @NonNull ActionsProperties actionsProperties,
                              @NonNull GrantTypesTimeoutsProperties grantTypesTimeoutsProperties,
                              @NonNull Cipher cipher,
                              @NonNull SecretKeySpec secretKeySpec)
    {
        this.perunAdapter = perunAdapter;
        this.perunAttrNames = perunAttrNames;
        this.clientRepository = clientRepository;
        this.actionsProperties = actionsProperties;
        this.grantTypesTimeoutsProperties = grantTypesTimeoutsProperties;
        this.cipher = cipher;
        this.secretKeySpec = secretKeySpec;
        this.proxyIdentifier = perunAttrNames.getProxyIdentifier();
        this.proxyIdentifierValue = confProperties.getProxyIdentifierValue();
    }

    public SyncResult syncToOidc(boolean interactiveMode) {
        this.interactiveMode = interactiveMode;
        log.info("Started synchronization to OIDC DB");
        SyncResult res = new SyncResult();
        Set<Facility> facilities;
        try {
            facilities = new HashSet<>(perunAdapter.getFacilitiesByAttribute(
                    proxyIdentifier, proxyIdentifierValue));
        } catch (PerunConnectionException | PerunUnknownException e) {
            log.error("Caught exception when fetching facilities by attr '{}' with value '{}'",
                    proxyIdentifier, proxyIdentifierValue, e);
            return res;
        }
        log.info("Processing facilities");
        Set<String> foundClientIds = new HashSet<>();
        for (Facility f : facilities) {
            processFacility(f, foundClientIds, res);
        }
        if (proceedToDelete) {
            log.info("Removing old clients");
            deleteClients(foundClientIds, res);
        } else {
            log.warn("Script has disabled removing of old clients. This might be due to Peruns unreachability! Check previous logs for more info.");
        }
        return res;
    }

    private void processFacility(Facility f, Set<String> foundClientIds, SyncResult res) {
        try {
            if (f == null) {
                log.warn("NULL facility given, generating error and continue on processing");
                res.incErrors();
                return;
            }
            log.debug("Processing facility '{}'", f);
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
        } catch (PerunConnectionException | PerunUnknownException ex) {
            log.warn("Caught exception from Perun. Can be unreachable. Disabling client removal!", ex);
            proceedToDelete = false;
            res.incErrors();
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

    private void createClient(Map<String, PerunAttributeValue> attrs, SyncResult res)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        if (actionsProperties.getToOidc().isCreate()) {
            MitreidClient c = new MitreidClient();
            setClientFields(c, attrs);
            c.setCreatedAt(new Date());
            if (interactiveMode) {
                System.out.println("Following client will be created");
                System.out.println(c);
                System.out.println(DO_YOU_WANT_TO_PROCEED);
                String response = scanner.nextLine();
                if (!Y.equalsIgnoreCase(response) && !YES.equalsIgnoreCase(response)) {
                    return;
                }
            }
            clientRepository.saveClient(c);
            log.debug("Client created");
            res.incCreated();
        } else {
            log.warn("Creating clients is disabled, skip creation");
        }
    }

    private void updateClient(MitreidClient original, Map<String, PerunAttributeValue> attrs, SyncResult res)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        if (actionsProperties.getToOidc().isUpdate()) {
            MitreidClient toUpdate;
            if (interactiveMode) {
                MitreidClient updated = clientRepository.getClientByClientId(original.getClientId());
                this.setClientFields(updated, attrs);
                DiffNode diff = ObjectDifferBuilder.buildDefault().compare(original, updated);
                if (diff.hasChanges()) {
                    System.out.println(SPACER);
                    diff.visit((node, visit) -> diffVisit(node, original, updated));
                    System.out.println(DO_YOU_WANT_TO_PROCEED);
                    String response = scanner.nextLine();
                    if (!Y.equalsIgnoreCase(response) && !YES.equalsIgnoreCase(response)) {
                        return;
                    } else {
                        System.out.println(SPACER);
                    }
                }
                toUpdate = updated;
                clientRepository.updateClient(original.getId(), updated);
            } else {
                this.setClientFields(original, attrs);
                toUpdate = original;
            }
            clientRepository.updateClient(original.getId(), toUpdate);
            log.debug("Client updated");
            res.incUpdated();
        } else {
            log.warn("Updating clients is disabled, skip update");
        }
    }

    private void diffVisit(DiffNode node, MitreidClient original, MitreidClient updated) {
        if (node.isRootNode()) {
            return;
        }
        Object baseValue = node.canonicalGet(original);
        Object workingValue = node.canonicalGet(updated);
        if (node.getParentNode().isRootNode()) {
            System.out.printf("Changes in field '%s'\n",
                    node.getElementSelector().toHumanReadableString());
            System.out.printf("  original: '%s'\n", baseValue);
            System.out.printf("  updated: '%s'\n", workingValue);
            System.out.println("  diff:");
        } else {
            if (baseValue == null) {
                System.out.printf("    added: '%s'\n", workingValue);
            } else if (workingValue == null) {
                System.out.printf("    removed: '%s'\n", baseValue);
            } else {
                System.out.printf("    changed: '%s' to: '%s'\n", baseValue, workingValue);
            }
        }
    }

    private void deleteClients(Set<String> foundClientIds, SyncResult res) {
        Set<String> clientsToDelete = getClientIdsToDelete(foundClientIds);
        if (actionsProperties.getToOidc().isDelete()) {
            if (interactiveMode) {
                for (String clientId: clientsToDelete) {
                    MitreidClient c = clientRepository.getClientByClientId(clientId);
                    System.out.println("About to remove following client");
                    System.out.println(c);
                    System.out.println(DO_YOU_WANT_TO_PROCEED);
                    String response = scanner.nextLine();
                    if (!Y.equalsIgnoreCase(response) && !YES.equalsIgnoreCase(response)) {
                        continue;
                    }
                    clientRepository.deleteClient(c);
                }
            } else {
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
            }
        } else {
            log.warn("Deleting of clients is disabled. Following clientIDs would be deleted: {}", clientsToDelete);
        }
    }

    private void setClientFields(MitreidClient c, Map<String, PerunAttributeValue> attrs)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        c.setClientId(attrs.get(perunAttrNames.getClientId()).valueAsString());
        c.setClientSecret(Utils.decrypt(
                attrs.get(perunAttrNames.getClientSecret()).valueAsString(), cipher, secretKeySpec));
        c.setClientName(attrs.get(perunAttrNames.getName()).valueAsMap().get("en"));
        c.setClientDescription(attrs.get(perunAttrNames.getDescription()).valueAsMap().get("en"));
        c.setRedirectUris(new HashSet<>(attrs.get(perunAttrNames.getRedirectUris()).valueAsList()));
        c.setAllowIntrospection(attrs.get(perunAttrNames.getIntrospection()).valueAsBoolean());
        c.setPostLogoutRedirectUris(new HashSet<>(attrs.get(perunAttrNames.getPostLogoutRedirectUris()).valueAsList()));
        c.setScope(new HashSet<>(attrs.get(perunAttrNames.getScopes()).valueAsList()));
        setPolicyUri(c, attrs);
        setContacts(c, attrs);
        setClientUri(c, attrs);
        setGrantAndResponseTypes(c, attrs);
        setRefreshTokens(c, attrs);
        setTokenTimeouts(c, attrs);
    }

    private void setRefreshTokens(MitreidClient c, Map<String, PerunAttributeValue> attrs) {
        Set<String> grantTypes = c.getGrantTypes();
        if (grantTypes == null) {
            grantTypes = new HashSet<>();
        }
        if (grantAllowsRefreshTokens(grantTypes)) {
            boolean requestedViaAttr = attrs.containsKey(perunAttrNames.getIssueRefreshTokens())
                    && attrs.get(perunAttrNames.getIssueRefreshTokens()).valueAsBoolean();
            boolean requestedViaScopes = c.getScope().contains(OFFLINE_ACCESS);
            log.debug("Refresh tokens requested via: attr({}), scopes({})", requestedViaAttr, requestedViaScopes);
            if (requestedViaAttr || requestedViaScopes) {
                setUpRefreshTokens(c, attrs);
            }
        }
    }

    private void setUpRefreshTokens(MitreidClient c, Map<String, PerunAttributeValue> attrs) {
        c.getScope().add(OFFLINE_ACCESS);
        c.getGrantTypes().add(GRANT_REFRESH_TOKEN);
        c.setClearAccessTokensOnRefresh(true);
        c.setReuseRefreshToken(false);
        PerunAttributeValue reuseTokens = attrs.getOrDefault(perunAttrNames.getReuseRefreshTokens(), null);
        if (reuseTokens != null) {
            c.setReuseRefreshToken(reuseTokens.valueAsBoolean());
        }
    }

    private boolean grantAllowsRefreshTokens(Set<String> grantTypes) {
        boolean res = !grantTypes.isEmpty()
                && (grantTypes.contains(GRANT_DEVICE)
                || grantTypes.contains(GRANT_AUTHORIZATION_CODE)
                || grantTypes.contains(GRANT_HYBRID));
        log.debug("Grants '{}' {} issuing refresh tokens", grantTypes, res ? "allow" : "disallow");
        return res;
    }

    private void setGrantAndResponseTypes(MitreidClient c, Map<String, PerunAttributeValue> attrs) {
        List<String> grantTypesAttrValue = attrs.get(perunAttrNames.getGrantTypes()).valueAsList().stream()
                .map(String::toLowerCase).collect(Collectors.toList());

        Set<String> grantTypes = new HashSet<>();
        Set<String> responseTypes = new HashSet<>();

        if (grantTypesAttrValue.contains(AUTHORIZATION_CODE)) {
            grantTypes.add(GRANT_AUTHORIZATION_CODE);
            responseTypes.addAll(Arrays.asList(RESPONSE_TYPE_AUTH_CODE));
            log.debug("Added grant '{}' with response types '{}'", GRANT_AUTHORIZATION_CODE, RESPONSE_TYPE_AUTH_CODE);
        }

        if (grantTypesAttrValue.contains(IMPLICIT)) {
            grantTypes.add(GRANT_IMPLICIT);
            responseTypes.addAll(Arrays.asList(RESPONSE_TYPE_IMPLICIT));
            log.debug("Added grant '{}' with response types '{}'", GRANT_IMPLICIT, RESPONSE_TYPE_IMPLICIT);
        }

        if (grantTypesAttrValue.contains(HYBRID)) {
            grantTypes.add(GRANT_HYBRID);
            grantTypes.add(GRANT_AUTHORIZATION_CODE);
            responseTypes.addAll(Arrays.asList(RESPONSE_TYPE_HYBRID));
            log.debug("Added grants '{} {}' with response types '{}'", GRANT_HYBRID, GRANT_AUTHORIZATION_CODE,
                    RESPONSE_TYPE_HYBRID);
        }

        if (grantTypesAttrValue.contains(DEVICE)) {
            grantTypes.add(GRANT_DEVICE);
            log.debug("Added grant '{}'", GRANT_DEVICE);
        }

        if (grantTypes.contains(GRANT_AUTHORIZATION_CODE)) {
            setPKCEOptionsForAuthorizationCode(c, attrs);
        }

        c.setGrantTypes(grantTypes);
        c.setResponseTypes(responseTypes);
    }

    private void setPKCEOptionsForAuthorizationCode(MitreidClient c, Map<String, PerunAttributeValue> attrs) {
        log.trace("Setting PKCE options");
        String codeChallengeType = attrs.get(perunAttrNames.getCodeChallengeType()).valueAsString();
        c.setCodeChallengeMethod(null);
        if (!PKCE_TYPE_NONE.equalsIgnoreCase(codeChallengeType)) {
            log.debug("Code challenge requested is not equal to '{}'", PKCE_TYPE_NONE);
            if (PKCE_TYPE_PLAIN.equalsIgnoreCase(codeChallengeType)) {
                log.debug("Preparing for PKCE with challenge '{}'", PKCE_TYPE_PLAIN);
                preparePkce(c);
                c.setCodeChallengeMethod(PKCEAlgorithm.plain);
            } else if (PKCE_TYPE_SHA256.equalsIgnoreCase(codeChallengeType)) {
                log.debug("Preparing for PKCE with challenge '{}'", PKCE_TYPE_SHA256);
                preparePkce(c);
                c.setCodeChallengeMethod(PKCEAlgorithm.S256);
            }
        }
    }

    private void preparePkce(MitreidClient c) {
        c.setClientSecret(null);
        c.setTokenEndpointAuthMethod(MitreidClient.AuthMethod.NONE);
    }

    private void setTokenTimeouts(MitreidClient c, Map<String, PerunAttributeValue> attrs) {
        Set<String> grantTypes = c.getGrantTypes();
        PerunAttributeValue attrValue = attrs.getOrDefault(perunAttrNames.getTokenTimeouts(), null);

        Map<String, String> attrValueAsMap;
        if (attrValue != null) {
            attrValueAsMap = attrValue.valueAsMap();
        } else {
            attrValueAsMap = new HashMap<>();
        }

        Map<String, Integer> tokenTimeouts = new HashMap<>();
        tokenTimeouts.put(ACCESS_TOKEN_TIMEOUT, 0);
        tokenTimeouts.put(ID_TOKEN_TIMEOUT, 0);
        tokenTimeouts.put(REFRESH_TOKEN_TIMEOUT, 0);
        tokenTimeouts.put(DEVICE_CODE_TIMEOUT, 0);

        if (grantTypes.contains(GRANT_AUTHORIZATION_CODE)) {
            setDefaultTokenTimeoutsByGrantType(grantTypesTimeoutsProperties.getAuthorizationCode(), tokenTimeouts);
        }
        if (grantTypes.contains(GRANT_IMPLICIT)) {
            setDefaultTokenTimeoutsByGrantType(grantTypesTimeoutsProperties.getImplicit(), tokenTimeouts);
        }
        if (grantTypes.contains(GRANT_HYBRID)) {
            setDefaultTokenTimeoutsByGrantType(grantTypesTimeoutsProperties.getHybrid(), tokenTimeouts);
        }
        if (grantTypes.contains(GRANT_DEVICE)) {
            setDefaultTokenTimeoutsByGrantType(grantTypesTimeoutsProperties.getDevice(), tokenTimeouts);
        }

        if (attrValueAsMap.containsKey(ACCESS_TOKEN_TIMEOUT)) {
            tokenTimeouts.put(ACCESS_TOKEN_TIMEOUT, Integer.parseInt(attrValueAsMap.get(ACCESS_TOKEN_TIMEOUT)));
        }
        if (attrValueAsMap.containsKey(ID_TOKEN_TIMEOUT)) {
            tokenTimeouts.put(ID_TOKEN_TIMEOUT, Integer.parseInt(attrValueAsMap.get(ID_TOKEN_TIMEOUT)));
        }
        if (attrValueAsMap.containsKey(REFRESH_TOKEN_TIMEOUT)) {
            tokenTimeouts.put(REFRESH_TOKEN_TIMEOUT, Integer.parseInt(attrValueAsMap.get(REFRESH_TOKEN_TIMEOUT)));
        }
        if (attrValueAsMap.containsKey(DEVICE_CODE_TIMEOUT)) {
            tokenTimeouts.put(DEVICE_CODE_TIMEOUT, Integer.parseInt(attrValueAsMap.get(DEVICE_CODE_TIMEOUT)));
        }

        c.setAccessTokenValiditySeconds(tokenTimeouts.get(ACCESS_TOKEN_TIMEOUT));
        c.setIdTokenValiditySeconds(tokenTimeouts.get(ID_TOKEN_TIMEOUT));
        c.setRefreshTokenValiditySeconds(tokenTimeouts.get(REFRESH_TOKEN_TIMEOUT));
        c.setDeviceCodeValiditySeconds(tokenTimeouts.get(DEVICE_CODE_TIMEOUT));
    }

    private void setDefaultTokenTimeoutsByGrantType(
            GrantTypesTimeoutsProperties.GrantType grantType,
            Map<String, Integer> tokenTimeouts
    ) {
        if (grantType.getAccessToken() > tokenTimeouts.get(ACCESS_TOKEN_TIMEOUT)) {
            tokenTimeouts.put(ACCESS_TOKEN_TIMEOUT, grantType.getAccessToken());
        }
        if (grantType.getIdToken() > tokenTimeouts.get(ID_TOKEN_TIMEOUT)) {
            tokenTimeouts.put(ID_TOKEN_TIMEOUT, grantType.getIdToken());
        }
        if (grantType.getRefreshToken() > tokenTimeouts.get(REFRESH_TOKEN_TIMEOUT)) {
            tokenTimeouts.put(REFRESH_TOKEN_TIMEOUT, grantType.getRefreshToken());
        }
        if (grantType.getDeviceCode() > tokenTimeouts.get(DEVICE_CODE_TIMEOUT)) {
            tokenTimeouts.put(DEVICE_CODE_TIMEOUT, grantType.getDeviceCode());
        }
    }

    private void setPolicyUri(MitreidClient c, Map<String, PerunAttributeValue> attrs) {
        PerunAttributeValue attributeValue = attrs.get(perunAttrNames.getPrivacyPolicy());
        if (PerunAttributeValue.MAP_TYPE.equals(attributeValue.getType())) {
            c.setPolicyUri(attributeValue.valueAsMap().get("en"));
        } else if (PerunAttributeValue.STRING_TYPE.equals(attributeValue.getType())) {
            c.setPolicyUri(attributeValue.valueAsString());
        } else {
            c.setPolicyUri("");
        }
    }

    private void setContacts(MitreidClient c, Map<String, PerunAttributeValue> attrs) {
        Set<String> contacts = new TreeSet<>();
        for (String attr: perunAttrNames.getContacts()) {
            //in case of string attribute, it will be returned as singleton array
            if (attrs.containsKey(attr)) {
                contacts.addAll(attrs.get(attr).valueAsList());
            }
        }
        c.setContacts(contacts);
    }

    private void setClientUri(MitreidClient c, Map<String, PerunAttributeValue> attrs) {
        if (attrs == null || perunAttrNames.getHomePageUris() == null) {
            return;
        }

        for (String attr: perunAttrNames.getHomePageUris()) {
            PerunAttributeValue attributeValue = attrs.get(attr);

            if (PerunAttributeValue.MAP_TYPE.equals(attributeValue.getType()) &&
                    StringUtils.hasText(attributeValue.valueAsMap().get("en"))) {
                c.setClientUri(attributeValue.valueAsMap().get("en"));
                break;
            } else if (PerunAttributeValue.STRING_TYPE.equals(attributeValue.getType()) &&
                    StringUtils.hasText(attributeValue.valueAsString())) {
                c.setClientUri(attributeValue.valueAsString());
                break;
            }
        }
    }
}
