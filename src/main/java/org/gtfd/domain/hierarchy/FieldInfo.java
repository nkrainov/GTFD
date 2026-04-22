package org.gtfd.domain.hierarchy;

import java.util.ArrayList;
import java.util.List;

public class FieldInfo {
    private final String name;
    private final String descriptor;
    private final String signature;
    private final int access;
    private final List<AnnotationInfo> annotations = new ArrayList<>();

    public FieldInfo(String name, String descriptor, String signature, int access) {
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
        this.access = access;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getSignature() {
        return signature;
    }

    public int getAccess() {
        return access;
    }

    public List<AnnotationInfo> getAnnotations() {
        return annotations;
    }

    public void addAnnotation(AnnotationInfo ann) {
        annotations.add(ann);
    }
}