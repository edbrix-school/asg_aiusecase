package com.asg.aiusecase.repository;

import com.asg.aiusecase.entity.InventoryEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class InventoryRepository {

    private final JdbcTemplate commonDbJdbcTemplate;

    public InventoryRepository(@Qualifier("commonDbJdbcTemplate") JdbcTemplate commonDbJdbcTemplate) {
        this.commonDbJdbcTemplate = commonDbJdbcTemplate;
    }

    public Optional<InventoryEntity> findById(Long stockPoid) {
        String sql = """
                SELECT STOCK_POID, STOCK_CODE, STOCK_NAME, STOCK_NAME2, STOCK_DESCRIPTION, ACTIVE, DELETED
                FROM STOCK_MASTER
                WHERE STOCK_POID = ?
                FETCH FIRST 1 ROWS ONLY
                """;
        return commonDbJdbcTemplate.query(sql, this::mapStock, stockPoid).stream().findFirst();
    }

    public Optional<InventoryEntity> findFirstByStockCodeIgnoreCase(String stockCode) {
        String sql = """
                SELECT STOCK_POID, STOCK_CODE, STOCK_NAME, STOCK_NAME2, STOCK_DESCRIPTION, ACTIVE, DELETED
                FROM STOCK_MASTER
                WHERE lower(STOCK_CODE) = lower(?)
                  AND COALESCE(ACTIVE, 'Y') = 'Y'
                  AND COALESCE(DELETED, 'N') = 'N'
                FETCH FIRST 1 ROWS ONLY
                """;
        return commonDbJdbcTemplate.query(sql, this::mapStock, stockCode).stream().findFirst();
    }

    public Optional<InventoryEntity> findFirstByStockNameIgnoreCase(String stockName) {
        String sql = """
                SELECT STOCK_POID, STOCK_CODE, STOCK_NAME, STOCK_NAME2, STOCK_DESCRIPTION, ACTIVE, DELETED
                FROM STOCK_MASTER
                WHERE lower(STOCK_NAME) = lower(?)
                  AND COALESCE(ACTIVE, 'Y') = 'Y'
                  AND COALESCE(DELETED, 'N') = 'N'
                FETCH FIRST 1 ROWS ONLY
                """;
        return commonDbJdbcTemplate.query(sql, this::mapStock, stockName).stream().findFirst();
    }

    public List<InventoryEntity> findByActiveIgnoreCaseAndDeletedIgnoreCase(String active, String deleted) {
        String sql = """
                SELECT STOCK_POID, STOCK_CODE, STOCK_NAME, STOCK_NAME2, STOCK_DESCRIPTION, ACTIVE, DELETED
                FROM STOCK_MASTER
                WHERE lower(COALESCE(ACTIVE, 'Y')) = lower(?)
                  AND lower(COALESCE(DELETED, 'N')) = lower(?)
                """;
        return commonDbJdbcTemplate.query(sql, this::mapStock, active, deleted);
    }

    public List<InventoryEntity> findAllActiveNotDeleted() {
        String sql = """
                SELECT STOCK_POID, STOCK_CODE, STOCK_NAME, STOCK_NAME2, STOCK_DESCRIPTION, ACTIVE, DELETED
                FROM STOCK_MASTER
                WHERE COALESCE(ACTIVE, 'Y') = 'Y'
                  AND COALESCE(DELETED, 'N') = 'N'
                ORDER BY STOCK_POID
                """;
        return commonDbJdbcTemplate.query(sql, this::mapStock);
    }

    private InventoryEntity mapStock(ResultSet rs, int rowNum) throws SQLException {
        InventoryEntity stock = new InventoryEntity();
        stock.setStockPoid(rs.getLong("STOCK_POID"));
        stock.setStockCode(rs.getString("STOCK_CODE"));
        stock.setStockName(rs.getString("STOCK_NAME"));
        stock.setStockName2(rs.getString("STOCK_NAME2"));
        stock.setStockDescription(rs.getString("STOCK_DESCRIPTION"));
        stock.setActive(rs.getString("ACTIVE"));
        stock.setDeleted(rs.getString("DELETED"));
        return stock;
    }
}
