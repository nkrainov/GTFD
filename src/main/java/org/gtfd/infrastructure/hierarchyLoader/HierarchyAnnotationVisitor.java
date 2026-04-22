package org.gtfd.infrastructure.hierarchyLoader;

import org.gtfd.domain.hierarchy.AnnotationInfo;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public class HierarchyAnnotationVisitor extends org.objectweb.asm.AnnotationVisitor {
    private final AnnotationInfo target;

    public HierarchyAnnotationVisitor(AnnotationInfo target) {
        super(Opcodes.ASM9);
        this.target = target;
    }

    @Override
    public void visit(String name, Object value) {
        if (value instanceof Type) {
            value = ((Type) value).getClassName();
        }
        target.addValue(name, value);
    }

    @Override
    public void visitEnum(String name, String descriptor, String value) {
        // ИСПРАВЛЕНИЕ: конвертируем дескриптор из JVM-формата (Lorg/foo/Bar;)
        // в dot-нотацию (org.foo.Bar), чтобы Analyzator мог корректно его обработать.
        String className = Type.getType(descriptor).getClassName();
        target.addValue(name, className + "." + value);
    }

    @Override
    public org.objectweb.asm.AnnotationVisitor visitAnnotation(String name, String descriptor) {
        AnnotationInfo nested = new AnnotationInfo(descriptor);
        target.addValue(name, nested);
        return new HierarchyAnnotationVisitor(nested);
    }

    @Override
    public org.objectweb.asm.AnnotationVisitor visitArray(String name) {
        List<Object> arrayValues = new ArrayList<>();
        target.addValue(name, arrayValues);
        return new org.objectweb.asm.AnnotationVisitor(Opcodes.ASM9) {
            @Override
            public void visit(String n, Object value) {
                if (value instanceof Type) value = ((Type) value).getClassName();
                arrayValues.add(value);
            }

            @Override
            public void visitEnum(String n, String descriptor, String value) {
                // ИСПРАВЛЕНИЕ: то же самое — конвертируем дескриптор в dot-нотацию.
                String className = Type.getType(descriptor).getClassName();
                arrayValues.add(className + "." + value);
            }

            @Override
            public org.objectweb.asm.AnnotationVisitor visitAnnotation(String n, String descriptor) {
                AnnotationInfo nested = new AnnotationInfo(descriptor);
                arrayValues.add(nested);
                return new HierarchyAnnotationVisitor(nested);
            }

            @Override
            public org.objectweb.asm.AnnotationVisitor visitArray(String n) {
                List<Object> nested = new ArrayList<>();
                arrayValues.add(nested);
                return new org.objectweb.asm.AnnotationVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(String nn, Object value) {
                        if (value instanceof Type) value = ((Type) value).getClassName();
                        nested.add(value);
                    }
                };
            }
        };
    }
}