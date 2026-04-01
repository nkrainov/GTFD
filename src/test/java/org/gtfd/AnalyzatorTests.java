package org.gtfd;

import org.gtfd.application.doc.Analyzator;
import org.gtfd.domain.hierarchy.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AnalyzatorTest {

    @Test
    void analyze_shouldDetectRestController() {
        Map<String, ClassInfo> classMap = new HashMap<>();
        ClassInfo controller = createControllerWithAnnotation("RestController");
        classMap.put("Controller", controller);

        Analyzator.ApiDocumentation doc = Analyzator.analyze(classMap);
        assertTrue(doc.paths.isEmpty());
    }

    @Test
    void analyze_shouldExtractBasePath() {
        ClassInfo controller = createControllerWithRequestMapping("/api");
        MethodInfo method = createGetMethod("/users");
        controller.getMethods().add(method);

        Map<String, ClassInfo> classMap = new HashMap<>();
        classMap.put("Controller", controller);

        Analyzator.ApiDocumentation doc = Analyzator.analyze(classMap);
        assertEquals(1, doc.paths.size());
        Analyzator.EndpointInfo endpoint = doc.paths.get("/api/users").get("GET");
        assertNotNull(endpoint);
        assertEquals("/api/users", endpoint.path);
        assertEquals("GET", endpoint.method);
    }

    @Test
    void analyze_shouldExtractParameters() {
        ClassInfo controller = createControllerWithRequestMapping("");
        MethodInfo method = createMethodWithParameters();
        controller.getMethods().add(method);

        Map<String, ClassInfo> classMap = new HashMap<>();
        classMap.put("Controller", controller);

        Analyzator.ApiDocumentation doc = Analyzator.analyze(classMap);
        Analyzator.EndpointInfo endpoint = doc.paths.get("/test").get("POST");
        assertNotNull(endpoint);
        assertEquals(2, endpoint.parameters.size());

        Analyzator.ParameterInfo p0 = endpoint.parameters.get(0);
        assertEquals("query", p0.in);
        assertTrue(p0.required);

        Analyzator.ParameterInfo p1 = endpoint.parameters.get(1);
        assertEquals("body", p1.in);
        assertEquals("object", p1.type);
        assertTrue(p1.required);
    }

    @Test
    void analyze_shouldExtractRequestBody() {
        ClassInfo controller = createControllerWithRequestMapping("");
        MethodInfo method = createMethodWithRequestBody();
        controller.getMethods().add(method);

        Map<String, ClassInfo> classMap = new HashMap<>();
        classMap.put("Controller", controller);

        Analyzator.ApiDocumentation doc = Analyzator.analyze(classMap);
        Analyzator.EndpointInfo endpoint = doc.paths.get("/test").get("POST");
        assertEquals("User", endpoint.requestBody);
    }

    @Test
    void analyze_shouldExtractResponses() {
        ClassInfo controller = createControllerWithRequestMapping("");
        MethodInfo method = createMethodWithReturnType("User");
        controller.getMethods().add(method);

        Map<String, ClassInfo> classMap = new HashMap<>();
        classMap.put("Controller", controller);

        Analyzator.ApiDocumentation doc = Analyzator.analyze(classMap);
        Analyzator.EndpointInfo endpoint = doc.paths.get("/test").get("GET");
        assertEquals(1, endpoint.responses.size());
        assertEquals("User", endpoint.responses.get("200"));
    }

    @Test
    void analyze_shouldCollectModels() {
        ClassInfo controller = createControllerWithRequestMapping("");
        MethodInfo method = createMethodWithRequestBody();
        controller.getMethods().add(method);

        ClassInfo userClass = createUserClass();
        Map<String, ClassInfo> classMap = new HashMap<>();
        classMap.put("Controller", controller);
        classMap.put("User", userClass);

        Analyzator.ApiDocumentation doc = Analyzator.analyze(classMap);
        assertTrue(doc.models.containsKey("User"));
        Analyzator.ModelInfo model = doc.models.get("User");
        assertEquals(2, model.fields.size());
        assertEquals("name", model.fields.get(0).name);
        assertEquals("string", model.fields.get(0).type);
        assertEquals("age", model.fields.get(1).name);
        assertEquals("integer", model.fields.get(1).type);
    }

    @Test
    void toOpenApiType_shouldMapBasicTypes() {
        assertEquals("string", Analyzator.toOpenApiType("String"));
        assertEquals("integer", Analyzator.toOpenApiType("int"));
        assertEquals("integer", Analyzator.toOpenApiType("Integer"));
        assertEquals("number", Analyzator.toOpenApiType("double"));
        assertEquals("boolean", Analyzator.toOpenApiType("boolean"));
        assertEquals("array", Analyzator.toOpenApiType("List"));
        assertEquals("object", Analyzator.toOpenApiType("Map"));
        assertEquals("string", Analyzator.toOpenApiType("LocalDateTime"));
        assertEquals("string", Analyzator.toOpenApiType("Date"));
        assertEquals("string", Analyzator.toOpenApiType("UUID"));
    }


    private ClassInfo createControllerWithAnnotation(String annotationName) {
        ClassInfo ci = new ClassInfo("TestController", 0, Collections.emptyList(), new ArrayList<>(), Collections.emptyList());
        AnnotationInfo ann = new AnnotationInfo("Lorg/springframework/web/bind/annotation/" + annotationName + ";");
        ci.getAnnotations().add(ann);
        return ci;
    }

    private ClassInfo createControllerWithRequestMapping(String path) {
        ClassInfo ci = createControllerWithAnnotation("RestController");
        AnnotationInfo mapping = new AnnotationInfo("Lorg/springframework/web/bind/annotation/RequestMapping;");
        mapping.getValues().put("value", Collections.singletonList(path));
        ci.getAnnotations().add(mapping);
        return ci;
    }

    private MethodInfo createGetMethod(String path) {
        MethodInfo mi = new MethodInfo("getUsers", "()Lorg/gtfd/User;", null, 0, new String[0]);
        AnnotationInfo getMapping = new AnnotationInfo("Lorg/springframework/web/bind/annotation/GetMapping;");
        getMapping.getValues().put("value", Collections.singletonList(path));
        mi.getAnnotations().add(getMapping);
        return mi;
    }

    private MethodInfo createMethodWithParameters() {
        MethodInfo mi = new MethodInfo("createUser", "(Ljava/lang/String;Lorg/gtfd/User;)V", null, 0, new String[0]);
        AnnotationInfo requestParam = new AnnotationInfo("Lorg/springframework/web/bind/annotation/RequestParam;");
        requestParam.getValues().put("required", true);
        mi.addParameterAnnotation(0, requestParam);
        AnnotationInfo requestBody = new AnnotationInfo("Lorg/springframework/web/bind/annotation/RequestBody;");
        mi.addParameterAnnotation(1, requestBody);
        AnnotationInfo postMapping = new AnnotationInfo("Lorg/springframework/web/bind/annotation/PostMapping;");
        postMapping.getValues().put("value", Collections.singletonList("/test"));
        mi.getAnnotations().add(postMapping);
        return mi;
    }

    private MethodInfo createMethodWithRequestBody() {
        MethodInfo mi = new MethodInfo("createUser", "(Lorg/gtfd/User;)Lorg/gtfd/User;", null, 0, new String[0]);
        AnnotationInfo requestBody = new AnnotationInfo("Lorg/springframework/web/bind/annotation/RequestBody;");
        mi.addParameterAnnotation(0, requestBody);
        AnnotationInfo postMapping = new AnnotationInfo("Lorg/springframework/web/bind/annotation/PostMapping;");
        postMapping.getValues().put("value", Collections.singletonList("/test"));
        mi.getAnnotations().add(postMapping);
        return mi;
    }

    private MethodInfo createMethodWithReturnType(String returnType) {
        MethodInfo mi = new MethodInfo("getUser", "()L" + returnType.replace('.', '/') + ";", null, 0, new String[0]);
        AnnotationInfo getMapping = new AnnotationInfo("Lorg/springframework/web/bind/annotation/GetMapping;");
        getMapping.getValues().put("value", Collections.singletonList("/test"));
        mi.getAnnotations().add(getMapping);
        return mi;
    }

    private ClassInfo createUserClass() {
        List<FieldInfo> fields = new ArrayList<>();
        fields.add(new FieldInfo("name", "Ljava/lang/String;", null, 0));
        fields.add(new FieldInfo("age", "I", null, 0));
        ClassInfo user = new ClassInfo("User", 0, Collections.emptyList(), Collections.emptyList(), fields);
        return user;
    }
}