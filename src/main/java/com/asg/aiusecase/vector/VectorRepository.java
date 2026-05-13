package com.asg.aiusecase.vector;

import com.asg.aiusecase.entity.InventoryEntity;
import com.asg.aiusecase.entity.UnitEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class VectorRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public VectorRepository(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<InventoryVectorMatch> searchInventory(float[] embedding, int topK, double threshold) {
        String vector = toPgVector(embedding);
        String sql = """
                SELECT stock_poid AS id,
                       stock_code,
                       stock_name,
                       stock_description,
                       serialized_json,
                       1 - (embedding_vector <=> ?::vector) AS similarity
                FROM stock_vector_embedding
                WHERE embedding_vector IS NOT NULL
                  AND COALESCE(active, 'Y') = 'Y'
                  AND COALESCE(deleted, 'N') = 'N'
                  AND 1 - (embedding_vector <=> ?::vector) >= ?
                ORDER BY embedding_vector <=> ?::vector
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, this::mapInventory, vector, vector, threshold, vector, topK);
    }

    public List<UnitVectorMatch> searchUnits(float[] embedding, int topK, double threshold) {
        String vector = toPgVector(embedding);
        String sql = """
                SELECT stock_unit_poid AS id,
                       stock_unit_code,
                       stock_unit_name,
                       serialized_json,
                       1 - (embedding_vector <=> ?::vector) AS similarity
                FROM stock_unit_vector_embedding
                WHERE embedding_vector IS NOT NULL
                  AND COALESCE(active, 'Y') = 'Y'
                  AND COALESCE(deleted, 'N') = 'N'
                  AND 1 - (embedding_vector <=> ?::vector) >= ?
                ORDER BY embedding_vector <=> ?::vector
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, this::mapUnit, vector, vector, threshold, vector, topK);
    }

    public void upsertStockEmbedding(InventoryEntity stock, Map<String, Object> serialized, float[] embedding) {
        String sql = """
                INSERT INTO stock_vector_embedding (
                    stock_poid, stock_code, stock_name, stock_name2, stock_description,
                    active, deleted, serialized_json, embedding_vector, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::vector, now())
                ON CONFLICT (stock_poid) DO UPDATE
                SET stock_code = EXCLUDED.stock_code,
                    stock_name = EXCLUDED.stock_name,
                    stock_name2 = EXCLUDED.stock_name2,
                    stock_description = EXCLUDED.stock_description,
                    active = EXCLUDED.active,
                    deleted = EXCLUDED.deleted,
                    serialized_json = EXCLUDED.serialized_json,
                    embedding_vector = EXCLUDED.embedding_vector,
                    updated_at = now()
                """;
        jdbcTemplate.update(sql,
                stock.getStockPoid(),
                stock.getStockCode(),
                stock.getStockName(),
                stock.getStockName2(),
                stock.getStockDescription(),
                stock.getActive(),
                stock.getDeleted(),
                toJson(serialized),
                toPgVector(embedding));
    }

    public void upsertStockUnitEmbedding(UnitEntity stockUnit, Map<String, Object> serialized, float[] embedding) {
        String sql = """
                INSERT INTO stock_unit_vector_embedding (
                    stock_unit_poid, stock_unit_code, stock_unit_name,
                    active, deleted, serialized_json, embedding_vector, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?::jsonb, ?::vector, now())
                ON CONFLICT (stock_unit_poid) DO UPDATE
                SET stock_unit_code = EXCLUDED.stock_unit_code,
                    stock_unit_name = EXCLUDED.stock_unit_name,
                    active = EXCLUDED.active,
                    deleted = EXCLUDED.deleted,
                    serialized_json = EXCLUDED.serialized_json,
                    embedding_vector = EXCLUDED.embedding_vector,
                    updated_at = now()
                """;
        jdbcTemplate.update(sql,
                stockUnit.getStockUnitPoid(),
                stockUnit.getStockUnitCode(),
                stockUnit.getStockUnitName(),
                stockUnit.getActive(),
                stockUnit.getDeleted(),
                toJson(serialized),
                toPgVector(embedding));
    }

    public void deleteStockEmbedding(Long stockPoid) {
        jdbcTemplate.update("DELETE FROM stock_vector_embedding WHERE stock_poid = ?", stockPoid);
    }

    public void deleteStockUnitEmbedding(Long stockUnitPoid) {
        jdbcTemplate.update("DELETE FROM stock_unit_vector_embedding WHERE stock_unit_poid = ?", stockUnitPoid);
    }

    private InventoryVectorMatch mapInventory(ResultSet rs, int rowNum) throws SQLException {
        return new InventoryVectorMatch(
                rs.getLong("id"),
                rs.getString("stock_code"),
                rs.getString("stock_name"),
                rs.getString("stock_description"),
                Map.of(),
                parseJson(rs.getString("serialized_json")),
                rs.getDouble("similarity")
        );
    }

    private UnitVectorMatch mapUnit(ResultSet rs, int rowNum) throws SQLException {
        return new UnitVectorMatch(
                rs.getLong("id"),
                rs.getString("stock_unit_code"),
                rs.getString("stock_unit_name"),
                Map.of(),
                parseJson(rs.getString("serialized_json")),
                rs.getDouble("similarity")
        );
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private String toJson(Map<String, Object> json) {
        try {
            return objectMapper.writeValueAsString(json == null ? Map.of() : json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize JSON", e);
        }
    }

    private static String toPgVector(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding vector must not be empty");
        }
        return Arrays.stream(toDoubleArray(embedding))
                .mapToObj(Double::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static double[] toDoubleArray(float[] values) {
        double[] converted = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            converted[i] = values[i];
        }
        return converted;
    }
}
