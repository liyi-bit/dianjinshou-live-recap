import http from './http';

export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface AdminUser {
  id: number;
  username: string;
  phone: string;
  role: string;
  orgId: number | null;
  vipLevel: number;
  status: number;
  createdAt: string;
  lastLoginAt: string | null;
  streamerCount: number | null;
  recordingCount: number | null;
  todayStreamerCount: number | null;
  todayRecordingCount: number | null;
  dailyAiUsed: number | null;
  dailyAiLimit: number | null;
  dailyAiUnlimited: boolean | null;
}

export interface DashboardStats {
  totalUsers: number;
  todayNewUsers: number;
  monthNewUsers: number;
  todayActive: number;
  totalRecordings: number;
  todayRecordings: number;
  totalDuration: number;
  totalTasks: number;
  consumedChars: number;
  avgProcessTime: number;
  paidUsers: number;
  monthRevenue: number;
  pendingRenew: number;
}

export function dashboardStats() {
  return http.get<any, DashboardStats>('/api/v1/admin/dashboard');
}

export function listUsers(params: {
  page?: number;
  size?: number;
  keyword?: string;
  role?: string;
  status?: number;
}) {
  return http.get<any, PageResult<AdminUser>>('/api/v1/admin/users', { params });
}

export interface AdminUserDetail {
  id: number;
  username: string;
  phone: string | null;
  email: string | null;
  role: string;
  orgId: number | null;
  vipLevel: number;
  vipExpireAt: string | null;
  aiQuotaTotal: number;
  aiQuotaUsed: number;
  durationQuotaTotal: number;
  durationQuotaUsed: number;
  status: number;
  lastLoginAt: string | null;
  wechatOpenId: string | null;
  qqOpenId: string | null;
  createdAt: string;
  updatedAt: string;
}

export function userDetail(id: number) {
  return http.get<any, AdminUserDetail>(`/api/v1/admin/users/${id}`);
}

export interface UserRelatedCounts {
  recordingCount: number;
  analysisTaskCount: number;
  fileAnalysisCount: number;
  uploadCount: number;
}

export function userRelatedCounts(id: number) {
  return http.get<any, UserRelatedCounts>(`/api/v1/admin/users/${id}/related-counts`);
}

// ========== Recordings ==========

export interface AdminRecording {
  id: number;
  userId: number;
  username: string | null;
  userPhone: string | null;
  streamerId: number | null;
  streamerName: string | null;
  streamerAvatar: string | null;
  orgId: number | null;
  startTime: string | null;
  endTime: string | null;
  duration: number | null;
  fileSize: number | null;
  status: string | null;
  analysisStatus: string | null;
  errorMsg: string | null;
  createdAt: string;
}

export interface AdminRecordingStats {
  total: number;
  todayCount: number;
  weekCount: number;
  completed: number;
  failed: number;
  recording: number;
  analyzedDone: number;
  analyzedPending: number;
  totalDurationSec: number;
  totalFileBytes: number;
}

export interface AdminRecordingDetail extends AdminRecording {
  localFilePath: string | null;
  localFileName: string | null;
  storageKey: string | null;
  streamUrl: string | null;
  resolution: string | null;
  segmentIndex: number | null;
  sessionId: string | null;
  coreData: string | null;
  sensitiveWordCount: number | null;
  operationKeywordCount: number | null;
  updatedAt: string | null;
  relatedTasks: AdminTask[];
}

export function listRecordings(params: {
  page?: number;
  size?: number;
  userId?: number;
  userPhone?: string;
  analysisStatus?: string;
  start?: string;
  end?: string;
}) {
  return http.get<any, PageResult<AdminRecording>>('/api/v1/admin/recordings', { params });
}

export function recordingStats() {
  return http.get<any, AdminRecordingStats>('/api/v1/admin/recordings/stats');
}

export function recordingDetail(id: number) {
  return http.get<any, AdminRecordingDetail>(`/api/v1/admin/recordings/${id}`);
}

// ========== Tasks ==========

export interface AdminTask {
  id: number;
  taskType: string;
  userId: number;
  username: string | null;
  userPhone: string | null;
  streamerId: number | null;
  streamerName: string | null;
  streamerAvatar: string | null;
  orgId: number | null;
  subType: string | null;
  status: string | null;
  aiModel: string | null;
  errorMsg: string | null;
  resource: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
}

export interface AdminTaskStats {
  byType: Record<string, number>;
  byStatus: Record<string, number>;
  todayCount: number;
  failedCount: number;
  pendingCount: number;
  completedCount: number;
}

export interface AdminTaskDetail {
  id: number;
  taskType: string;
  userId: number;
  username: string | null;
  orgId: number | null;
  recordingId: number | null;
  recordingName: string | null;
  subType: string | null;
  status: string | null;
  priority: number | null;
  aiModel: string | null;
  industry: string | null;
  fileName: string | null;
  fileSize: number | null;
  duration: number | null;
  totalParts: number | null;
  uploadedParts: number | null;
  storageKey: string | null;
  asrText: string | null;
  asrWordCount: number | null;
  aiResult: string | null;
  aiDiagnosis: string | null;
  keywordSummary: string | null;
  sensitiveWords: string | null;
  sensitiveCount: number | null;
  contentCompass: string | null;
  optimizedText: string | null;
  optimizationAction: string | null;
  optimizationGoal: string | null;
  summary: string | null;
  consumedChars: number | null;
  errorMsg: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string;
  updatedAt: string | null;
}

export function listTasks(params: {
  page?: number;
  size?: number;
  taskType?: string;
  userId?: number;
  userPhone?: string;
  status?: string;
  start?: string;
  end?: string;
}) {
  return http.get<any, PageResult<AdminTask>>('/api/v1/admin/tasks', { params });
}

export function taskStats() {
  return http.get<any, AdminTaskStats>('/api/v1/admin/tasks/stats');
}

export function taskDetail(type: string, id: number) {
  return http.get<any, AdminTaskDetail>(`/api/v1/admin/tasks/${type}/${id}`);
}
