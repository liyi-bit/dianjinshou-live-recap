-- Add 'comparison' to ai_conversations.assistant_type enum
ALTER TABLE ai_conversations MODIFY COLUMN assistant_type ENUM('operation','compliance','script','comparison') NOT NULL;
