package com.gui.kline.server.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ModelMapper bean.
 */
@Configuration
public class ModelMapperConfig {
    
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        
        // Use loose matching strategy to handle different property names
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.LOOSE);
        
        // Enable field matching
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);
        
        // Skip null values during mapping
        modelMapper.getConfiguration()
                .setSkipNullEnabled(true);
        
        // Enable automatic type conversion
        modelMapper.getConfiguration()
                .setAmbiguityIgnored(true);
        
        return modelMapper;
    }
}