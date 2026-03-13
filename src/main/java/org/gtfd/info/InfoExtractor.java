package org.gtfd.info;

import org.gtfd.info.innnerModels.RequestMapping;
import org.gtfd.info.models.EndpointInfo;
import org.gtfd.info.models.ExtractionInfo;
import org.gtfd.info.models.Method;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InfoExtractor {
    public static ExtractionInfo extractInfo(List<ClassNode> nodeList) {
        List<EndpointInfo> infoList = new ArrayList<EndpointInfo>();
        for (ClassNode node : nodeList) {
            infoList.addAll(Objects.requireNonNull(extractInfoFromClass(node))); //TODO: переделать этого монстра
        }

        //TODO: собрать модели в приложении

        return null;
    }

    private static List<EndpointInfo> extractInfoFromClass(ClassNode node) {
        boolean isNotRestController = true;
        RequestMapping baseRequestMapping = null;
        //CrossOrigin baseCrossOrigin = null;
        //SessionAttributes baseSessionAttributes = null;

        for (AnnotationNode annotation : node.visibleAnnotations) {
            if (annotation.desc.contains("RestController")) { //TODO: добавить поддержку ResponseBody + Controller
                isNotRestController = false;
            } else if (annotation.desc.contains("RequestMapping")) {
                baseRequestMapping = readRequestMapping(annotation);
            } else if (annotation.desc.contains("CrossOrigin")) {
                //TODO: crossorigin processing
            } else if (annotation.desc.contains("SessionAttributes")) {
                //TODO: sessionattributes processing
            }
        }

        if (isNotRestController) {
            return null;
        }

        List<EndpointInfo> result = new ArrayList<>();
        for (MethodNode methodNode : node.methods) {
            RequestMapping methodRequestMapping = baseRequestMapping;
            Method method = null;
            for (AnnotationNode annotation : methodNode.visibleAnnotations) {
                switch (annotation.desc) {
                    case "GetMapping":
                        if (method == null) {
                            method = Method.GET;
                        }
                    case "PostMapping":
                        if (method == null) {
                            method = Method.POST;
                        }
                    case "PutMapping":
                        if (method == null) {
                            method = Method.PUT;
                        }
                    case "DeleteMapping":
                        if (method == null) {
                            method = Method.DELETE;
                        }
                    case "PatchMapping":
                        if (method == null) {
                            method = Method.PATCH;
                        }
                    case "RequestMapping":
                        if (methodRequestMapping != null) {
                            // TODO: join annotations
                        } else {
                            methodRequestMapping = readRequestMapping(annotation);
                        }
                        if (method != null) {
                            methodRequestMapping.setMethod(method);
                        }
                        break;
                    case "CrossOrigin":
                        // TODO: cross origin
                }
            }

            //TODO: analyze method parameters

            if (methodRequestMapping == null) {
                continue;
            }

            var methodInfo = new EndpointInfo(methodRequestMapping.path(), methodRequestMapping.method());
            result.add(methodInfo);
        }

        return result;
    }

    public static RequestMapping readRequestMapping(AnnotationNode node) {
        List<String> path     = List.of();
        List<Method> method   = List.of();
        List<String> params   = List.of();
        List<String> headers  = List.of();
        List<String> consumes = List.of();
        List<String> produces = List.of();

        if (node.values == null) {
            return new RequestMapping(path, method, params, headers, consumes, produces);
        }

        for (int i = 0; i < node.values.size(); i += 2) {
            String key = (String) node.values.get(i);
            Object raw = node.values.get(i + 1);

            switch (key) {
                case "value", "path" -> path     = resolveStringList(raw);
                case "method"        -> method   = resolveMethodList(raw);
                case "params"        -> params   = resolveStringList(raw);
                case "headers"       -> headers  = resolveStringList(raw);
                case "consumes"      -> consumes = resolveStringList(raw);
                case "produces"      -> produces = resolveStringList(raw);
            }
        }

        return new RequestMapping(path, method, params, headers, consumes, produces);
    }

    private static List<String> resolveStringList(Object raw) {
        if (raw instanceof String s)   return List.of(s);
        if (raw instanceof List<?> list) return (List<String>) list;
        return List.of();
    }

    private static List<Method> resolveMethodList(Object raw) {
        //for one Enum
        if (raw instanceof String[] e) {
            return List.of(Method.valueOf(e[1]));
        }

        //for many Enums
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(e -> Method.valueOf(((String[]) e)[1]))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}
