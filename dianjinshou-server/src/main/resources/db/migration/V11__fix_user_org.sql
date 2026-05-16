-- V11: Create default organization for users without one

-- Create default org for users who have no org_id
INSERT INTO organizations (name, max_members, vip_level, created_at, updated_at)
SELECT CONCAT(u.username, '的组织'), 20, 0, NOW(), NOW()
FROM users u
WHERE u.org_id IS NULL
LIMIT 1;

-- Associate first user without org to the new org
UPDATE users u
SET u.org_id = (SELECT MAX(id) FROM organizations)
WHERE u.org_id IS NULL;
