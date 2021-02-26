package cz.muni.ics.oidc;

import cz.muni.ics.oidc.data.ClientRepository;
import cz.muni.ics.oidc.exception.PerunConnectionException;
import cz.muni.ics.oidc.exception.PerunUnknownException;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.MitreidClient;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.models.ResultCounter;
import cz.muni.ics.oidc.models.SyncResult;
import cz.muni.ics.oidc.props.ActionsProperties;
import cz.muni.ics.oidc.props.AttrsMapping;
import cz.muni.ics.oidc.props.ConfProperties;
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
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

@Component
@Slf4j
public class ToOidcSynchronizer {

    private static final String YES = "yes";
    private static final String Y = "y";
    private static final String DO_YOU_WANT_TO_PROCEED = "Do you want to proceed? (YES/no)";

    private final PerunAdapter perunAdapter;
    private final String proxyIdentifier;
    private final String proxyIdentifierValue;
    private final AttrsMapping perunAttrNames;
    private final ClientRepository clientRepository;
    private final ActionsProperties actionsProperties;
    private final Cipher cipher;
    private final SecretKeySpec secretKeySpec;

    private final Scanner scanner = new Scanner(System.in);

    private boolean interactiveMode = false;

    @Autowired
    public ToOidcSynchronizer(@NonNull PerunAdapter perunAdapter,
                              @NonNull ConfProperties confProperties,
                              @NonNull AttrsMapping perunAttrNames,
                              @NonNull ClientRepository clientRepository,
                              @NonNull ActionsProperties actionsProperties,
                              @NonNull Cipher cipher,
                              @NonNull SecretKeySpec secretKeySpec)
    {
        this.perunAdapter = perunAdapter;
        this.perunAttrNames = perunAttrNames;
        this.clientRepository = clientRepository;
        this.actionsProperties = actionsProperties;
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
        log.info("Removing old clients");
        deleteClients(foundClientIds, res);
        return res;
    }

    private void processFacility(Facility f, Set<String> foundClientIds, SyncResult res) {
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

    private void updateClient(MitreidClient c, Map<String, PerunAttributeValue> attrs, SyncResult res)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        if (actionsProperties.getToOidc().isUpdate()) {
            MitreidClient toUpdate;
            if (interactiveMode) {
                MitreidClient updated = clientRepository.getClientByClientId(c.getClientId());
                this.setClientFields(updated, attrs);
                DiffNode diff = ObjectDifferBuilder.buildDefault().compare(c, updated);
                if (diff.hasChanges()) {
                    System.out.println("--------------------------------------------");
                    diff.visit((node, visit) -> {
                        if (node.isRootNode()) {
                            return;
                        }
                        Object baseValue = node.canonicalGet(c);
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
                    });
                    System.out.println(DO_YOU_WANT_TO_PROCEED);
                    String response = scanner.nextLine();
                    if (!Y.equalsIgnoreCase(response) && !YES.equalsIgnoreCase(response)) {
                        return;
                    } else {
                        System.out.println("--------------------------------------------");
                    }
                }
                toUpdate = updated;
                clientRepository.updateClient(c.getId(), updated);
            } else {
                this.setClientFields(c, attrs);
                toUpdate = c;
            }
            clientRepository.updateClient(c.getId(), toUpdate);
            log.debug("Client updated");
            res.incUpdated();
        } else {
            log.warn("Updating clients is disabled, skip update");
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
        c.setPolicyUri(attrs.get(perunAttrNames.getPrivacyPolicy()).valueAsString());
        setContacts(c, attrs);
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

    private void setContacts(MitreidClient c, Map<String, PerunAttributeValue> attrs) {
        Set<String> contacts = new HashSet<>();
        for (String attr: perunAttrNames.getContacts()) {
            //in case of string attribute, it will be returned as singleton array
            if (attrs.containsKey(attr)) {
                contacts.addAll(attrs.get(attr).valueAsList());
            }
        }
        contacts.remove(null); // just to be sure
        c.setContacts(contacts);
    }


}
