-- 修复 V13 迁移 checksum（此前手动执行过 ALTER 导致 flyway 校验失败）
UPDATE flyway_schema_history
SET checksum = 1359566003
WHERE version = '13';
