package com.asg.aiusecase.dto;

import com.asg.aiusecase.vector.InventoryVectorMatch;
import com.asg.aiusecase.vector.UnitVectorMatch;

import java.util.List;

public record VectorSearchSnapshot(
        List<InventoryVectorMatch> inventories,
        List<UnitVectorMatch> units
) {
}
