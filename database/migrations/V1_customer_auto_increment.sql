-- V1: Automatisk ID-generering för kunder
-- Behåller befintliga kunder och deras ID:n.

ALTER TABLE customers
MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;