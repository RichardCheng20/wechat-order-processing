-- 合伙人/档口管理员 → 档口经理
UPDATE users SET role = 'STALL_MANAGER' WHERE role = 'PARTNER_ADMIN';

UPDATE staff_activation_tokens SET target_role = 'STALL_MANAGER' WHERE target_role = 'PARTNER_ADMIN';

UPDATE workers SET job_role = 'STALL_MANAGER' WHERE job_role = 'STALL_ADMIN';
