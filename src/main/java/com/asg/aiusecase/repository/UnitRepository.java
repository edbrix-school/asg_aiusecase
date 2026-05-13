package com.asg.aiusecase.repository;

import com.asg.aiusecase.entity.UnitEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class UnitRepository {

    private final JdbcTemplate commonDbJdbcTemplate;

    public UnitRepository(@Qualifier("commonDbJdbcTemplate") JdbcTemplate commonDbJdbcTemplate) {
        this.commonDbJdbcTemplate = commonDbJdbcTemplate;
    }

    public Optional<UnitEntity> findById(Long stockUnitPoid) {
        String sql = """
                SELECT STOCK_UNIT_POID, STOCK_UNIT_CODE, STOCK_UNIT_NAME, ACTIVE, DELETED
                FROM STOCK_UNIT_MASTER
                WHERE STOCK_UNIT_POID = ?
                FETCH FIRST 1 ROWS ONLY
                """;
        return commonDbJdbcTemplate.query(sql, this::mapStockUnit, stockUnitPoid).stream().findFirst();
    }

    public Optional<UnitEntity> findFirstByStockUnitCodeIgnoreCase(String stockUnitCode) {
        String sql = """
                SELECT STOCK_UNIT_POID, STOCK_UNIT_CODE, STOCK_UNIT_NAME, ACTIVE, DELETED
                FROM STOCK_UNIT_MASTER
                WHERE lower(STOCK_UNIT_CODE) = lower(?)
                  AND COALESCE(ACTIVE, 'Y') = 'Y'
                  AND COALESCE(DELETED, 'N') = 'N'
                FETCH FIRST 1 ROWS ONLY
                """;
        return commonDbJdbcTemplate.query(sql, this::mapStockUnit, stockUnitCode).stream().findFirst();
    }

    public Optional<UnitEntity> findFirstByStockUnitNameIgnoreCase(String stockUnitName) {
        String sql = """
                SELECT STOCK_UNIT_POID, STOCK_UNIT_CODE, STOCK_UNIT_NAME, ACTIVE, DELETED
                FROM STOCK_UNIT_MASTER
                WHERE lower(STOCK_UNIT_NAME) = lower(?)
                  AND COALESCE(ACTIVE, 'Y') = 'Y'
                  AND COALESCE(DELETED, 'N') = 'N'
                FETCH FIRST 1 ROWS ONLY
                """;
        return commonDbJdbcTemplate.query(sql, this::mapStockUnit, stockUnitName).stream().findFirst();
    }

    public List<UnitEntity> findByActiveIgnoreCaseAndDeletedIgnoreCase(String active, String deleted) {
        String sql = """
                SELECT STOCK_UNIT_POID, STOCK_UNIT_CODE, STOCK_UNIT_NAME, ACTIVE, DELETED
                FROM STOCK_UNIT_MASTER
                WHERE lower(COALESCE(ACTIVE, 'Y')) = lower(?)
                  AND lower(COALESCE(DELETED, 'N')) = lower(?)
                """;
        return commonDbJdbcTemplate.query(sql, this::mapStockUnit, active, deleted);
    }

    private UnitEntity mapStockUnit(ResultSet rs, int rowNum) throws SQLException {
        UnitEntity unit = new UnitEntity();
        unit.setStockUnitPoid(rs.getLong("STOCK_UNIT_POID"));
        unit.setStockUnitCode(rs.getString("STOCK_UNIT_CODE"));
        unit.setStockUnitName(rs.getString("STOCK_UNIT_NAME"));
        unit.setActive(rs.getString("ACTIVE"));
        unit.setDeleted(rs.getString("DELETED"));
        return unit;
    }
}
