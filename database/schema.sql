USE `inventory_management_system`;

CREATE TABLE items(
	id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    item_name VARCHAR(50),
    category VARCHAR(50),
    specification VARCHAR(50),
    unit VARCHAR(50),
    amount INT
);