package in.ashwanthkumar.gocd.github.util;

import java.util.HashMap;
import java.util.Map;

public class FieldFactory {

    public static final String CHECKBOX_TRUE_VALUE = "on";

    /**
     * Create plugin setting field for SCM view
     * @param displayName
     * @param defaultValue
     * @param isPartOfIdentity
     * @param isRequired
     * @param isSecure
     * @param displayOrder
     * @return
     */
    public static Map<String, Object> createForScm(String displayName, String defaultValue, boolean isPartOfIdentity, boolean isRequired, boolean isSecure, String displayOrder) {
        Map<String, Object> fieldProperties = new HashMap<String, Object>();
        fieldProperties.put("display-name", displayName);
        fieldProperties.put("default-value", defaultValue);
        fieldProperties.put("part-of-identity", isPartOfIdentity);
        fieldProperties.put("required", isRequired);
        fieldProperties.put("secure", isSecure);
        fieldProperties.put("display-order", displayOrder);
        return fieldProperties;
    }

    /**
     * Create plugin setting field for plugin general settings view
     * @param displayName
     * @param defaultValue
     * @param isRequired
     * @param isSecure
     * @param displayOrder
     * @return
     */
    public static Map<String, Object> createForGeneral(String displayName, String defaultValue, boolean isRequired, boolean isSecure, String displayOrder) {
        Map<String, Object> fieldProperties = new HashMap<String, Object>();
        fieldProperties.put("display-name", displayName);
        fieldProperties.put("default-value", defaultValue);
        fieldProperties.put("required", isRequired);
        fieldProperties.put("secure", isSecure);
        fieldProperties.put("display-order", displayOrder);
        return fieldProperties;
    }

}
