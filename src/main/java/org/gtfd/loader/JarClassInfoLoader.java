package org.gtfd.loader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


public class JarClassInfoLoader {
    private JarClassInfoLoader() {}

    public static List<ClassNode> loadJar(String path) throws IOException {
        List<ClassNode> classNodes = new ArrayList<>();

        try (JarFile jar = new JarFile(path)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    try (InputStream is = jar.getInputStream(entry)) {
                        ClassReader reader = new ClassReader(is);
                        ClassNode classNode = new ClassNode();
                        reader.accept(classNode, 0);
                        classNodes.add(classNode);
                    }
                }
            }
        }

        return classNodes;
    }
}
