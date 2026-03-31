package org.gtfd.infrastructure.hierarchyLoader;

import org.gtfd.domain.hierarchy.PreClassInfo;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class PreInfoLoader {
    static public Map<String, PreClassInfo> loadPreInfoFromJar(String path) throws IOException {
        try (JarFile jar = new JarFile(path)) {
            Map<String, PreClassInfo> preInfoMap = new HashMap<>();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        ClassReader reader = new ClassReader(is);
                        reader.accept(new HierarchyVisitor(preInfoMap),
                                ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                    }
                }
            }
            return preInfoMap;
        }
    }
}
