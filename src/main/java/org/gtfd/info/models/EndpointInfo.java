package org.gtfd.info.models;

import java.util.List;

public class EndpointInfo { //TODO переименовать, это не про эндпоинты
    public List<String> path;
    public List<Method> method;

    public EndpointInfo(List<String> path, List<Method> method) {
        this.path = path;
        this.method = method;
    }
}
