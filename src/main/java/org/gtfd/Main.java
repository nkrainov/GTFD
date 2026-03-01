package org.gtfd;

import org.gtfd.loader.JarClassInfoLoader;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar <this jar> <filename>");
            System.exit(1);
        }

        try {
            JarClassInfoLoader.loadJar(args[0]);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }


    }
}