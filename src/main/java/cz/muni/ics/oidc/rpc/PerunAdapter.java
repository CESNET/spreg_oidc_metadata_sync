package cz.muni.ics.oidc.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import cz.muni.ics.oidc.exception.PerunConnectionException;
import cz.muni.ics.oidc.exception.PerunUnknownException;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.PerunAttribute;
import cz.muni.ics.oidc.models.PerunAttributeValue;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the adapter via Perun RPC.
 * RPC supports all the methods, thus we implement the FullAdapter interface.
 *
 * NOTE: When returning the User object, do not forget to set the login. The best way is to use private methods
 * "returnUser(...)" present in this class.
 *
 * NOTE: When returning the Facility object, do not forget to set the rpIdentifier. The best way is to use private
 * methods "returnFacility(...)" or "returnFacilityList(...)" present in this class.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
@Component
@Slf4j
public class PerunAdapter {

    // MANAGERS
    public static final String ATTRIBUTES_MANAGER = "attributesManager";
    public static final String SEARCHER = "searcher";

    // PARAMS
    public static final String PARAM_ATTR_NAMES = "attrNames";
    public static final String PARAM_ATTRIBUTES = "attributes";
    public static final String PARAM_ATTRIBUTE_NAME = "attributeName";
    private static final String PARAM_ATTRIBUTES_WITH_SEARCHING_VALUES = "attributesWithSearchingValues";

    private final PerunConnector perunConnector;

    @Autowired
    public PerunAdapter(@NonNull PerunConnector perunConnector)
    {
        this.perunConnector = perunConnector;
    }

    public Map<String, PerunAttribute> getAttributes(@NonNull Long facilityId, List<String> attrIdentifiers)
            throws PerunUnknownException, PerunConnectionException
    {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("facility", facilityId);
        params.put(PARAM_ATTR_NAMES, attrIdentifiers);

        JsonNode perunResponse = perunConnector.post(ATTRIBUTES_MANAGER, "getAttributes", params);
        return Mapper.mapAttributes(perunResponse);
    }

    public PerunAttribute getAttribute(@NonNull Long facilityId,
                                       @NonNull String attrToFetch)
            throws PerunUnknownException, PerunConnectionException
    {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("facility", facilityId);
        params.put(PARAM_ATTRIBUTE_NAME, attrToFetch);
        JsonNode perunResponse = perunConnector.post(ATTRIBUTES_MANAGER, "getAttribute", params);

        return Mapper.mapAttribute(perunResponse);
    }

    public boolean setAttributes(@NonNull Long facilityId,
                                 @NonNull List<PerunAttribute> attributes)
            throws PerunUnknownException, PerunConnectionException
    {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("facility", facilityId);
        params.put(PARAM_ATTRIBUTES, attributes);

        JsonNode perunResponse = perunConnector.post(ATTRIBUTES_MANAGER, "setAttributes", params);
        return (perunResponse == null || perunResponse.isNull() || perunResponse instanceof NullNode);
    }

    public Map<String, PerunAttributeValue> getAttributesValues(@NonNull Long facilityId,
                                                                List<String> attributes)
            throws PerunUnknownException, PerunConnectionException
    {
        Map<String, PerunAttribute> attrs = this.getAttributes(facilityId, attributes);
        return this.extractAttrValues(attrs);
    }

    public PerunAttributeValue getAttributeValue(@NonNull Long facilityId, @NonNull String attribute)
            throws PerunUnknownException, PerunConnectionException
    {
        PerunAttribute attr = this.getAttribute(facilityId, attribute);
        return this.extractAttrValue(attr);
    }

    public List<Facility> getFacilitiesByAttribute(@NonNull String attributeName, @NonNull String attrValue)
            throws PerunUnknownException, PerunConnectionException
    {
        Map<String, Object> params = new LinkedHashMap<>();
        Map<String, String> attributesWithSearchingValues = new HashMap<>();
        attributesWithSearchingValues.put(attributeName, attrValue);
        params.put(PARAM_ATTRIBUTES_WITH_SEARCHING_VALUES, attributesWithSearchingValues);

        JsonNode perunResponse = perunConnector.post(SEARCHER, "getFacilities", params);
        return Mapper.mapFacilities(perunResponse);
    }

    private Map<String, PerunAttributeValue> extractAttrValues(Map<String, PerunAttribute> attributeMap) {
        if (attributeMap == null || attributeMap.isEmpty()) {
            log.debug("Given attributeMap is {}", (attributeMap == null ? "null" : "empty"));
            return new HashMap<>();
        }

        Map<String, PerunAttributeValue> resultMap = new LinkedHashMap<>();
        attributeMap.forEach((identifier, attr) -> resultMap.put(identifier, attr == null ?
                null : attr.toPerunAttributeValue())
        );

        return resultMap;
    }

    private PerunAttributeValue extractAttrValue(PerunAttribute attribute) {
        if (attribute == null ) {
            return null;
        }
        return attribute.toPerunAttributeValue();
    }

}

