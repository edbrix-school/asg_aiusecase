package com.asg.aiusecase.ingestion.parser;

import com.asg.aiusecase.ingestion.InventoryLine;
import com.asg.aiusecase.ingestion.SearchOptions;

import java.util.List;

public record ParsedQuery(
        List<InventoryLine> lines,
        SearchOptions defaults
) {
}
