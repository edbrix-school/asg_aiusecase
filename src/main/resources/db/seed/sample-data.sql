INSERT INTO inventory (product_code, stock_code, product_name, description, metadata_json, serialized_json)
VALUES
('FR001', 'STK-101', 'French Orange', 'Imported orange from France', '{"category":"fruit"}',
 '{"productCode":"FR001","stockCode":"STK-101","productName":"French Orange","description":"Imported orange from France"}'),
('APL001', 'STK-201', 'Apple', 'Fresh red apple', '{"category":"fruit"}',
 '{"productCode":"APL001","stockCode":"STK-201","productName":"Apple","description":"Fresh red apple"}'),
('MLK001', 'STK-301', 'Milk', 'Pasteurized dairy milk', '{"category":"dairy"}',
 '{"productCode":"MLK001","stockCode":"STK-301","productName":"Milk","description":"Pasteurized dairy milk"}')
ON CONFLICT DO NOTHING;

INSERT INTO unit (inventory_id, unit_code, unit_name, description, metadata_json, serialized_json)
SELECT i.id, u.unit_code, u.unit_name, u.description, '{}'::jsonb,
       jsonb_build_object('unitCode', u.unit_code, 'unitName', u.unit_name, 'description', u.description)
FROM inventory i
JOIN (VALUES
    ('APL001', 'BOX', 'Box', 'Packed apple box'),
    ('APL001', 'CRATE', 'Crate', 'Bulk apple crate'),
    ('APL001', 'LTR', 'Liter', 'Invalid liquid unit for apples'),
    ('MLK001', 'LTR', 'Liter', 'Liquid milk liter'),
    ('MLK001', 'CONT', 'Container', 'Milk container'),
    ('MLK001', 'PCS', 'Piece', 'Invalid piece unit for milk'),
    ('FR001', 'BOX', 'Box', 'Orange box'),
    ('FR001', 'CRATE', 'Crate', 'Orange crate')
) AS u(product_code, unit_code, unit_name, description)
ON i.product_code = u.product_code
ON CONFLICT DO NOTHING;

INSERT INTO compatibility_rules (inventory_id, unit_id, is_valid, priority)
SELECT i.id, u.id,
       CASE
           WHEN i.product_code = 'APL001' AND u.unit_code IN ('BOX', 'CRATE') THEN true
           WHEN i.product_code = 'MLK001' AND u.unit_code IN ('LTR', 'CONT') THEN true
           WHEN i.product_code = 'FR001' AND u.unit_code IN ('BOX', 'CRATE') THEN true
           ELSE false
       END,
       CASE u.unit_code WHEN 'BOX' THEN 10 WHEN 'LTR' THEN 10 WHEN 'CRATE' THEN 20 WHEN 'CONT' THEN 20 ELSE 100 END
FROM inventory i
JOIN unit u ON u.inventory_id = i.id
ON CONFLICT DO NOTHING;

INSERT INTO synonym (synonym, actual_value, type, confidence)
VALUES
('orange france', 'French Orange', 'INVENTORY', 0.9500),
('fr orange', 'French Orange', 'INVENTORY', 0.9300),
('apple carton', 'Box', 'UNIT', 0.8800),
('milk bottle', 'Container', 'UNIT', 0.8500),
('ltr', 'Liter', 'UNIT', 0.9800),
('pieces', 'Piece', 'UNIT', 0.9000)
ON CONFLICT DO NOTHING;
