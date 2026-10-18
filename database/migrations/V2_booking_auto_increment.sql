-- V2: Automatisk ID-generering för bokningar
-- Behåller befintliga bokningar och deras relationer.

-- 1. Ta tillfälligt bort Foreign Keys mot bookings
ALTER TABLE booking_services
DROP FOREIGN KEY FK1etky587qu1tqlr3t1r7w59gx;

ALTER TABLE invoice_lines
DROP FOREIGN KEY FKie4hcu97t7gl093i9aiqom635;

-- 2. Aktivera AUTO_INCREMENT
ALTER TABLE bookings
MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT;

-- 3. Återskapa relationen för booking_services
ALTER TABLE booking_services
ADD CONSTRAINT FK1etky587qu1tqlr3t1r7w59gx
FOREIGN KEY (booking_id)
REFERENCES bookings(id);

-- 4. Återskapa relationen för invoice_lines
ALTER TABLE invoice_lines
ADD CONSTRAINT FKie4hcu97t7gl093i9aiqom635
FOREIGN KEY (booking_id)
REFERENCES bookings(id);