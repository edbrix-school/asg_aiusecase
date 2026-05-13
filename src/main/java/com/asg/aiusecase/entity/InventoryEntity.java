package com.asg.aiusecase.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class InventoryEntity {

    private Long stockPoid;
    private String stockCode;
    private String stockName;
    private String stockName2;
    private String stockDescription;
    private String active;
    private String deleted;
    private Map<String, Object> serializedJson = new LinkedHashMap<>();

    public Long getId() {
        return stockPoid;
    }

    public Map<String, Object> getMetadataJson() {
        return Map.of();
    }
}
