package com.bakouan.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class InitialAdminPropertiesTest {
    @Test void deploymentEnvironmentVariablesBindToBootstrapConfiguration() {
        var env = new StandardEnvironment();
        env.getPropertySources().addFirst(new SystemEnvironmentPropertySource("bootstrap-test",Map.of(
                "APP_BOOTSTRAP_ADMIN_ENABLED","true", "APP_BOOTSTRAP_ADMIN_EMAIL","setup@example.test",
                "APP_BOOTSTRAP_ADMIN_PASSWORD","Startup-test-Password", "APP_BOOTSTRAP_ADMIN_TELEPHONE","+22670001122")));
        var properties = Binder.get(env).bind("app.bootstrap.admin", Bindable.of(InitialAdminProperties.class)).get();
        assertThat(properties.isEnabled()).isTrue();assertThat(properties.getEmail()).isEqualTo("setup@example.test");
        assertThat(properties.getPassword()).isEqualTo("Startup-test-Password");assertThat(properties.getTelephone()).isEqualTo("+22670001122");
    }
    @Test void defaultsContainNoSecretAndDoNotChangeExistingDeployments() {
        var properties=new InitialAdminProperties();assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getPassword()).isNull();assertThat(properties.getTelephone()).isNull();
        assertThat(properties.getEmail()).isEqualTo("admin@bsic.bf");
    }
}
