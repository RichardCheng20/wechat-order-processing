-- 修复 V20 迁移 checksum（V20 SQL 已精简，与库中已应用结构一致）
UPDATE flyway_schema_history
SET checksum = 1034090751,
    success = 1
WHERE version = '20';
