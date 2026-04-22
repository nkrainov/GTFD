package org.gtfd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.gtfd.application.doc.Analyzator;
import org.gtfd.application.hierarchy.HierarchyBuilder;
import org.gtfd.infrastructure.hierarchyLoader.PreInfoLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Main {

    private static final String[] FRONTEND_RESOURCES = {
            "index.html",
            "app.js",
            "style.css"
    };

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java -jar <this jar> <filename>");
            System.exit(1);
        }

        try {
            //main use case of application
            var preInfo = PreInfoLoader.loadPreInfoFromJar(args[0]);
            var hierarchy = HierarchyBuilder.buildHierarchy(preInfo);
            var doc = Analyzator.analyze(hierarchy);

            //saving result - we create directory and put frontend resources
            //with server jar
            Path outDir = Path.of("api-docs");
            Path docDir = outDir.resolve("doc");
            Files.createDirectories(docDir);

            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(docDir.resolve("api-docs.json").toFile(), doc);

            for (String resource : FRONTEND_RESOURCES) {
                try (InputStream is = Main.class.getClassLoader().getResourceAsStream("frontend/" + resource)) {
                    if (is == null) {
                        System.err.println("Warning: resource not found in JAR: frontend/" + resource);
                        continue;
                    }
                    Files.copy(is, outDir.resolve(resource), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            try (InputStream is = Main.class.getClassLoader().getResourceAsStream("server.jar")) {
                if (is == null) {
                    System.err.println("Warning: server.jar not found in JAR resources");
                } else {
                    Files.copy(is, outDir.resolve("server.jar"), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            System.exit(1);
        }


        System.out.println("Done. Output written to api-docs directory.");
        System.out.println("To start the server: cd api-docs && java -jar server.jar");
    }
}