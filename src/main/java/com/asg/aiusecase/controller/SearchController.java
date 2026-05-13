package com.asg.aiusecase.controller;

import com.asg.aiusecase.dto.SearchResponse;
import com.asg.aiusecase.orchestration.InventorySearchOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final InventorySearchOrchestrator orchestrator;

    @PostMapping(value = "/search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<SearchResponse>> search(
            @Valid @RequestPart(value = "query", required = false) String query,
            @RequestPart(value = "meta", required = false) String meta,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        return ResponseEntity.ok(orchestrator.search(query, meta, file));
    }
}
