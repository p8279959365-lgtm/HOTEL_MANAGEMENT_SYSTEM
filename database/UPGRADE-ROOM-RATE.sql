USE HOTEL_MANAGEMENT_SYSTEM;

-- Store a custom nightly rate per room while keeping room_types.price_per_night as the default.
-- Existing rooms inherit their current room type rate.
SET @has_room_rate = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rooms' AND COLUMN_NAME = 'room_rate'
);
SET @ddl = IF(@has_room_rate = 0,
    'ALTER TABLE rooms ADD COLUMN room_rate DECIMAL(10,2) NULL AFTER status',
    'SELECT ''rooms.room_rate already exists'' AS message');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE rooms r
JOIN room_types rt ON rt.type_id = r.type_id
SET r.room_rate = rt.price_per_night
WHERE r.room_rate IS NULL;
