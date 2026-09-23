package com.fatecrepository.seed;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Data
@ConfigurationProperties(prefix = "faterepo.seed")
public class SeedProperties {

    private String mode = "upsert";
    private List<String> files = new ArrayList<>();
}