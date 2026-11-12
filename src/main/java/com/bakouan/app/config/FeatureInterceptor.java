package com.bakouan.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FeatureInterceptor implements HandlerInterceptor {

    private static final List<String> HABILITATION_PATHS = List.of(
            "/api/habilitations",
            "/api/circuits",
            "/api/validation-delegations",
            "/api/dga-pole-validateurs",
            "/api/reporting/habilitations"
    );

    private final FeatureConfig featureConfig;

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response,
                             final Object handler) throws IOException {
        if (featureConfig.isHabilitationsEnabled() || !isHabilitationPath(request.getRequestURI())) {
            return true;
        }

        response.setStatus(HttpStatus.NOT_FOUND.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":\"FEATURE_DISABLED\",\"message\":\"Le domaine Habilitations est desactive.\"}");
        return false;
    }

    private boolean isHabilitationPath(final String requestUri) {
        return HABILITATION_PATHS.stream()
                .anyMatch(path -> requestUri.equals(path) || requestUri.startsWith(path + "/"));
    }
}
