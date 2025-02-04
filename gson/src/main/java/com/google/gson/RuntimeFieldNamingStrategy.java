package com.google.gson;

import java.lang.reflect.Field;
import java.util.Map;

import com.google.gson.annotations.RuntimeSerializedName;

/**
 * Naming strategy that uses the annotation {@link RuntimeSerializedName}
 * If the annotation exists, it attempts to get a real serialized name from the source field mappings.
 * An {@link IllegalArgumentException} is thrown if no mapping exists.
 * A default naming strategy is used if no annotation is present.
 * The default for this is {@link FieldNamingPolicy#IDENTITY}
 *
 * @author Damien Biggs
 */
public class RuntimeFieldNamingStrategy implements FieldNamingStrategy {

    private Map<String, String> runtimeFieldNameMappings;

    private FieldNamingStrategy defaultStrategy;

    public RuntimeFieldNamingStrategy(Map<String, String> runtimeFieldNameMappings) {
        this(runtimeFieldNameMappings, FieldNamingPolicy.IDENTITY);
    }

    public RuntimeFieldNamingStrategy(Map<String, String> runtimeFieldNameMappings, FieldNamingStrategy defaultStrategy) {
        this.runtimeFieldNameMappings = runtimeFieldNameMappings;
        this.defaultStrategy = defaultStrategy != null ? defaultStrategy : FieldNamingPolicy.IDENTITY;
    }

    @Override
    public String translateName(Field field) {
        RuntimeSerializedName runtimeFieldNameAnnotation = field.getAnnotation(RuntimeSerializedName.class);
        if (runtimeFieldNameAnnotation == null) {
            return defaultStrategy.translateName(field);
        }
        String runtimeFieldName = runtimeFieldNameAnnotation.value();
        if (!runtimeFieldNameMappings.containsKey(runtimeFieldName)) {
            throw new IllegalArgumentException("No field name mapping for runtime field name " + runtimeFieldName);
        }
        return runtimeFieldNameMappings.get(runtimeFieldName);
    }
}
