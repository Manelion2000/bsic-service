package com.bakouan.app.controller;

import com.bakouan.app.config.FeatureConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/features")
public class FeatureController {

    private final FeatureConfig featureConfig;

    @GetMapping
    public Map<String, Boolean> getFeatures() {
        return Map.of("habilitations", featureConfig.isHabilitationsEnabled());
    }
}
