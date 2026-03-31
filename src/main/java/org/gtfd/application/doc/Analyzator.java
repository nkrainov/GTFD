package org.gtfd.application.doc;

import org.gtfd.domain.hierarchy.*;
import org.objectweb.asm.Type;

import java.util.*;

public class Analyzator {
    public static class ApiDocumentation {
        public Map<String, Map<String, EndpointInfo>> paths = new LinkedHashMap<>();
        public Map<String, ModelInfo> models = new LinkedHashMap<>();

        public void addEndpoint(EndpointInfo endpoint) {
            Map<String, EndpointInfo> byMethod = paths.computeIfAbsent(endpoint.path, k -> new LinkedHashMap<>());
            byMethod.put(endpoint.method, endpoint);
        }
    }

    public static class EndpointInfo {
        public String path;
        public String method;
        public List<ParameterInfo> parameters = new ArrayList<>();
        public String requestBody;
        public Map<String, String> responses = new LinkedHashMap<>();

        public EndpointInfo(String path, String method) {
            this.path = path;
            this.method = method;
        }
    }

    public static class ParameterInfo {
        public String in;
        public String type;
        public boolean required;
    }

    public static class ModelInfo {
        public List<AFieldInfo> fields = new ArrayList<>();
    }

    public static class AFieldInfo {
        public String type;

        public AFieldInfo(String type) {
            this.type = type;
        }
    }

    //utils

    private static List<AnnotationInfo> getAllAnnotations(ClassInfo classInfo) {
        if (classInfo == null) {
            return Collections.emptyList();
        }

        List<AnnotationInfo> result = new ArrayList<>(classInfo.getAnnotations());
        ClassInfo superClass = classInfo.getSuperClass();
        if (superClass != null) {
            result.addAll(getAllAnnotations(superClass));
        }
        return result;
    }

    private static List<MethodInfo> getAllMethods(ClassInfo classInfo) {
        if (classInfo == null) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, MethodInfo> bySignature = new LinkedHashMap<>();

        ClassInfo superClass = classInfo.getSuperClass();
        if (superClass != null) {
            for (MethodInfo m : getAllMethods(superClass)) {
                bySignature.put(methodKey(m), m);
            }
        }
        for (MethodInfo m : classInfo.getMethods()) {
            bySignature.put(methodKey(m), m);
        }
        return new ArrayList<>(bySignature.values());
    }

    private static String methodKey(MethodInfo m) {
        return m.getName() + "#" + m.getDescriptor();
    }

    private static List<FieldInfo> getAllFields(ClassInfo classInfo) {
        if (classInfo == null) {
            return Collections.emptyList();
        }
        List<FieldInfo> result = new ArrayList<>();
        ClassInfo superClass = classInfo.getSuperClass();
        if (superClass != null) {
            result.addAll(getAllFields(superClass));
        }
        result.addAll(classInfo.getFields());
        return result;
    }

    //main

    public static ApiDocumentation analyze(Map<String, ClassInfo> classInfoMap) {
        ApiDocumentation doc = new ApiDocumentation();

        List<ClassInfo> controllers = classInfoMap.values().stream()
                .filter(Analyzator::isRestController)
                .toList();

        Map<String, ModelInfo> models = new LinkedHashMap<>();

        for (ClassInfo controller : controllers) {
            String basePath = extractBasePath(controller);

            for (MethodInfo method : getAllMethods(controller)) {
                EndpointInfo endpoint = extractEndpoint(method, basePath);
                if (endpoint != null) {
                    doc.addEndpoint(endpoint);
                    collectModels(endpoint, classInfoMap, models);
                }
            }
        }

        doc.models.putAll(models);
        return doc;
    }

    private static boolean isRestController(ClassInfo classInfo) {
        List<AnnotationInfo> allAnnotations = getAllAnnotations(classInfo);
        if (hasAnnotation(allAnnotations, "org.springframework.web.bind.annotation.RestController")) {
            return true;
        }
        return hasAnnotation(allAnnotations, "org.springframework.web.bind.annotation.Controller") &&
                hasAnnotation(allAnnotations, "org.springframework.web.bind.annotation.ResponseBody");
    }

    private static boolean hasAnnotation(List<AnnotationInfo> annotations, String className) {
        String desc = "L" + className.replace('.', '/') + ";";
        return annotations.stream().anyMatch(ann -> ann.getDescriptor().equals(desc));
    }


    private static String extractBasePath(ClassInfo controller) {
        if (controller == null) {
            return "";
        }

        Optional<AnnotationInfo> requestMapping = controller.getAnnotations().stream()
                .filter(ann -> ann.getDescriptor().equals("Lorg/springframework/web/bind/annotation/RequestMapping;"))
                .findFirst();
        if (requestMapping.isPresent()) {
            String path = extractFirstString(requestMapping.get().getValues().get("value"));
            if (path != null) return normalizePath(path);
            path = extractFirstString(requestMapping.get().getValues().get("path"));
            if (path != null) return normalizePath(path);
        }

        return extractBasePath(controller.getSuperClass());
    }

    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "";
        return path.startsWith("/") ? path : "/" + path;
    }

    private static EndpointInfo extractEndpoint(MethodInfo method, String basePath) {
        for (AnnotationInfo ann : method.getAnnotations()) {
            String desc = ann.getDescriptor();
            if (desc.startsWith("Lorg/springframework/web/bind/annotation/")) {
                EndpointInfo endpoint = null;
                if (desc.contains("GetMapping")) {
                    endpoint = buildEndpoint(method, "GET", basePath, ann);
                } else if (desc.contains("PostMapping")) {
                    endpoint = buildEndpoint(method, "POST", basePath, ann);
                } else if (desc.contains("PutMapping")) {
                    endpoint = buildEndpoint(method, "PUT", basePath, ann);
                } else if (desc.contains("DeleteMapping")) {
                    endpoint = buildEndpoint(method, "DELETE", basePath, ann);
                } else if (desc.contains("PatchMapping")) {
                    endpoint = buildEndpoint(method, "PATCH", basePath, ann);
                } else if (desc.contains("RequestMapping")) {
                    String httpMethod = extractHttpMethodFromRequestMapping(ann);
                    if (httpMethod != null) {
                        endpoint = buildEndpoint(method, httpMethod, basePath, ann);
                    }
                }
                if (endpoint != null) {
                    endpoint.parameters = extractParameters(method);
                    endpoint.requestBody = extractRequestBody(method);
                    endpoint.responses = extractResponses(method);
                    return endpoint;
                }
            }
        }
        return null;
    }

    private static String extractHttpMethodFromRequestMapping(AnnotationInfo ann) {
        Object methodAttr = ann.getValues().get("method");
        if (methodAttr instanceof List) {
            List<?> methods = (List<?>) methodAttr;
            if (!methods.isEmpty()) {
                return stripRequestMethodPrefix(methods.getFirst().toString());
            }
        } else if (methodAttr instanceof String) {
            return stripRequestMethodPrefix(methodAttr.toString());
        }
        return null;
    }

    private static String stripRequestMethodPrefix(String value) {
        final String prefix = "org.springframework.web.bind.annotation.RequestMethod.";
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private static EndpointInfo buildEndpoint(MethodInfo method, String httpMethod, String basePath, AnnotationInfo mappingAnn) {
        String methodPath = extractPathFromMapping(mappingAnn);
        String fullPath = normalizePath(basePath + methodPath);
        return new EndpointInfo(fullPath, httpMethod);
    }

    private static String extractPathFromMapping(AnnotationInfo mappingAnn) {
        String path = extractFirstString(mappingAnn.getValues().get("value"));
        if (path != null) return normalizePath(path);

        path = extractFirstString(mappingAnn.getValues().get("path"));
        if (path != null) return normalizePath(path);

        return "";
    }

    //we need it because ASM
    private static String extractFirstString(Object value) {
        if (value instanceof String) {
            return (String) value;
        } else if (value instanceof List<?> list) {
            if (!list.isEmpty() && list.getFirst() instanceof String) {
                return (String) list.getFirst();
            }
        }
        return null;
    }

    private static List<ParameterInfo> extractParameters(MethodInfo method) {
        List<ParameterInfo> params = new ArrayList<>();
        Map<Integer, List<AnnotationInfo>> paramAnnotations = method.getParameterAnnotations();
        Type[] argumentTypes = Type.getArgumentTypes(method.getDescriptor());

        for (int i = 0; i < argumentTypes.length; i++) {
            Type type = argumentTypes[i];
            List<AnnotationInfo> annotations = paramAnnotations.getOrDefault(i, Collections.emptyList());
            ParameterInfo param = new ParameterInfo();
            param.type = toOpenApiType(getTypeName(type));

            for (AnnotationInfo ann : annotations) {
                String desc = ann.getDescriptor();
                if (desc.contains("RequestParam")) {
                    param.in = "query";
                    param.required = isRequired(ann);
                    param.required = isRequired(ann);
                    break;
                } else if (desc.contains("PathVariable")) {
                    param.in = "path";
                    param.required = true;
                    break;
                } else {
                    param.in = "body";
                    param.required = true;
                    break;
                }
            }

            if (param.in == null) {
                continue;
            }
            params.add(param);
        }
        return params;
    }

    private static boolean isRequired(AnnotationInfo ann) {
        Object required = ann.getValues().get("required");
        if (required instanceof Boolean) {
            return (Boolean) required;
        }
        return true;
    }

    private static String getTypeName(Type type) {
        String fullName = type.getClassName();
        int arrayIndex = fullName.indexOf('[');
        String suffix = arrayIndex >= 0 ? fullName.substring(arrayIndex) : "";
        String baseName = arrayIndex >= 0 ? fullName.substring(0, arrayIndex) : fullName;
        int dot = baseName.lastIndexOf('.');
        String simpleName = dot >= 0 ? baseName.substring(dot + 1) : baseName;
        return simpleName + suffix;
    }

    public static String toOpenApiType(String javaType) {
        if (javaType == null) return "string";

        return switch (javaType.toLowerCase()) {
            case "int", "integer"                    -> "integer";
            case "long"                              -> "integer";
            case "float"                             -> "number";
            case "double"                            -> "number";
            case "boolean", "bool"                   -> "boolean";
            case "string", "uuid" -> "string";
            case "localdate"                         -> "string";
            case "localdatetime", "offsetdatetime",
                 "zoneddatetime", "date"             -> "string";
            case "byte[]"                            -> "string";
            case "list", "arraylist",
                 "set", "hashset"                    -> "array";
            case "map", "hashmap",
                 "object"                            -> "object";
            default                                  -> "object";
        };
    }

    private static String extractRequestBody(MethodInfo method) {
        Map<Integer, List<AnnotationInfo>> paramAnnotations = method.getParameterAnnotations();
        Type[] argumentTypes = Type.getArgumentTypes(method.getDescriptor());
        for (int i = 0; i < argumentTypes.length; i++) {
            List<AnnotationInfo> annotations = paramAnnotations.getOrDefault(i, Collections.emptyList());
            for (AnnotationInfo ann : annotations) {
                if (ann.getDescriptor().contains("RequestBody")) {
                    return getTypeName(argumentTypes[i]);
                }
            }
        }
        return null;
    }

    private static Map<String, String> extractResponses(MethodInfo method) {
        Map<String, String> responses = new LinkedHashMap<>();
        Type returnType = Type.getReturnType(method.getDescriptor());
        String returnTypeName = getTypeName(returnType);
        if (!returnTypeName.equals("void")) {
            responses.put("200", returnTypeName);
        }

        for (AnnotationInfo ann : method.getAnnotations()) {
            if (ann.getDescriptor().equals("Lorg/springframework/web/bind/annotation/ResponseStatus;")) {
                Object code = ann.getValues().get("code");
                if (code != null) {
                    responses.clear();
                    responses.put(code.toString(), returnTypeName);
                }
            }
        }
        return responses;
    }

    private static ClassInfo findClass(String typeName, Map<String, ClassInfo> allClasses) {
        String internalName = typeName.replace('.', '/');
        ClassInfo result = allClasses.get(internalName);
        if (result != null) return result;

        String suffix = "/" + internalName;
        for (Map.Entry<String, ClassInfo> entry : allClasses.entrySet()) {
            if (entry.getKey().endsWith(suffix)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static void collectModels(EndpointInfo endpoint, Map<String, ClassInfo> allClasses, Map<String, ModelInfo> models) {
        if (endpoint.requestBody != null) {
            collectModel(endpoint.requestBody, allClasses, models);
        }
        for (String responseType : endpoint.responses.values()) {
            if (responseType != null) {
                collectModel(responseType, allClasses, models);
            }
        }
        for (ParameterInfo param : endpoint.parameters) {
            if ("body".equals(param.in) && param.type != null) {
                collectModel(param.type, allClasses, models);
            }
        }
    }

    private static void collectModel(String typeName, Map<String, ClassInfo> allClasses, Map<String, ModelInfo> models) {
        if (models.containsKey(typeName)) return;
        ClassInfo classInfo = findClass(typeName, allClasses);
        if (classInfo == null) {
            return;
        }

        ModelInfo model = new ModelInfo();
        models.put(typeName, model);

        for (FieldInfo field : getAllFields(classInfo)) {
            String fieldType = getTypeName(Type.getType(field.getDescriptor()));
            model.fields.add(new AFieldInfo(toOpenApiType(fieldType)));
            collectModel(fieldType, allClasses, models);
        }
    }
}