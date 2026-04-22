package org.gtfd.domain.hierarchy;

import java.util.ArrayList;
import java.util.List;

public class PreClassInfo {
    private final String name;
    private final int access;
    private final String superName;
    private final String[] interfaces;
    private final List<AnnotationInfo> annotations = new ArrayList<>();
    private final List<MethodInfo> methods = new ArrayList<>();
    private final List<FieldInfo> fields = new ArrayList<>(); // новое поле

    public PreClassInfo(String name, int access, String superName, String[] interfaces) {
        this.name = name;
        this.access = access;
        this.superName = superName;
        this.interfaces = interfaces;
    }

    public String getName() { return name; }
    public int getAccess() { return access; }
    public String getSuperName() { return superName; }
    public String[] getInterfaces() { return interfaces; }
    public List<AnnotationInfo> getAnnotations() { return annotations; }
    public List<MethodInfo> getMethods() { return methods; }
    public List<FieldInfo> getFields() { return fields; }

    public void addAnnotation(AnnotationInfo ann) { annotations.add(ann); }
    public void addMethod(MethodInfo method) { methods.add(method); }
    public void addField(FieldInfo field) { fields.add(field); }

    public boolean isInterface() {
        return (access & org.objectweb.asm.Opcodes.ACC_INTERFACE) != 0;
    }
}