package com.bakouan.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureInterceptorTest {

    private final FeatureConfig featureConfig = new FeatureConfig();
    private final FeatureInterceptor interceptor = new FeatureInterceptor(featureConfig);

    @Test
    void blocksHabilitationEndpointWhenFeatureIsDisabled() throws Exception {
        featureConfig.setHabilitationsEnabled(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(
                request("/api/habilitations/fiches"), response, new Object());

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(404);
        assertThat(response.getContentAsString()).contains("FEATURE_DISABLED");
    }

    @Test
    void keepsRhEndpointAvailableWhenHabilitationsAreDisabled() throws Exception {
        featureConfig.setHabilitationsEnabled(false);

        boolean allowed = interceptor.preHandle(
                request("/api/autorisation-sorties/mes-demandes"),
                new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }

    @Test
    void allowsHabilitationEndpointWhenFeatureIsEnabled() throws Exception {
        featureConfig.setHabilitationsEnabled(true);

        boolean allowed = interceptor.preHandle(
                request("/api/circuits/actifs"), new MockHttpServletResponse(), new Object());

        assertThat(allowed).isTrue();
    }

    private MockHttpServletRequest request(final String uri) {
        return new MockHttpServletRequest("GET", uri);
    }
}
