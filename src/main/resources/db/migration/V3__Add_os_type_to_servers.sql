-- Add os_type column to servers table
-- 添加操作系统类型字段到服务器表

ALTER TABLE servers 
ADD COLUMN os_type VARCHAR(20) DEFAULT 'LINUX' AFTER server_type;

-- Add comment for the new column
COMMENT ON COLUMN servers.os_type IS '操作系统类型: LINUX, WINDOWS, MACOS, UNIX, BSD, OTHER';

-- Update existing records to have default value
UPDATE servers SET os_type = 'LINUX' WHERE os_type IS NULL;
