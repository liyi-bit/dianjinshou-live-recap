-- Grant enterprise VIP (level 3) to all existing users, expiring 2027-01-01
UPDATE users
SET vip_level = 3,
    vip_expire_at = '2027-01-01 00:00:00'
WHERE vip_expire_at IS NULL
   OR vip_expire_at < '2027-01-01 00:00:00';
