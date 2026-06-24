-- 重置演示用配送员档案：仅保留配送员A/B，编号 PS000001 / PS000002

UPDATE orders
SET picked_by_worker_id = NULL,
    assigned_worker_id = NULL
WHERE merchant_id = 1;

DELETE FROM workers WHERE merchant_id = 1;

UPDATE users
SET nickname = '配送员A',
    phone    = '13800000001'
WHERE openid = 'dev-worker-001'
  AND deleted = 0;

UPDATE users
SET nickname = '配送员B',
    phone    = '13800000002'
WHERE openid = 'dev-worker-002'
  AND deleted = 0;

INSERT INTO workers (merchant_id, user_id, worker_code, name, phone, job_role, status, deleted, created_at, updated_at)
SELECT 1, u.id, 'PS000001', '配送员A', '13800000001', 'DELIVERY', 1, 0, NOW(), NOW()
FROM users u
WHERE u.openid = 'dev-worker-001'
  AND u.deleted = 0
LIMIT 1;

INSERT INTO workers (merchant_id, user_id, worker_code, name, phone, job_role, status, deleted, created_at, updated_at)
SELECT 1, u.id, 'PS000002', '配送员B', '13800000002', 'DELIVERY', 1, 0, NOW(), NOW()
FROM users u
WHERE u.openid = 'dev-worker-002'
  AND u.deleted = 0
LIMIT 1;
