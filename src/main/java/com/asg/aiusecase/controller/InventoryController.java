package com.asg.aiusecase.controller;

import com.asg.aiusecase.dto.CreateInventoryRequest;
import com.asg.aiusecase.dto.InventoryDto;
import com.asg.aiusecase.dto.UpdateInventoryRequest;
import com.asg.aiusecase.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/inventory")
    public ResponseEntity<InventoryDto> create(@Valid @RequestBody CreateInventoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventoryService.create(request));
    }

    @PutMapping("/inventory/{id}")
    public ResponseEntity<InventoryDto> update(@PathVariable Long id, @Valid @RequestBody UpdateInventoryRequest request) {
        return ResponseEntity.ok(inventoryService.update(id, request));
    }

    @GetMapping("/inventory/{id}")
    public ResponseEntity<InventoryDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryService.get(id));
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<InventoryDto>> list() {
        return ResponseEntity.ok(inventoryService.list());
    }
}
