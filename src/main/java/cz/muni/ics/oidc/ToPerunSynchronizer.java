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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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

    public SyncResult syncToPerun() {
        SyncResult syncResult = new SyncResult();
        Map<String, Facility> presentFacilities = fillPresentFacilities();
        if (presentFacilities == null) {
            return syncResult;
        }

        List<MitreidClient> mitreidClients = clientRepository.getAll();
        for (MitreidClient client: mitreidClients) {
            syncClient(client, presentFacilities, syncResult);
        }
        if (actionsProperties.getToPerun().isDelete()) {
            for (Facility f: presentFacilities.values()) {
                if (f != null && !deleteFacility(f)) {
                    syncResult.incDeleted();
                }
            }
        } else {
            log.info("Delete facility disabled, skipping clients {}", presentFacilities.keySet());
        }
        return syncResult;
    }

    private void syncClient(MitreidClient client, Map<String, Facility> presentFacilities, SyncResult syncResult) {
        String clientId = client.getClientId();
        if (actionsProperties.getProtectedClientIds().contains(clientId)) {
            presentFacilities.replace(clientId, null);
            presentFacilities.remove(clientId);
            return;
        }
        try {
            if (presentFacilities.containsKey(clientId)) {
                handleUpdateFacility(client, presentFacilities.get(clientId), syncResult);
            } else {
                List<Facility> withClientId = perunAdapter.getFacilitiesByAttribute(perunAttrNames.getClientId(), clientId);
                if (withClientId != null && withClientId.size() == 1) {
                    handleUpdateFacility(client, withClientId.get(0), syncResult);
                } else {
                    handleCreateFacility(client, presentFacilities, syncResult);
                }
            }
        } catch (Exception e) {
            syncResult.incErrors();
            log.warn("Error when processing client {}", client.getClientId(), e);
        }
        if (presentFacilities.containsKey(clientId)) {
            presentFacilities.replace(clientId, null);
            presentFacilities.remove(clientId);
        }
    }

    private void handleCreateFacility(MitreidClient client, Map<String, Facility> presentFacilities,
                                      SyncResult syncResult)
    {
        if (!actionsProperties.getToOidc().isCreate()) {
            log.info("Create facility disabled, skipping client {}", client.getClientId());
            return;
        }
        if (createFacility(client)) {
            log.info("Created facility for client {}", client.getClientId());
            syncResult.incCreated();
            presentFacilities.remove(client.getClientId());
        } else {
            syncResult.incErrors();
            log.info("Did not create facility for client {}", client.getClientId());
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
                log.debug("Failed to delete group with managers for facility {}", f.getId());
            }
            log.debug("Deleting facility");
            if (perunAdapter.deleteFacility(f)) {
                log.info("Deleted facility {}", f.getId());
            } else {
                log.warn("Did not delete facility {}", f.getId());
                return false;
            }
        } catch (Exception e) {
            log.warn("Failed deleting facility {}", f.getId(), e);
            return false;
        }
        return true;
    }

    private void handleUpdateFacility(MitreidClient client, Facility facility, SyncResult syncResult) {
        try {
            if (!actionsProperties.getToPerun().isUpdate()) {
                log.info("Update facility disabled, skipping client {}", client.getClientId());
                return;
            }
            if (setFacilityAttributes(client, facility, false)) {
                log.info("Updated facility for client {}", client.getClientId());
                syncResult.incUpdated();
            } else {
                log.info("Did not update facility for client {}", client.getClientId());
                syncResult.incErrors();
            }
            updateFindOrCreateManagersGroup(facility, client);
        } catch (Exception e) {
            syncResult.incErrors();
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
                groupId = createAdminsGroup(facility, client.getClientName());
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
        try {
            Map<String, Facility> presentFacilities = new HashMap<>();
            Set<Facility> facilities = new HashSet<>(perunAdapter.getFacilitiesByAttribute(
                    proxyIdentifier, confProperties.getProxyIdentifierValue()));
            for (Facility f : facilities) {
                if (f.getId() == null) {
                    continue;
                }
                PerunAttribute clientId = perunAdapter.getAttribute(f.getId(), perunAttrNames.getClientId());
                if (clientId == null || !StringUtils.hasText(clientId.valueAsString())) {
                    continue;
                }
                presentFacilities.put(clientId.valueAsString(), f);
            }
            return presentFacilities;
        } catch (PerunConnectionException | PerunUnknownException e) {
            log.error("Caught exception when fetching facilities by attr {} with value {}",
                    proxyIdentifier, confProperties.getProxyIdentifierValue(), e);
            return null;
        }
    }

    private boolean createFacility(MitreidClient client) {
        try {
            log.debug("Creating facility");
            String name = normalizeClientName(client.getClientName());
            Facility f = perunAdapter.createFacility(name, client.getClientId());
            if (f == null || f.getId() == null) {
                log.warn("Failed creating facility for client {}", client.getClientId());
                return false;
            }
            if (!setFacilityAttributes(client, f, true)) {
                return false;
            }
            Long managerGroupId = createAdminsGroup(f, name);
            log.debug("Setting admins group in attr");
            PerunAttribute managersGroupIdAttr = perunAdapter.getAttribute(f.getId(), perunAttrNames.getManagersGroupId());
            managersGroupIdAttr.setValue(managersGroupIdAttr.getType(), JsonNodeFactory.instance.numberNode(managerGroupId));
            perunAdapter.setAttributes(f.getId(), Collections.singletonList(managersGroupIdAttr));
        } catch (Exception e) {
            log.warn("Failed creating facility for client {}", client.getClientId(), e);
            return false;
        }
        return true;
    }

    private String normalizeClientName(String name) {
        Pattern pattern = Pattern.compile("[^A-Za-z0-9]");
        Pattern pattern2 = Pattern.compile("_+_");

        name = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        name = pattern.matcher(name).replaceAll("_");
        name = pattern2.matcher(name).replaceAll("_");
        return name;
    }

    private Long createAdminsGroup(Facility f, String perunFacilityName)
            throws PerunUnknownException, PerunConnectionException
    {
        log.debug("Creating admins group");
        perunFacilityName = normalizeClientName(perunFacilityName);
        Group adminsGroup = new Group(perunFacilityName, perunFacilityName, perunFacilityName,
                confProperties.getManagersGroupParentGroupId(),
                confProperties.getManagersGroupVoId());
        adminsGroup = perunAdapter.createGroup(adminsGroup.getParentGroupId(), adminsGroup);
        log.debug("Add group as manager");
        perunAdapter.addGroupAsAdmins(f.getId(), adminsGroup.getId());
        return adminsGroup.getId();
    }

    private boolean setFacilityAttributes(MitreidClient client, Facility f, boolean setIsTestSp)
            throws PerunUnknownException, PerunConnectionException, BadPaddingException,
            InvalidKeyException, IllegalBlockSizeException
    {
        log.debug("Setting facility attributes");
        Map<String, PerunAttribute> attributeMap = perunAdapter.getAttributes(f.getId(), perunAttrNames.getNames());
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
        perunAdapter.setAttributes(f.getId(), new ArrayList<>(attributeMap.values()));
        return true;
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
