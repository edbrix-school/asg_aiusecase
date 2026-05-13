package com.asg.aiusecase.controller;

import com.asg.aiusecase.dto.PublishEmbeddingEventRequest;
import com.asg.aiusecase.messaging.InventoryEvent;
import com.asg.aiusecase.messaging.InventoryEventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryEventPublisher eventPublisher;

    @PostMapping("/stock/embedding-events")
    public ResponseEntity<Map<String, Object>> publishEmbeddingEvent(@Valid @RequestBody PublishEmbeddingEventRequest request) {
        InventoryEvent event = new InventoryEvent(request.type(), request.stockPoid(), request.stockUnitPoid());
        eventPublisher.publish(event);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "eventId", event.eventId(),
                "status", "PUBLISHED"
        ));
    }
}
