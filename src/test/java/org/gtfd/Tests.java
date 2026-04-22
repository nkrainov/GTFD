package org.gtfd;

import org.gtfd.application.hierarchy.HierarchyBuilder;
import org.gtfd.domain.hierarchy.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HierarchyBuilderTest {

    @Test
    void buildHierarchy_shouldSetSuperClassAndSubClasses() {
        PreClassInfo preParent = createPreClassInfo("Parent", null, new String[]{});
        PreClassInfo preChild = createPreClassInfo("Child", "Parent", new String[]{});

        Map<String, PreClassInfo> preMap = new HashMap<>();
        preMap.put("Parent", preParent);
        preMap.put("Child", preChild);

        Map<String, ClassInfo> classMap = HierarchyBuilder.buildHierarchy(preMap);

        ClassInfo parent = classMap.get("Parent");
        ClassInfo child = classMap.get("Child");

        assertNotNull(parent);
        assertNotNull(child);
        assertEquals(parent, child.getSuperClass());
        assertTrue(parent.getSubClasses().contains(child));
    }

    @Test
    void buildHierarchy_shouldSetInterfacesAndImplementors() {
        PreClassInfo preInterface = createPreClassInfo("MyInterface", null, new String[]{});
        PreClassInfo preImpl = createPreClassInfo("MyImpl", null, new String[]{"MyInterface"});

        Map<String, PreClassInfo> preMap = new HashMap<>();
        preMap.put("MyInterface", preInterface);
        preMap.put("MyImpl", preImpl);

        Map<String, ClassInfo> classMap = HierarchyBuilder.buildHierarchy(preMap);

        ClassInfo iface = classMap.get("MyInterface");
        ClassInfo impl = classMap.get("MyImpl");

        assertNotNull(iface);
        assertNotNull(impl);
        assertTrue(impl.getInterfaces().contains(iface));
        assertTrue(iface.getImplementors().contains(impl));
    }

    @Test
    void buildHierarchy_shouldHandleMissingSuperClass() {
        PreClassInfo preChild = createPreClassInfo("Child", "Missing", new String[]{});

        Map<String, PreClassInfo> preMap = new HashMap<>();
        preMap.put("Child", preChild);

        Map<String, ClassInfo> classMap = HierarchyBuilder.buildHierarchy(preMap);

        ClassInfo child = classMap.get("Child");
        assertNotNull(child);
        assertNull(child.getSuperClass());
    }

    private PreClassInfo createPreClassInfo(String name, String superName, String[] interfaces) {
        PreClassInfo pre = new PreClassInfo(name, 0, superName, interfaces);
        return pre;
    }
}