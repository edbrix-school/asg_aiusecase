package com.asg.aiusecase.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class UnitEntity {

    private Long stockUnitPoid;
    private String stockUnitCode;
    private String stockUnitName;
    private String active;
    private String deleted;
    private Map<String, Object> serializedJson = new LinkedHashMap<>();

    public Long getId() {
        return stockUnitPoid;
    }

    public String getUnitCode() {
        return stockUnitCode;
    }

    public String getUnitName() {
        return stockUnitName;
    }

    public Map<String, Object> getMetadataJson() {
        return Map.of();
    }
}
