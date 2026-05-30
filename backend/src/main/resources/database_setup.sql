USE expense_tracker_sem;

-- Check if Category table is empty --
SELECT * FROM category;

-- if empty, Insert Categories
-- Expense categories --
INSERT INTO category (category_name, enabled, transaction_type_id) VALUES 
('Other', 1, 1),
('Food', 1, 1),
('Leisure', 1, 1),
('Household', 1, 1),
('Clothing', 1, 1),
('Education', 1, 1),
('Healthcare', 1, 1);

-- Income categories --
INSERT INTO category (category_name, enabled, transaction_type_id) VALUES 
('Other', 1, 2),
('Salary', 1, 2),
('Sales', 1, 2),
('Awards', 1, 2),
('Interest', 1, 2);

-- Assign admin role --
INSERT INTO user_roles (user_id, role_id) VALUES (2, 2);

-- Check if role added --
SELECT * FROM user_roles;

-- Delete admin acc's user role --
DELETE FROM user_roles WHERE user_id = 2 AND role_id = 1;