-- V24 建表时把 app_id 声明为 UNIQUE，但与 @TableLogic 的软删除（deleted=1 记录仍在表中）冲突：
-- 用户删除 bot 后，若想重新添加同一 appId，会撞唯一键 → 500。
-- 改为只保留普通索引，唯一性由 Service 层 `findByAppId`（MyBatis Plus 自动带 deleted=0）校验。
ALTER TABLE user_feishu_bots DROP INDEX app_id;
