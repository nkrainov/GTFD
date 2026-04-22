package org.gtfd.domain.hierarchy;

import java.util.LinkedHashMap;
import java.util.Map;

public class AnnotationInfo {
    private final String descriptor;
    private final Map<String, Object> values = new LinkedHashMap<>();

    public AnnotationInfo(String descriptor) {
        this.descriptor = descriptor;
    }

    public String getDescriptor() { return descriptor; }
    public Map<String, Object> getValues() { return values; }

    public void addValue(String name, Object value) {
        values.put(name, value);
    }
}