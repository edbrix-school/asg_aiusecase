package com.asg.aiusecase.ingestion.file;

import com.asg.aiusecase.ingestion.InventoryLine;
import com.asg.aiusecase.ingestion.SearchOptions;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class TextFileProcessor implements FileProcessor {

    @Override
    public boolean supports(String contentType) {
        return contentType != null && contentType.equalsIgnoreCase("text/plain");
    }

    @Override
    public List<InventoryLine> process(MultipartFile file, String sourceLabel) {
        List<InventoryLine> lines = new ArrayList<>();
        if (file == null || file.isEmpty()) {
            return lines;
        }
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            for (String raw : content.split("\\r?\\n")) {
                if (raw == null) {
                    continue;
                }
                String normalized = raw.trim();
                if (normalized.isBlank()) {
                    continue;
                }
                lines.add(new InventoryLine(
                        UUID.randomUUID().toString(),
                        raw,
                        normalized,
                        sourceLabel,
                        SearchOptions.empty()
                ));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read text file", e);
        }
        return lines;
    }
}
