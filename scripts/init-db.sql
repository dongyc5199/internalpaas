-- ========================================
-- 数据库初始化脚本
-- 为各个服务创建独立的数据库
-- ========================================

-- Gitea数据库
CREATE DATABASE gitea;
GRANT ALL PRIVILEGES ON DATABASE gitea TO codehub;

-- Drone CI数据库
CREATE DATABASE drone;
GRANT ALL PRIVILEGES ON DATABASE drone TO codehub;

-- SonarQube数据库
CREATE DATABASE sonarqube;
GRANT ALL PRIVILEGES ON DATABASE sonarqube TO codehub;

-- 连接到SonarQube数据库并设置编码
\c sonarqube
ALTER DATABASE sonarqube SET standard_conforming_strings = off;
