-- 回填 recordings.sensitive_word_count / operation_keyword_count
-- 历史完成的整场分析没把这两个数写回 recording 表，导致 AI 助手列表永远显示 0。
-- 取每个 recording 最新一条 full 类型的 analysis_task，按 keywords.type 计数后回填。
UPDATE recordings r
JOIN (
    SELECT recording_id, MAX(id) AS latest_full_id
    FROM analysis_tasks
    WHERE type = 'full' AND deleted = 0
    GROUP BY recording_id
) latest ON latest.recording_id = r.id
LEFT JOIN (
    SELECT task_id,
           SUM(CASE WHEN type = 'sensitive'   THEN 1 ELSE 0 END) AS s_count,
           SUM(CASE WHEN type = 'operational' THEN 1 ELSE 0 END) AS o_count
    FROM keywords
    WHERE deleted = 0
    GROUP BY task_id
) kc ON kc.task_id = latest.latest_full_id
SET
    r.sensitive_word_count    = COALESCE(kc.s_count, 0),
    r.operation_keyword_count = COALESCE(kc.o_count, 0);
