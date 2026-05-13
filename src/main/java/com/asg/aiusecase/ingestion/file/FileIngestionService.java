package com.asg.aiusecase.ingestion.file;

import com.asg.aiusecase.config.AppProperties;
import com.asg.aiusecase.ingestion.InventoryLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileIngestionService {

    private final List<FileProcessor> processors;
    private final AppProperties properties;

    public List<InventoryLine> ingest(MultipartFile file, String sourceLabel) {
        if (file == null || file.isEmpty()) {
            return List.of();
        }
        validateFile(file);
        String contentType = file.getContentType();
        return processors.stream()
                .filter(processor -> processor.supports(contentType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported file type: " + contentType))
                .process(file, sourceLabel);
    }

    private void validateFile(MultipartFile file) {
        long maxSize = properties.getIngestion().getMaxFileSizeBytes();
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File too large (max " + maxSize + " bytes)");
        }
        if (file.getContentType() == null || file.getContentType().isBlank()) {
            throw new IllegalArgumentException("Missing file content type");
        }
    }
}
