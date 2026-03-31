package org.gtfd.application.hierarchy;

import org.gtfd.domain.hierarchy.*;

import java.util.*;

public class HierarchyBuilder {

    public static Map<String, ClassInfo> buildHierarchy(Map<String, PreClassInfo> preInfoMap) {
        Map<String, ClassInfo> classInfoMap = new HashMap<>();
        for (PreClassInfo pre : preInfoMap.values()) {
            ClassInfo classInfo = new ClassInfo(
                    pre.getName(),
                    pre.getAccess(),
                    pre.getAnnotations(),
                    pre.getMethods(),
                    pre.getFields()
            );
            classInfoMap.put(pre.getName(), classInfo);
        }

        for (PreClassInfo pre : preInfoMap.values()) {
            ClassInfo current = classInfoMap.get(pre.getName());

            if (!pre.isInterface() && pre.getSuperName() != null) {
                ClassInfo superClass = classInfoMap.get(pre.getSuperName());
                current.setSuperClass(superClass);
                if (superClass != null) {
                    superClass.addSubClass(current);
                }
            }

            for (String ifaceName : pre.getInterfaces()) {
                ClassInfo iface = classInfoMap.get(ifaceName);
                if (iface != null) {
                    current.addInterface(iface);
                    iface.addImplementor(current);
                }
            }
        }

        return classInfoMap;
    }
}