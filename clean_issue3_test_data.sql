-- ============================================
-- CLEANUP: DELETE TEST DATA SAFELY
-- ============================================

SET FOREIGN_KEY_CHECKS=0;

-- Delete user_roles for test users (must go first due to FK)
DELETE FROM user_roles WHERE user_id IN (
    SELECT id FROM users WHERE username LIKE 'testuser%'
);

-- Delete test users
DELETE FROM users WHERE username LIKE 'testuser%';

-- Delete test categories
DELETE FROM category WHERE category_name LIKE 'Test%';

SET FOREIGN_KEY_CHECKS=1;

-- Verify cleanup
SELECT COUNT(*) as 'Remaining Users' FROM users;
SELECT COUNT(*) as 'Remaining Categories' FROM category;
