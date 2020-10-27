package cz.muni.ics.oidc.enums;

/**
 * Represents type of Value in Attribute from Perun.
 */
public enum AttributeType {
    STRING,
    LARGE_STRING,
    INTEGER,
    BOOLEAN,
    ARRAY,
    LARGE_ARRAY,
    MAP_JSON,
    MAP_KEY_VALUE;

    public static AttributeType parse(String str){
        if (str == null) {
            return STRING;
        }

        switch (str.toLowerCase()) {
            case "integer": return INTEGER;
            case "boolean": return BOOLEAN;
            case "array":
            case "list": return ARRAY;
            case "map_json": return MAP_JSON;
            case "map_key_value": return MAP_KEY_VALUE;
            default: return STRING;
        }
    }
}
