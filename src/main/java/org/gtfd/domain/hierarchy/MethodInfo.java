package org.gtfd.domain.hierarchy;

import java.util.*;

public class MethodInfo {
    private final String name;
    private final String descriptor;
    private final String signature;
    private final int access;
    private final String[] exceptions;
    private final List<AnnotationInfo> annotations = new ArrayList<>();
    private final Map<Integer, List<AnnotationInfo>> parameterAnnotations = new HashMap<>();

    public MethodInfo(String name, String descriptor, String signature, int access, String[] exceptions) {
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
        this.access = access;
        this.exceptions = exceptions;
    }

    public String getName() { return name; }
    public String getDescriptor() { return descriptor; }
    public String getSignature() { return signature; }
    public int getAccess() { return access; }
    public String[] getExceptions() { return exceptions; }
    public List<AnnotationInfo> getAnnotations() { return annotations; }
    public Map<Integer, List<AnnotationInfo>> getParameterAnnotations() { return parameterAnnotations; }

    public void addAnnotation(AnnotationInfo ann) {
        annotations.add(ann);
    }

    public void addParameterAnnotation(int paramIndex, AnnotationInfo ann) {
        parameterAnnotations.computeIfAbsent(paramIndex, k -> new ArrayList<>()).add(ann);
    }
}