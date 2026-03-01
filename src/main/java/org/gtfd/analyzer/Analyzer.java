package org.gtfd.analyzer;

import org.gtfd.analyzer.records.AnalyzeRecord;
import org.gtfd.analyzer.records.MethodRecord;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class Analyzer {
    public static AnalyzeRecord analyzeClasses(List<ClassNode> nodes) {
        AnalyzeRecord record = new AnalyzeRecord();
        for (ClassNode node : nodes) {
            var methods = analyzeClass(node);
            if (methods != null) {
                record.addMethodRecords(methods);
            }
        }

        return record;
    }

    private static List<MethodRecord> analyzeClass(ClassNode node) {
        //проверяем, что класс аннотирован нужным образом
        if (isRestController(node)) {
            return null;
        }

        return analyzeClassMethods(node);
    }

    //TODO: добавить проверку не только @RestController, но и @Controller + @ResponseBody
    private static boolean isRestController(ClassNode node) {
        for (AnnotationNode annotation : node.visibleAnnotations) {
            if (annotation.desc.endsWith("RestController;")) {
                return true;
            }
        }

        return false;
    }

    public static List<MethodRecord> analyzeClassMethods(ClassNode node) {
        List<MethodRecord> records = new ArrayList<MethodRecord>();
        for (MethodNode method : node.methods) {
            records.add(analyzeMethod(method));
        }

        return records;
    }

    public static MethodRecord analyzeMethod(MethodNode method) {
        MethodRecord result = null;
        for (AnnotationNode annotation : method.visibleAnnotations) {
            if (annotation.desc.endsWith("GetMapping;")) {
                //TODO: заполнять record, не пересоздавать
                result = new MethodRecord();
            }
        }

        return result;
    }
}
