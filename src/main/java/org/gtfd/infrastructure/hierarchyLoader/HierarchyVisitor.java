package org.gtfd.infrastructure.hierarchyLoader;

import org.gtfd.domain.hierarchy.*;
import org.objectweb.asm.*;

import java.util.Map;

public class HierarchyVisitor extends ClassVisitor {
    private final Map<String, PreClassInfo> registry;
    private PreClassInfo currentClassInfo;

    public HierarchyVisitor(Map<String, PreClassInfo> registry) {
        super(Opcodes.ASM9);
        this.registry = registry;
    }

    @Override
    public void visit(int version, int access, String name,
                      String signature, String superName, String[] interfaces) {
        currentClassInfo = new PreClassInfo(name, access, superName, interfaces);
        registry.put(name, currentClassInfo);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationInfo ann = new AnnotationInfo(descriptor);
        currentClassInfo.addAnnotation(ann);
        return new HierarchyAnnotationVisitor(ann);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        MethodInfo methodInfo = new MethodInfo(name, descriptor, signature, access, exceptions);
        currentClassInfo.addMethod(methodInfo);

        return new MethodVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationInfo ann = new AnnotationInfo(desc);
                methodInfo.addAnnotation(ann);
                return new HierarchyAnnotationVisitor(ann);
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
                AnnotationInfo ann = new AnnotationInfo(desc);
                methodInfo.addParameterAnnotation(parameter, ann);
                return new HierarchyAnnotationVisitor(ann);
            }
        };
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor,
                                   String signature, Object value) {
        FieldInfo fieldInfo = new FieldInfo(name, descriptor, signature, access);
        currentClassInfo.addField(fieldInfo);

        return new FieldVisitor(Opcodes.ASM9) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                AnnotationInfo ann = new AnnotationInfo(desc);
                fieldInfo.addAnnotation(ann);
                return new HierarchyAnnotationVisitor(ann);
            }
        };
    }
}