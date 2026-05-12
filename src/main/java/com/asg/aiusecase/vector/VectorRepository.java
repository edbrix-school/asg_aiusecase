package com.asg.aiusecase.vector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class VectorRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public List<InventoryVectorMatch> searchInventory(float[] embedding, int topK, double threshold) {
        String vector = toPgVector(embedding);
        String sql = """
                SELECT id, product_code, stock_code, product_name, description, metadata_json, serialized_json,
                       1 - (embedding_vector <=> ?::vector) AS similarity
                FROM inventory
                WHERE embedding_vector IS NOT NULL
                  AND 1 - (embedding_vector <=> ?::vector) >= ?
                ORDER BY embedding_vector <=> ?::vector
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, this::mapInventory, vector, vector, threshold, vector, topK);
    }

    public List<UnitVectorMatch> searchUnits(float[] embedding, int topK, double threshold) {
        String vector = toPgVector(embedding);
        String sql = """
                SELECT id, inventory_id, unit_code, unit_name, description, metadata_json, serialized_json,
                       1 - (embedding_vector <=> ?::vector) AS similarity
                FROM unit
                WHERE embedding_vector IS NOT NULL
                  AND 1 - (embedding_vector <=> ?::vector) >= ?
                ORDER BY embedding_vector <=> ?::vector
                LIMIT ?
                """;
        return jdbcTemplate.query(sql, this::mapUnit, vector, vector, threshold, vector, topK);
    }

    public void updateInventoryEmbedding(Long id, Map<String, Object> serialized, float[] embedding) {
        String sql = """
                UPDATE inventory
                SET serialized_json = ?::jsonb,
                    embedding_vector = ?::vector,
                    updated_at = now()
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, toJson(serialized), toPgVector(embedding), id);
    }

    public void updateUnitEmbedding(Long id, Map<String, Object> serialized, float[] embedding) {
        String sql = """
                UPDATE unit
                SET serialized_json = ?::jsonb,
                    embedding_vector = ?::vector,
                    updated_at = now()
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, toJson(serialized), toPgVector(embedding), id);
    }

    private InventoryVectorMatch mapInventory(ResultSet rs, int rowNum) throws SQLException {
        return new InventoryVectorMatch(
                rs.getLong("id"),
                rs.getString("product_code"),
                rs.getString("stock_code"),
                rs.getString("product_name"),
                rs.getString("description"),
                parseJson(rs.getString("metadata_json")),
                parseJson(rs.getString("serialized_json")),
                rs.getDouble("similarity")
        );
    }

    private UnitVectorMatch mapUnit(ResultSet rs, int rowNum) throws SQLException {
        return new UnitVectorMatch(
                rs.getLong("id"),
                rs.getLong("inventory_id"),
                rs.getString("unit_code"),
                rs.getString("unit_name"),
                rs.getString("description"),
                parseJson(rs.getString("metadata_json")),
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
