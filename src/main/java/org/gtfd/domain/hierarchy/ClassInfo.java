package org.gtfd.domain.hierarchy;

import java.util.*;

public class ClassInfo {
    private final String name;
    private final int access;
    private final List<AnnotationInfo> annotations;
    private final List<MethodInfo> methods;
    private final List<FieldInfo> fields; // новое поле

    private ClassInfo superClass;
    private final List<ClassInfo> interfaces;
    private final List<ClassInfo> subClasses;
    private final List<ClassInfo> implementors;

    public ClassInfo(String name, int access, List<AnnotationInfo> annotations,
                     List<MethodInfo> methods, List<FieldInfo> fields) { // обновлённый конструктор
        this.name = name;
        this.access = access;
        this.annotations = new ArrayList<>(annotations);
        this.methods = new ArrayList<>(methods);
        this.fields = new ArrayList<>(fields);
        this.interfaces = new ArrayList<>();
        this.subClasses = new ArrayList<>();
        this.implementors = new ArrayList<>();
    }

    public String getName() { return name; }
    public List<AnnotationInfo> getAnnotations() { return annotations; }
    public List<MethodInfo> getMethods() { return methods; }
    public List<FieldInfo> getFields() { return fields; } // новый геттер

    public ClassInfo getSuperClass() { return superClass; }

    public void setSuperClass(ClassInfo superClass) {
        this.superClass = superClass;
    }

    public void addInterface(ClassInfo iface) {
        if (!interfaces.contains(iface)) {
            interfaces.add(iface);
        }
    }

    public void addSubClass(ClassInfo subClass) {
        if (!subClasses.contains(subClass)) {
            subClasses.add(subClass);
        }
    }

    public void addImplementor(ClassInfo implementor) {
        if (!implementors.contains(implementor)) {
            implementors.add(implementor);
        }
    }
}