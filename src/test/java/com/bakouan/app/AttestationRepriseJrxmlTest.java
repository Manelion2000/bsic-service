package com.bakouan.app;

import net.sf.jasperreports.engine.JasperCompileManager;
import org.junit.jupiter.api.Test;

class AttestationRepriseJrxmlTest {
    @Test
    void compileAttestationRepriseTemplate() throws Exception {
        JasperCompileManager.compileReport("src/main/resources/reports/attestation-reprise-service.jrxml");
    }
}
