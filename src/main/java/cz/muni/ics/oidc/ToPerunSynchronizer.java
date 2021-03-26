package cz.muni.ics.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import cz.muni.ics.oidc.data.ClientRepository;
import cz.muni.ics.oidc.exception.PerunConnectionException;
import cz.muni.ics.oidc.exception.PerunUnknownException;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.MitreidClient;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import cz.muni.ics.oidc.models.SyncResult;
import cz.muni.ics.oidc.props.ActionsProperties;
import cz.muni.ics.oidc.props.AttrsMapping;
import cz.muni.ics.oidc.props.ConfProperties;
import cz.muni.ics.oidc.rpc.PerunAdapter;
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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static cz.muni.ics.oidc.Synchronizer.DO_YOU_WANT_TO_PROCEED;
import static cz.muni.ics.oidc.Synchronizer.SPACER;
import static cz.muni.ics.oidc.Synchronizer.Y;
import static cz.muni.ics.oidc.Synchronizer.YES;

@Component
@Slf4j
public class ToPerunSynchronizer {

    private final PerunAdapter perunAdapter;
    private final ConfProperties confProperties;
    private final String proxyIdentifier;
    private final AttrsMapping perunAttrNames;
    private final ClientRepository clientRepository;
    private final ActionsProperties actionsProperties;
    private final Cipher cipher;
    private final SecretKeySpec secretKeySpec;

    private final Scanner scanner = new Scanner(System.in);

    private boolean interactiveMode = false;

    @Autowired
    public ToPerunSynchronizer(@NonNull PerunAdapter perunAdapter,
                               @NonNull ConfProperties confProperties,
                               @NonNull AttrsMapping perunAttrNames,
                               @NonNull ClientRepository clientRepository,
                               @NonNull ActionsProperties actionsProperties,
                               @NonNull Cipher cipher,
                               @NonNull SecretKeySpec secretKeySpec)
    {
        this.perunAdapter = perunAdapter;
        this.confProperties = confProperties;
        this.perunAttrNames = perunAttrNames;
        this.clientRepository = clientRepository;
        this.actionsProperties = actionsProperties;
        this.cipher = cipher;
        this.secretKeySpec = secretKeySpec;
        this.proxyIdentifier = perunAttrNames.getProxyIdentifier();
    }

    public SyncResult syncToPerun(boolean interactive) {
        this.interactiveMode = interactive;
        SyncResult syncResult = new SyncResult();
        Map<String, Facility> presentFacilities = fillPresentFacilities();
        if (presentFacilities == null || presentFacilities.isEmpty()) {
            return syncResult;
        }

        List<MitreidClient> mitreidClients = clientRepository.getAll();
        for (MitreidClient client: mitreidClients) {
            processClient(client, presentFacilities, syncResult);
        }
        deleteFacilitiesWithoutClients(presentFacilities, syncResult);
        return syncResult;
    }

    private void processClient(MitreidClient client, Map<String, Facility> presentFacilities, SyncResult res) {
        if (client == null) {
            log.warn("NULL client given, generating error and continue on processing");
            res.incErrors();
            return;
        }

        String clientId = client.getClientId();
        log.debug("Processing client '{}'", clientId);
        if (!StringUtils.hasText(clientId)) {
            log.debug("ClientID is null, skip to next client for client.id '{}'", client.getId());
            presentFacilities.remove(clientId);
            return;
        } else if (actionsProperties.getProtectedClientIds().contains(clientId)) {
            presentFacilities.remove(clientId);
            log.debug("ClientID '{}' is marked as protected in configuration, skip it.", clientId);
            return;
        }
        try {
            if (presentFacilities.containsKey(clientId)) {
                Facility f = presentFacilities.get(clientId);
                log.debug("Updating facility '{}' for client '{}({})'", f, client.getClientName(), clientId);
                updateFacility(client, f, res);
            } else {
                log.debug("No facility found for client '{}({})', trying to fetch it once more",
                        client.getClientName(), clientId);
                List<Facility> withClientId = perunAdapter.getFacilitiesByAttribute(
                        perunAttrNames.getClientId(), clientId);
                if (withClientId != null && withClientId.size() == 1) {
                    Facility f = withClientId.get(0);
                    log.debug("Repeated search successful. Found facility '{}' for client '{}({})'",
                            f, client.getClientName(), clientId);
                    updateFacility(client, f, res);
                } else if (withClientId != null && withClientId.size() > 1) {
                    log.warn("Multiple facilities found for client '{}({})': IDS in perun '{}'",
                            client.getClientName(), clientId, withClientId);
                } else {
                    log.debug("Create facility for client '{}'", clientId);
                    createFacility(client, res);
                }
            }
        } catch (Exception e) {
            log.warn("Error when processing client '{}'", client.getClientId(), e);
            res.incErrors();
        }
        presentFacilities.remove(clientId);
    }

    private void deleteFacilitiesWithoutClients(Map<String, Facility> presentFacilities, SyncResult syncResult) {
        if (actionsProperties.getToPerun().isDelete()) {
            for (Map.Entry<String, Facility> entry: presentFacilities.entrySet()) {
                Facility f = entry.getValue();
                log.info("Deleting facility '{}' as no client has been found for it", f);
                if (f == null) {
                    log.warn("Null facility given for ClientID '{}', skipping to the next one", entry.getKey());
                    continue;
                }
                if (interactiveMode) {
                    System.out.println("About to remove following facility");
                    System.out.println(f);
                    System.out.println(DO_YOU_WANT_TO_PROCEED);
                    String response = scanner.nextLine();
                    if (!Y.equalsIgnoreCase(response) && !YES.equalsIgnoreCase(response)) {
                        continue;
                    }
                }
                try {
                    if (deleteFacility(f)) {
                        syncResult.incDeleted();
                    } else {
                        syncResult.incErrors();
                    }
                } catch (Exception e) {
                    log.warn("Caught exception '{}' when deleting facility '{}' for ClientID '{}'",
                            e.getClass().getSimpleName(), f, entry.getKey(), e);
                    syncResult.incErrors();
                }
            }
        } else {
            log.info("Delete facility disabled, skipping clients {}", presentFacilities.keySet());
        }
    }

    private boolean deleteFacility(Facility f) {
        try {
            log.debug("Deleting managers group");
            PerunAttributeValue managersGroupId = perunAdapter.getAttributeValue(f.getId(),
                    perunAttrNames.getManagersGroupId());
            if (perunAdapter.deleteGroup(managersGroupId.valueAsInteger().longValue())) {
                log.debug("Deleted group for managers");
            } else {
                log.debug("Failed to delete group with managers for facility {}", f);
            }
            log.debug("Deleting facility");
            if (perunAdapter.deleteFacility(f)) {
                log.info("Deleted facility '{}'", f);
                return true;
            } else {
                log.warn("Did not delete facility '{}'", f);
                return false;
            }
        } catch (Exception e) {
            log.warn("Failed deleting facility '{}'", f, e);
            return false;
        }
    }

    private void updateFacility(MitreidClient client, Facility facility, SyncResult syncResult) {
        try {
            if (!actionsProperties.getToPerun().isUpdate()) {
                log.info("Update facility disabled, skipping client '{}({})'",
                        client.getClientName(), client.getClientId());
                return;
            }

            final Map<String, PerunAttribute> attrs = perunAdapter.getAttributes(
                    facility.getId(), perunAttrNames.getNames());
            if (interactiveMode) {
                Map<String, PerunAttribute> newAttrs = perunAdapter.getAttributes(
                        facility.getId(), perunAttrNames.getNames());
                updateFacilityAttrValues(client, newAttrs);
                final List<PerunAttributeValue> oldList = attrs.values().stream()
                        .map(PerunAttribute::toPerunAttributeValue)
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(PerunAttributeValue::getAttrName))
                        .collect(Collectors.toList());
                final List<PerunAttributeValue> newList = newAttrs.values().stream()
                        .map(PerunAttribute::toPerunAttributeValue)
                        .filter(Objects::nonNull)
                        .sorted(Comparator.comparing(PerunAttributeValue::getAttrName))
                        .collect(Collectors.toList());
                String diff = checkUpdateChanges(oldList, newList);
                if (StringUtils.hasText(diff)) {
                    System.out.println(diff);
                    System.out.println(DO_YOU_WANT_TO_PROCEED);
                    String response = scanner.nextLine();
                    System.out.println(SPACER);
                    if (!Y.equalsIgnoreCase(response) && !YES.equalsIgnoreCase(response)) {
                        return;
                    }
                }
            }
            updateFacilityAttrValues(client, attrs);
            if (perunAdapter.setAttributes(facility.getId(), new ArrayList<>(attrs.values()))) {
                log.info("Updated facility for client '{}({})'", client.getClientName(), client.getClientId());
                syncResult.incUpdated();
                updateFindOrCreateManagersGroup(facility, client);
            } else {
                log.info("Updating facility for client '{}({})' has failed",
                        client.getClientName(), client.getClientId());
                syncResult.incErrors();
            }
        } catch (Exception e) {
            log.warn("Error when processing (update facility '{}' and managers group) client '{}({})'",
                    facility, client.getClientName(), client.getClientId(), e);
            syncResult.incErrors();
        }
    }

    private String checkUpdateChanges(List<PerunAttributeValue> oldList, List<PerunAttributeValue> newList) {
        boolean changed = false;
        StringBuilder diff = new StringBuilder(SPACER);
        for (int i = 0; i < oldList.size(); i++) {
            PerunAttributeValue oldAttr = oldList.get(i);
            PerunAttributeValue newAttr = newList.get(i);
            if (!Objects.equals(oldAttr.getValue(), newAttr.getValue())) {
                changed = true;
                JsonNode oldValue = oldAttr.getValue();
                JsonNode newValue = newAttr.getValue();
                diff.append(String.format("Changes in attribute '%s'\n", oldAttr.getAttrName()));
                diff.append(String.format("  original: '%s'\n", oldValue));
                diff.append(String.format("  updated: '%s'\n", newValue));
                diff.append("  diff:");
                if (oldValue.isNull()) {
                    diff.append(String.format("    added: '%s'\n", newValue));
                } else if (newValue.isNull()) {
                    diff.append(String.format("    removed: '%s'\n", oldValue));
                } else if (oldValue.isValueNode()) {
                    diff.append(String.format("    changed: '%s' to: '%s'\n", oldValue, newValue));
                } else if (oldValue.isArray()){
                    String res = compareLists(oldValue, newValue);
                    if (StringUtils.hasText(res)) {
                        diff.append(res);
                    }
                } else {
                    String res = compareMaps(oldValue, newValue);
                    if (StringUtils.hasText(res)) {
                        diff.append(res);
                    }
                }
            }
        }
        if (changed) {
            return diff.toString();
        }
        return null;
    }

    private String compareMaps(JsonNode oldValue, JsonNode newValue) {
        Map<String, String> oldVals = new HashMap<>();
        Map<String, String> newVals = new HashMap<>();
        Iterator<String> oldValueIt = oldValue.fieldNames();
        Iterator<String> newValueIt = newValue.fieldNames();
        while (oldValueIt.hasNext()) {
            String key = oldValueIt.next();
            oldVals.put(key, oldValue.get(key).asText());
        }
        while (newValueIt.hasNext()) {
            String key = newValueIt.next();
            newVals.put(key, newValue.get(key).asText());
        }

        StringBuilder diff = new StringBuilder();
        Set<String> allKeys = oldVals.keySet();
        allKeys.addAll(newVals.keySet());
        for (String key: allKeys) {
            String oldSubValue = oldVals.getOrDefault(key, null);
            String newSubValue = newVals.getOrDefault(key, null);
            if (oldSubValue == null) {
                diff.append(String.format("    added: '%s' => '%s'\n", key, newSubValue));
            } else if (newSubValue == null) {
                diff.append(String.format("    removed: '%s' => '%s'\n", key, oldSubValue));
            } else if (!oldSubValue.equals(newSubValue)){
                diff.append(String.format("    changed: '%s' => '%s' TO: '%s' => '%s'\n",
                        key, oldSubValue, key, newSubValue));
            }
        }
        return diff.toString();
    }

    private String compareLists(JsonNode oldValue, JsonNode newValue) {
        List<String> oldValList = new ArrayList<>();
        List<String> newValList = new ArrayList<>();
        for (JsonNode val: oldValue) {
            if (!val.isNull()) oldValList.add(val.asText());
        }
        for (JsonNode val: newValue) {
            if (!val.isNull()) newValList.add(val.asText());
        }
        Collections.sort(oldValList);
        Collections.sort(newValList);
        StringBuilder diff = new StringBuilder();
        for (int o = 0, n = 0; o < oldValList.size() || n < newValList.size(); ) {
            String oldSubValue = getOrNull(oldValList, o);
            String newSubValue = getOrNull(newValList, n);
            if (oldSubValue == null) {
                // old is at the end, move to the next one in new
                n++;
                diff.append(String.format("    added: '%s'\n", newSubValue));
            } else if (newSubValue == null) {
                // new is at the end, move to the next one in old
                o++;
                diff.append(String.format("    removed: '%s'\n", oldSubValue));
            } else if (oldSubValue.equals(newSubValue)){
                // values are equal, continue with moving in both lists at the same time
                o++; n++;
            } else {
                int direction = oldSubValue.compareTo(newSubValue);
                if (direction < 0) {
                    // old value is before the new, move in the old list
                    o++;
                    diff.append(String.format("    removed: '%s'\n", oldSubValue));
                } else {
                    // new value is before the old, move in the new list
                    n++;
                    diff.append(String.format("    added: '%s'\n", newSubValue));
                }
            }
        }
        return diff.toString();
    }

    private String getOrNull(List<String> list, int index) {
        if (index < list.size()) {
            return list.get(index);
        } else {
            return null;
        }
    }

    private void updateFindOrCreateManagersGroup(Facility facility, MitreidClient client)
            throws PerunUnknownException, PerunConnectionException
    {
        PerunAttribute managersGroupId = perunAdapter.getAttribute(facility.getId(),
                perunAttrNames.getManagersGroupId());
        if (managersGroupId.valueAsInteger() == null) {
            Group foundG = null;
            try {
                foundG = perunAdapter.getGroupByName(confProperties.getManagersGroupVoId(),
                        confProperties.getManagersGroupParentGroupName() + ':' + facility.getName());
                if (foundG == null) {
                    foundG = perunAdapter.getGroupByName(confProperties.getManagersGroupVoId(),
                            confProperties.getManagersGroupParentGroupName() + ':'
                                    + normalizeClientName(client.getClientName()));
                }
            } catch (Exception ignored) {
                //OKAY
            }
            Long groupId;
            if (foundG == null) {
                groupId = createManagersGroup(facility);
            } else {
                groupId = foundG.getId();
                try {
                    perunAdapter.addGroupAsAdmins(facility.getId(), groupId);
                } catch (Exception ignored) {
                    //OKAY
                }
            }
            managersGroupId.setValue(managersGroupId.getType(), getNumericNode(groupId.intValue()));
            perunAdapter.setAttributes(facility.getId(), Collections.singletonList(managersGroupId));
        }
    }

    private Map<String, Facility> fillPresentFacilities() {
        log.info("Fetching facilities from Perun");
        try {
            Map<String, Facility> presentFacilities = new HashMap<>();
            Set<Facility> facilities = new HashSet<>(perunAdapter.getFacilitiesByAttribute(
                    proxyIdentifier, confProperties.getProxyIdentifierValue()));
            for (Facility f : facilities) {
                PerunAttribute clientId = perunAdapter.getAttribute(f.getId(), perunAttrNames.getClientId());
                if (clientId == null || !StringUtils.hasText(clientId.valueAsString())) {
                    continue;
                }
                presentFacilities.put(clientId.valueAsString(), f);
            }
            return presentFacilities;
        } catch (PerunConnectionException | PerunUnknownException e) {
            log.error("Caught exception when fetching facilities by attr '{}' with value '{}'",
                    proxyIdentifier, confProperties.getProxyIdentifierValue(), e);
            return null;
        }
    }

    private void createFacility(MitreidClient client, SyncResult syncResult)
    {
        if (!actionsProperties.getToPerun().isCreate()) {
            log.info("Create facility disabled, skipping client '{}({})'", client.getClientName(), client.getClientId());
            return;
        }
        try {
            log.debug("Creating facility");
            String name = normalizeClientName(client.getClientName());
            Facility f = perunAdapter.createFacility(name, client.getClientId());
            if (f == null) {
                log.warn("Failed creating facility for client '{}({})'", client.getClientName(), client.getClientId());
                return;
            }
            final Map<String, PerunAttribute> attrs = perunAdapter.getAttributes(
                    f.getId(), perunAttrNames.getNames());
            updateFacilityAttrValues(client, attrs, true);
            if (interactiveMode) {
                System.out.println("A new facility will be created with following attributes");
                System.out.println(attrs.values().stream()
                        .map(PerunAttribute::toPerunAttributeValue)
                        .map(v -> v.getAttrName() + " - " + v.getValue())
                        .collect(Collectors.joining("\n"))
                );
                System.out.println(DO_YOU_WANT_TO_PROCEED);
                String response = scanner.nextLine();
                if (!Y.equalsIgnoreCase(response) && !YES.equalsIgnoreCase(response)) {
                    perunAdapter.deleteFacility(f);
                    return;
                }
            }
            if (perunAdapter.setAttributes(f.getId(), new ArrayList<>(attrs.values()))) {
                log.info("Created facility '{}' for client '{}({})'", f, client.getClientName(), client.getClientId());
                syncResult.incCreated();
                log.debug("Creating managers group for facility '{}'", f);
                updateFindOrCreateManagersGroup(f, client);
            } else {
                log.info("Did not create facility for facility '{}'", f);
                syncResult.incErrors();
            }
        } catch (Exception e) {
            log.warn("Error when processing (create facility and managers group) client '{}({})'",
                    client.getClientName(), client.getClientId(), e);
            syncResult.incErrors();
        }
    }

    private Long createManagersGroup(Facility f) throws PerunUnknownException, PerunConnectionException {
        log.debug("Creating admins group");
        String perunFacilityName = normalizeClientName(f.getName());
        Group adminsGroup = new Group(perunFacilityName, perunFacilityName, perunFacilityName,
            confProperties.getManagersGroupParentGroupId(),
            confProperties.getManagersGroupVoId());
        adminsGroup = perunAdapter.createGroup(adminsGroup.getParentGroupId(), adminsGroup);
        log.debug("Add group as manager");
        perunAdapter.addGroupAsAdmins(f.getId(), adminsGroup.getId());
        return adminsGroup.getId();
    }

    private String normalizeClientName(String name) {
        Pattern pattern = Pattern.compile("[^A-Za-z0-9]");
        Pattern pattern2 = Pattern.compile("_+_");

        name = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        name = pattern.matcher(name).replaceAll("_");
        name = pattern2.matcher(name).replaceAll("_");
        return name;
    }

    private void updateFacilityAttrValues(MitreidClient client,
                                                                 Map<String, PerunAttribute> attributeMap)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        updateFacilityAttrValues(client, attributeMap, false);
    }

    private void updateFacilityAttrValues(MitreidClient client,
                                                                 Map<String, PerunAttribute> attributeMap,
                                                                 boolean setIsTestSp)
            throws BadPaddingException, InvalidKeyException, IllegalBlockSizeException
    {
        setNewAttrValue(attributeMap, getTextNode(client.getClientId()), perunAttrNames.getClientId());
        if (StringUtils.hasText(client.getClientSecret())) {
            setNewAttrValue(attributeMap, getTextNode(Utils.encrypt(client.getClientSecret(), cipher, secretKeySpec)),
                    perunAttrNames.getClientSecret());
        }
        setNewAttrValue(attributeMap, getLocalizedObjectNode(client.getClientName()), perunAttrNames.getName());
        if (StringUtils.hasText(client.getClientDescription())) {
            setNewAttrValue(attributeMap, getLocalizedObjectNode(client.getClientDescription()), perunAttrNames.getDescription());
        } else {
            setNewAttrValue(attributeMap, getLocalizedObjectNode(client.getClientName()), perunAttrNames.getDescription());
        }

        setNewAttrValue(attributeMap, getTextNode(client.getPolicyUri()), perunAttrNames.getPrivacyPolicy());
        setNewAttrValue(attributeMap, getTextNode(new ArrayList<>(client.getContacts()).get(0)), perunAttrNames.getContacts().get(0));
        setNewAttrValue(attributeMap, getArrayNode(client.getScope()), perunAttrNames.getScopes());
        setNewAttrValue(attributeMap, getArrayNode(client.getRedirectUris()), perunAttrNames.getRedirectUris());
        setNewAttrValue(attributeMap, getArrayNode(client.getGrantTypes()), perunAttrNames.getGrantTypes());
        setNewAttrValue(attributeMap, getArrayNode(client.getResponseTypes()), perunAttrNames.getResponseTypes());
        setNewAttrValue(attributeMap, getBooleanNode(client.isReuseRefreshToken()), perunAttrNames.getIssueRefreshTokens());
        setNewAttrValue(attributeMap, getBooleanNode(client.isAllowIntrospection()), perunAttrNames.getIntrospection());
        setNewAttrValue(attributeMap, getArrayNode(client.getPostLogoutRedirectUris()), perunAttrNames.getPostLogoutRedirectUris());
        setNewAttrValue(attributeMap, getTextNode(confProperties.getProxyIdentifierValue()), perunAttrNames.getMasterProxyIdentifier());
        setNewAttrValue(attributeMap, getArrayNode(Collections.singletonList(confProperties.getProxyIdentifierValue())), perunAttrNames.getProxyIdentifier());
        if (setIsTestSp) {
            setNewAttrValue(attributeMap, getBooleanNode(true), perunAttrNames.getIsTestSp());
        }
        setNewAttrValue(attributeMap, getBooleanNode(true), perunAttrNames.getIsOidc());
    }

    private ObjectNode getLocalizedObjectNode(String val) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        for (String lang: confProperties.getLangs()) {
            node.put(lang, val);
        }
        return node;
    }

    private void setNewAttrValue(Map<String, PerunAttribute> attrs, JsonNode newValue, String attrName) {
        PerunAttribute attr = attrs.get(attrName);
        attr.setValue(attr.getType(), newValue);
        attrs.replace(attrName, attr);
    }

    private TextNode getTextNode(String val) {
        return JsonNodeFactory.instance.textNode(val);
    }

    private BooleanNode getBooleanNode(boolean val) {
        return JsonNodeFactory.instance.booleanNode(val);
    }

    private NumericNode getNumericNode(int val) {
        return JsonNodeFactory.instance.numberNode(val);
    }

    private ArrayNode getArrayNode(Collection<String> vals) {
        ArrayNode node = JsonNodeFactory.instance.arrayNode();
        for (String v: vals) {
            node.add(v);
        }
        return node;
    }

}
