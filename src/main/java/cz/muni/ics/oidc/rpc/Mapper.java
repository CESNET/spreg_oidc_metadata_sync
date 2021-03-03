package cz.muni.ics.oidc.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import cz.muni.ics.oidc.models.Facility;
import cz.muni.ics.oidc.models.Group;
import cz.muni.ics.oidc.models.PerunAttribute;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class is mapping JsonNodes to object models.
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class Mapper {

    /**
     * Maps JsonNode to Facility model.
     * @param json Facility in JSON format from Perun to be mapped.
     * @return Mapped Facility object.
     */
    public static Facility mapFacility(@NonNull JsonNode json) {
        if (json.isNull()) {
            return null;
        }

        Long id = json.get("id").asLong();
        String name = json.get("name").asText();
        String description = json.get("description").asText();

        return new Facility(id, name, description);
    }

    /**
     * Maps JsonNode to List of Facilities.
     * @param jsonArray JSON array of facilities in JSON format from Perun to be mapped.
     * @return List of facilities.
     */
    public static List<Facility> mapFacilities(@NonNull JsonNode jsonArray) {
        if (jsonArray.isNull()) {
            return new ArrayList<>();
        }

        List<Facility> result = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonNode facilityNode = jsonArray.get(i);
            Facility mappedFacility = Mapper.mapFacility(facilityNode);
            result.add(mappedFacility);
        }

        return result;
    }

    /**
     * Maps JsonNode to PerunAttribute model.
     * @param json PerunAttribute in JSON format from Perun to be mapped.
     * @return Mapped PerunAttribute object.
     */
    public static PerunAttribute mapAttribute(@NonNull JsonNode json) {
        if (json.isNull()) {
            return null;
        }

        Long id = json.get("id").asLong();
        String friendlyName = json.get("friendlyName").asText();
        String namespace = json.get("namespace").asText();
        String description = json.get("description").asText();
        String type = json.get("type").asText();
        String displayName = json.get("displayName").asText();
        boolean writable = json.get("writable").asBoolean();
        boolean unique = json.get("unique").asBoolean();
        String entity = json.get("entity").asText();
        String baseFriendlyName = json.get("baseFriendlyName").asText();
        String friendlyNameParameter = json.get("friendlyNameParameter").asText();
        JsonNode value = json.get("value");

        return new PerunAttribute(id, friendlyName, namespace, description, type, displayName,
                writable, unique, entity, baseFriendlyName, friendlyNameParameter, value);
    }

    /**
     * Maps JsonNode to Map<String, PerunAttribute>.
     * Keys are the internal identifiers of the attributes.
     * Values are attributes corresponding to the names.
     * @param jsonArray JSON array of perunAttributes in JSON format from Perun to be mapped.
     * @return Map<String, PerunAttribute>. If attribute for identifier has not been mapped, key contains NULL as value.
     */
    public static Map<String, PerunAttribute> mapAttributes(@NonNull JsonNode jsonArray) {
        if (jsonArray.isNull()) {
            return new HashMap<>();
        }

        Map<String, PerunAttribute> mappedAttrs = new HashMap<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonNode attribute = jsonArray.get(i);
            PerunAttribute mappedAttribute = Mapper.mapAttribute(attribute);

            if (mappedAttribute != null) {
                mappedAttrs.put(mappedAttribute.getUrn(), mappedAttribute);
            }
        }

        return mappedAttrs;
    }

    public static Group mapGroup(@NonNull JsonNode json) {
        if (json.isNull()) {
            return null;
        }

        Long id = json.get("id").asLong();
        String shortName = json.get("shortName").asText();
        String name = json.get("name").asText();
        String description = json.get("description").asText();
        Long parentGroupId = null;
        if (json.hasNonNull("parentGroupId")) {
            json.get("parentGroupId").asLong();
        }
        Long voId = json.get("voId").asLong();

        return new Group(id, name, shortName, description, parentGroupId, voId);
    }

}

