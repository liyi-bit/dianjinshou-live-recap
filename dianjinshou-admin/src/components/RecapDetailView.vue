<script setup lang="ts">
import { computed, ref } from 'vue';
import type { AdminTaskDetail } from '../api';
import { formatDateTime, formatDuration } from '../utils/format';
import AiDimensions from './AiDimensions.vue';
import AiDiagnosis from './AiDiagnosis.vue';
import AiCompass from './AiCompass.vue';
import AiKeywords from './AiKeywords.vue';

interface Props {
  task: AdminTaskDetail;
  /** 可选：显示在主播栏的标题。默认用 username/taskType */
  displayName?: string;
  /** 可选：开始时间 */
  startTime?: string | null;
  /** 可选：结束时间 */
  endTime?: string | null;
  /** 可选：时长（秒） */
  duration?: number | null;
  /** 可选：主播 ID，显示在标签 */
  streamerId?: number | null;
  /** 是否为切片（影响文案）默认 false */
  isClip?: boolean;
}
const props = defineProps<Props>();

const contentTab = ref<'paragraphs' | 'script' | 'optimized'>('paragraphs');

// 把 asrText 按 \n\n 或 \n 切分为伪段落；无换行就整段
const paragraphs = computed(() => {
  const text = props.task.asrText;
  if (!text) return [] as Array<{ idx: number; startLabel: string; textContent: string }>;
  const raw = text.includes('\n\n') ? text.split(/\n\n+/) : text.split(/\n+/);
  const list = raw.map(s => s.trim()).filter(Boolean);
  if (list.length <= 1) {
    // 单段：按句号粗分（仅展示，非精确时间戳）
    const parts = text.split(/(?<=[。！？!?])\s*/).map(s => s.trim()).filter(Boolean);
    if (parts.length > 1) {
      return parts.map((t, i) => ({
        idx: i,
        startLabel: `#${String(i + 1).padStart(2, '0')}`,
        textContent: t
      }));
    }
    return [{ idx: 0, startLabel: '#01', textContent: text }];
  }
  return list.map((t, i) => ({
    idx: i,
    startLabel: `#${String(i + 1).padStart(2, '0')}`,
    textContent: t
  }));
});

const fmtDate = (s: string | null | undefined) => {
  return formatDateTime(s);
};

const streamerName = computed(() => props.displayName || props.task.username || '—');
const streamerInitial = computed(() => streamerName.value.charAt(0).toUpperCase());

const durationLabel = computed(() => {
  const s = props.duration ?? props.task.duration ?? 0;
  return formatDuration(s);
});

function isEmptyTab() {
  if (contentTab.value === 'script') return !props.task.aiResult;
  if (contentTab.value === 'optimized') return !props.task.optimizedText;
  return false;
}
</script>

<template>
  <div class="recap-detail">
    <!-- 2. 主播信息栏 -->
    <div class="pg-title">
      <div class="pt-main">
        <div class="av av-lg av-1">{{ streamerInitial }}</div>
        <div class="pt-info">
          <div class="pt-name">{{ streamerName }}</div>
          <div class="pt-tags">
            <span class="tg tg-b">{{ task.taskType === 'analysis' ? '录制分析' : (task.taskType === 'file_analysis' ? '文件分析' : '分块上传') }}</span>
            <span v-if="streamerId" class="tg tg-or">主播 #{{ streamerId }}</span>
            <span v-if="task.subType" class="tg tg-or">{{ task.subType }}</span>
          </div>
        </div>
      </div>
      <div class="pt-actions">
        <span class="admin-readonly">管理员 · 只读视图</span>
      </div>
    </div>

    <!-- 2.5 AI 状态 Banner -->
    <div
      v-if="['transcribing','transcribed','ai_processing','AI_PROCESSING','failed','FAILED'].includes(task.status || '')"
      class="ai-analyze-banner"
      :class="{
        'banner-transcribed': task.status === 'transcribed',
        'banner-processing': task.status === 'ai_processing' || task.status === 'AI_PROCESSING' || task.status === 'transcribing',
        'banner-failed': task.status === 'failed' || task.status === 'FAILED'
      }"
    >
      <div class="ab-left">
        <template v-if="task.status === 'transcribing'">
          <span class="ab-icon">⏳</span>
          <div class="ab-text">
            <div class="ab-title">逐字稿生成中</div>
            <div class="ab-sub">ASR 转写处理中，完成后可查看完整内容</div>
          </div>
        </template>
        <template v-else-if="task.status === 'transcribed'">
          <span class="ab-icon">📝</span>
          <div class="ab-text">
            <div class="ab-title">逐字稿已生成（未分析）</div>
            <div class="ab-sub">等待用户手动触发 AI 复盘</div>
          </div>
        </template>
        <template v-else-if="task.status === 'ai_processing' || task.status === 'AI_PROCESSING'">
          <span class="ab-icon">🤖</span>
          <div class="ab-text">
            <div class="ab-title">AI 分析中</div>
            <div class="ab-sub">通常需要 30 秒到 2 分钟</div>
          </div>
        </template>
        <template v-else-if="task.status === 'failed' || task.status === 'FAILED'">
          <span class="ab-icon">⚠️</span>
          <div class="ab-text">
            <div class="ab-title">AI 分析失败</div>
            <div class="ab-sub">{{ task.errorMsg || '未提供错误原因' }}</div>
          </div>
        </template>
      </div>
    </div>

    <!-- 3. 时间 & 敏感词信息栏 -->
    <div class="info-bar">
      <div class="ib-item"><span class="lbl">开始</span><span class="val">{{ fmtDate(startTime) }}</span></div>
      <div class="ib-item"><span class="lbl">结束</span><span class="val">{{ fmtDate(endTime) }}</span></div>
      <div class="ib-item dur"><span class="lbl">时长</span><span class="val">{{ durationLabel }}</span></div>
      <div class="ib-item" v-if="task.aiModel"><span class="lbl">AI 模型</span><span class="val">{{ task.aiModel }}</span></div>
      <div class="ib-item" v-if="task.asrWordCount != null"><span class="lbl">ASR 字数</span><span class="val">{{ task.asrWordCount.toLocaleString() }}</span></div>
      <div class="ib-sens">
        <span class="d" />
        <span>敏感词 {{ task.sensitiveCount ?? 0 }}次</span>
      </div>
    </div>

    <!-- 4. 主体区域 -->
    <div class="main-body">
      <!-- 左列：视频占位 -->
      <div class="video-col">
        <div class="video-title-bar">
          <span class="vt-label">{{ isClip ? '切片回放' : '直播画面' }}</span>
          <span class="video-readonly-hint">管理员只读</span>
        </div>
        <div class="video-wrapper">
          <div class="video-empty">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M10 9l5 3-5 3z"/>
            </svg>
            <div class="video-empty-text">只读模式<br />不提供视频播放</div>
          </div>
        </div>
      </div>

      <!-- 右列 -->
      <div class="content-col">
        <!-- Tab 栏 -->
        <div class="content-tab-bar">
          <div class="ct-left">
            <span class="ct-tab" :class="{ active: contentTab === 'paragraphs' }" @click="contentTab = 'paragraphs'">分钟段落</span>
            <span class="ct-tab" :class="{ active: contentTab === 'script' }" @click="contentTab = 'script'">AI 脚本拆解</span>
            <span class="ct-tab" :class="{ active: contentTab === 'optimized' }" @click="contentTab = 'optimized'">优化原文</span>
          </div>
        </div>

        <!-- 内容区 -->
        <div class="content-body" :class="{ 'content-body--full': contentTab !== 'paragraphs' }">
          <!-- 分钟段落 -->
          <div v-if="contentTab === 'paragraphs'" class="paragraphs-view">
            <div v-if="paragraphs.length === 0" class="empty-state">
              <div class="empty-ico">📝</div>
              <div class="empty-txt">暂无 ASR 段落数据</div>
            </div>
            <div v-for="p in paragraphs" :key="p.idx" class="para-item">
              <div class="para-avatar">{{ streamerInitial }}</div>
              <div class="para-content">
                <span class="para-time">{{ p.startLabel }}</span>
                <div class="para-text">{{ p.textContent }}</div>
              </div>
            </div>
          </div>

          <!-- AI 脚本拆解 / 维度评分 -->
          <div v-if="contentTab === 'script'" class="tab-content">
            <div class="tab-notice">注意：AI 基于千万级场次数据训练，按 12 个维度对直播脚本进行评分。</div>
            <div v-if="isEmptyTab()" class="ai-empty-state">
              <div class="empty-ico">🧭</div>
              <div class="empty-txt">AI 尚未分析或无脚本拆解数据</div>
            </div>
            <div v-else class="json-rendered">
              <AiDimensions :json="task.aiResult" />
            </div>
          </div>

          <!-- 优化原文 -->
          <div v-if="contentTab === 'optimized'" class="tab-content">
            <div class="tab-notice">注意：优化原文是经过 AI 整理，在原文基础上进行了一定的话术优化，作为参考稿件。</div>
            <div v-if="isEmptyTab()" class="ai-empty-state">
              <div class="empty-ico">✍️</div>
              <div class="empty-txt">AI 尚未分析或无优化原文</div>
            </div>
            <template v-else>
              <pre class="ai-block">{{ task.optimizedText }}</pre>
            </template>
          </div>
        </div>

        <!-- 底部：AI 诊断 / 摘要 / 关键词 / 内容罗盘 -->
        <div
          v-if="contentTab === 'paragraphs' && (task.aiDiagnosis || task.summary || task.keywordSummary || task.contentCompass || task.sensitiveWords)"
          class="bottom-area"
        >
          <div class="ba-tabs">
            <div v-if="task.aiDiagnosis" class="ba-section">
              <div class="ba-title">AI 诊断</div>
              <div class="ba-body">
                <AiDiagnosis :json="task.aiDiagnosis" />
              </div>
            </div>
            <div v-if="task.summary" class="ba-section">
              <div class="ba-title">摘要</div>
              <div class="prose">{{ task.summary }}</div>
            </div>
            <div v-if="task.keywordSummary" class="ba-section">
              <div class="ba-title">关键词</div>
              <div class="ba-body">
                <AiKeywords :json="task.keywordSummary" />
              </div>
            </div>
            <div v-if="task.contentCompass" class="ba-section">
              <div class="ba-title">内容罗盘</div>
              <div class="ba-body">
                <AiCompass :json="task.contentCompass" />
              </div>
            </div>
            <div v-if="task.sensitiveWords" class="ba-section">
              <div class="ba-title">敏感词</div>
              <div class="prose">{{ task.sensitiveWords }}</div>
            </div>
            <div v-if="task.optimizationAction || task.optimizationGoal" class="ba-section">
              <div class="ba-title">优化建议</div>
              <div v-if="task.optimizationAction" class="prose">动作：{{ task.optimizationAction }}</div>
              <div v-if="task.optimizationGoal" class="prose">目标：{{ task.optimizationGoal }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.recap-detail {
  display: flex;
  flex-direction: column;
  height: calc(100vh - var(--header-height) - 60px);
  overflow: hidden;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: var(--radius);
}

/* 主播信息栏 */
.pg-title {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--line);
  flex-wrap: wrap;
  flex-shrink: 0;
}
.pg-title .pt-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 200px;
}
.pg-title .pt-info { display: flex; flex-direction: column; gap: 3px }
.pg-title .pt-name {
  font-size: 15px; font-weight: 700;
  color: var(--text-1);
  letter-spacing: -.01em; line-height: 1.2;
}
.pg-title .pt-tags { display: flex; gap: 4px; flex-wrap: wrap; align-items: center }
.pg-title .pt-actions {
  display: flex; gap: 7px; margin-left: auto;
}
.admin-readonly {
  font-size: 11px; letter-spacing: .1em;
  color: var(--text-3); text-transform: uppercase;
  background: var(--bg-2); padding: 4px 10px;
  border-radius: var(--radius-pill); font-weight: 600;
}

/* Avatar */
.av {
  width: 36px; height: 36px; border-radius: 9px;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 700; color: #fff;
  flex-shrink: 0; position: relative; overflow: hidden;
}
.av::after {
  content: ''; position: absolute; inset: 0;
  background: linear-gradient(180deg,rgba(255,255,255,.22),transparent 55%);
  border-radius: inherit;
}
.av-lg { width: 46px; height: 46px; font-size: 16px; border-radius: 12px }
.av-1 {
  background:
    radial-gradient(circle at 30% 25%, #E8BE74 0%, transparent 45%),
    linear-gradient(140deg, #D09E4E 0%, #8F6224 100%);
}

/* Tag */
.tg {
  display: inline-flex; align-items: center; height: 20px;
  padding: 0 9px; border-radius: var(--radius-pill);
  font-size: 11px; font-weight: 650; letter-spacing: .02em;
}
.tg-b { background: var(--blue2-soft); color: var(--blue2) }
.tg-or { background: var(--orange-soft); color: var(--orange) }

/* AI Banner */
.ai-analyze-banner {
  margin: 14px 16px 0; padding: 12px 16px;
  border-radius: var(--radius-md);
  display: flex; justify-content: space-between; align-items: center;
  gap: 16px; border: 1px solid transparent;
  flex-shrink: 0;
}
.ai-analyze-banner.banner-transcribed {
  background: rgba(224, 123, 0, 0.08); border-color: rgba(224, 123, 0, 0.3);
}
.ai-analyze-banner.banner-processing {
  background: rgba(52, 145, 250, 0.08); border-color: rgba(52, 145, 250, 0.3);
}
.ai-analyze-banner.banner-failed {
  background: rgba(245, 63, 63, 0.08); border-color: rgba(245, 63, 63, 0.3);
}
.ai-analyze-banner .ab-left { display: flex; align-items: center; gap: 14px; flex: 1 }
.ai-analyze-banner .ab-icon { font-size: 26px; line-height: 1 }
.ai-analyze-banner .ab-title { font-size: 13.5px; font-weight: 700; margin-bottom: 3px }
.ai-analyze-banner .ab-sub { font-size: 12px; color: var(--text-2b) }

/* Info Bar */
.info-bar {
  display: flex; align-items: center; gap: 0;
  border-bottom: 1px solid var(--line);
  padding: 10px 16px;
  flex-wrap: wrap;
  flex-shrink: 0;
}
.info-bar .ib-item {
  display: flex; align-items: center; gap: 6px;
  font-size: 12.5px; color: var(--text-3);
  padding: 0 14px; border-right: 1px solid var(--line-2);
  font-family: var(--fm);
}
.info-bar .ib-item:first-child { padding-left: 0 }
.info-bar .ib-item .lbl { color: var(--text-3); font-family: var(--ff) }
.info-bar .ib-item .val { color: var(--text-2); font-weight: 600 }
.info-bar .ib-item.dur .val { color: var(--brand); font-weight: 700 }
.info-bar .ib-sens {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 3px 10px; background: var(--red-soft);
  border-radius: var(--radius-pill);
  font-size: 12px; color: var(--red); margin-left: auto;
}
.info-bar .ib-sens .d {
  width: 6px; height: 6px; background: var(--red); border-radius: 50%;
}

/* 主体 */
.main-body {
  display: flex; flex: 1; min-height: 0; overflow: hidden;
}

/* 视频列 */
.video-col {
  width: 300px; flex-shrink: 0;
  display: flex; flex-direction: column;
  border-right: 1px solid var(--line);
}
.video-title-bar {
  height: 32px; padding: 0 12px;
  display: flex; align-items: center; justify-content: space-between;
  background: var(--text-1); flex-shrink: 0;
}
.vt-label { font-size: 12px; color: #fff; font-weight: 500 }
.video-readonly-hint {
  font-size: 10px; color: rgba(255,255,255,.5);
  letter-spacing: .05em; text-transform: uppercase;
}
.video-wrapper {
  flex: 1; background: #000;
  display: flex; align-items: center; justify-content: center;
  min-height: 0;
}
.video-empty {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 12px; color: rgba(255, 255, 255, 0.3);
}
.video-empty-text {
  font-size: 11px; letter-spacing: .15em;
  text-align: center; line-height: 1.8;
  text-transform: uppercase;
}

/* 内容列 */
.content-col {
  flex: 1; display: flex; flex-direction: column;
  min-width: 0; overflow: hidden;
}

/* Tab 栏 */
.content-tab-bar {
  height: 38px; padding: 0 16px;
  display: flex; align-items: center;
  border-bottom: 1px solid var(--line);
  flex-shrink: 0; background: var(--bg-2);
}
.ct-left { display: flex; gap: 0 }
.ct-tab {
  padding: 0 14px; height: 38px; line-height: 36px;
  font-size: 13px; color: var(--text-3); cursor: pointer;
  white-space: nowrap;
  border: 1px solid var(--line); border-bottom: none;
  border-radius: 4px 4px 0 0;
  margin-right: -1px; background: var(--hov);
  margin-top: 2px;
}
.ct-tab.active {
  color: var(--text-1); font-weight: 500;
  background: var(--card);
  border-bottom: 2px solid var(--brand);
}
.ct-tab:hover:not(.active) { color: var(--brand) }

/* 内容区 */
.content-body { flex: 1; overflow-y: auto; min-height: 0 }
.content-body.content-body--full { flex: 1 }

.tab-notice {
  padding: 12px 16px;
  font-size: 12px; color: var(--text-3); line-height: 1.5;
  background: var(--bg); border-bottom: 1px solid var(--line);
}

.tab-content {
  height: 100%;
  display: flex; flex-direction: column;
}

.ai-empty-state {
  flex: 1;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 48px 16px; gap: 10px;
}

/* 分钟段落 */
.paragraphs-view { padding: 12px 16px }
.para-item {
  display: flex; gap: 10px; padding: 10px 0;
  border-radius: 6px;
  border-bottom: 1px dashed var(--line-2);
}
.para-item:last-child { border-bottom: none }
.para-avatar {
  flex-shrink: 0;
  width: 28px; height: 28px; border-radius: 50%;
  background: linear-gradient(135deg, var(--brand), var(--brand-dark));
  color: #fff; font-size: 11px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
  margin-top: 2px;
}
.para-content { flex: 1; min-width: 0 }
.para-time {
  font-size: 11px; color: var(--text-3);
  font-family: var(--fm);
}
.para-text {
  font-size: 13px; line-height: 1.6;
  color: var(--text-1); margin-top: 4px;
}

.empty-state {
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 60px 20px; gap: 8px;
}
.empty-ico { font-size: 36px; opacity: .5 }
.empty-txt { color: var(--text-3); font-size: 13px }

.ai-block {
  margin: 16px;
  background: var(--bg-2);
  border: 1px solid var(--line);
  border-radius: var(--radius-md);
  padding: 14px;
  font-size: 13px;
  font-family: var(--fm);
  color: var(--text-1);
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}
.ai-block.small { margin: 12px 16px; font-size: 12px }

.json-rendered {
  padding: 16px;
}

.ba-body {
  margin-top: 4px;
}

/* 底部诊断 */
.bottom-area {
  border-top: 1px solid var(--line);
  background: var(--card);
  flex-shrink: 0;
  max-height: 320px;
  overflow-y: auto;
}
.ba-tabs { padding: 12px 0 }
.ba-section {
  padding: 8px 16px;
  border-bottom: 1px dashed var(--line-2);
}
.ba-section:last-child { border-bottom: none }
.ba-title {
  font-size: 11px; font-weight: 700;
  letter-spacing: .15em; text-transform: uppercase;
  color: var(--text-3); margin-bottom: 6px;
  display: flex; align-items: center; gap: 6px;
}
.ba-title::before {
  content: ''; width: 3px; height: 3px; border-radius: 50%;
  background: var(--brand);
}
.prose {
  font-size: 13px; line-height: 1.7;
  color: var(--text-1);
  margin-bottom: 4px;
}
</style>
