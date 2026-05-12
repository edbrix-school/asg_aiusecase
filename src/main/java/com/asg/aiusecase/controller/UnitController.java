package com.asg.aiusecase.controller;

import com.asg.aiusecase.dto.CreateUnitRequest;
import com.asg.aiusecase.dto.UnitDto;
import com.asg.aiusecase.dto.UpdateUnitRequest;
import com.asg.aiusecase.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    @PostMapping("/unit")
    public ResponseEntity<UnitDto> create(@Valid @RequestBody CreateUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unitService.create(request));
    }

    @PutMapping("/unit/{id}")
    public ResponseEntity<UnitDto> update(@PathVariable Long id, @Valid @RequestBody UpdateUnitRequest request) {
        return ResponseEntity.ok(unitService.update(id, request));
    }

    @GetMapping("/unit/{id}")
    public ResponseEntity<UnitDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(unitService.get(id));
    }

    @GetMapping("/unit")
    public ResponseEntity<List<UnitDto>> listByInventory(@RequestParam Long inventoryId) {
        return ResponseEntity.ok(unitService.listByInventory(inventoryId));
    }
}
