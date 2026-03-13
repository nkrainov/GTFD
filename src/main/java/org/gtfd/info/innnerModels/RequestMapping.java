package org.gtfd.info.innnerModels;

import org.gtfd.info.models.Method;

import java.util.List;

public record RequestMapping(
        List<String> path,
        List<Method> method,
        List<String> params,
        List<String> headers,
        List<String> consumes,
        List<String> produces
) {
    public void setMethod(Method method) {
        this.method.add(method);
    }
}
