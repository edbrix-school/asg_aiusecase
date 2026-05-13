package com.asg.aiusecase.ingestion.file;

import com.asg.aiusecase.ingestion.InventoryLine;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileProcessor {
    boolean supports(String contentType);

    List<InventoryLine> process(MultipartFile file, String sourceLabel);
}
