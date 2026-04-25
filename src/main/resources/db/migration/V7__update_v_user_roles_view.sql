CREATE OR REPLACE VIEW `v_user_roles` AS
SELECT
    user_id,
    username,
    name,
    role_history,
    student_id,
    CASE
        WHEN role_history LIKE '%超级管理员%' THEN 'admin'
        WHEN role_history LIKE '%主任%' OR role_history LIKE '%部长%' THEN 'minister'
        ELSE 'member'
    END AS role_level
FROM `user`;
