/**
 * 录制恢复服务
 *
 * App 重启后，检测后端中卡在 status="recording" 的孤儿记录：
 * - 如果本地存在有效的录制文件（>1MB）→ FLV 先 remux 为 MP4 → 调 completeRecording 触发分析
 * - 如果文件不存在或太小 → 标记为 interrupted
 */

import { backendApi } from './backend-api'
import { remuxFlvToMp4 } from './remux-service'
import { LocalAsrService } from './local-asr-service'
import { getAsrModelManager } from './asr-model-manager'
import { existsSync, statSync, readdirSync } from 'fs'
import { join, extname, basename } from 'path'

const MIN_VALID_FILE_SIZE = 1 * 1024 * 1024 // 1MB — 小于此视为无效录制

/**
 * 在 App 启动后调用，恢复所有被中断的录制记录。
 * 需要先设置好 authToken 再调用。
 */
export async function recoverOrphanedRecordings(): Promise<void> {
  try {
    const orphaned = await backendApi.getOrphanedRecordings()
    if (orphaned.length === 0) {
      console.log('[Recovery] No orphaned recordings found')
      return
    }

    console.log(`[Recovery] Found ${orphaned.length} orphaned recording(s), recovering...`)

    for (const record of orphaned) {
      try {
        await recoverSingleRecording(record)
      } catch (err) {
        console.error(`[Recovery] Failed to recover recording ${record.id}:`, (err as Error).message)
      }
    }

    console.log('[Recovery] Recovery complete')
  } catch (err) {
    console.error('[Recovery] Recovery process failed:', (err as Error).message)
  }
}

async function recoverSingleRecording(record: {
  id: number
  localFilePath: string | null
  localFileName: string | null
}): Promise<void> {
  const { id, localFilePath, localFileName } = record

  // 关键：只处理"本机路径存在"的 recording。路径不在本机 = 这条是别的设备在录，
  // 绝不能擅自标 failed（否则会把其他电脑正在进行的录制改状态）
  if (!localFilePath) {
    console.log(`[Recovery] Recording ${id}: no localFilePath, skip (likely other device)`)
    return
  }
  // localFilePath 可能是目录也可能是文件全路径 —— 都要能定位到本机目录
  const dirPath = /\.(mp4|flv)$/i.test(localFilePath) ? join(localFilePath, '..') : localFilePath
  if (!existsSync(dirPath)) {
    console.log(`[Recovery] Recording ${id}: path not on this device (${dirPath}), skip`)
    return
  }

  // Try to find the recording file
  const found = findRecordingFile(dirPath, localFileName)

  if (found) {
    let finalPath = found.path
    let finalName = found.name
    let finalSize = found.size

    // FLV → MP4 remux (HTML5 video 不支持 FLV)
    if (extname(found.path).toLowerCase() === '.flv') {
      const mp4Path = found.path.replace(/\.flv$/i, '.mp4')
      console.log(`[Recovery] Recording ${id}: remuxing FLV → MP4...`)
      const remuxResult = await remuxFlvToMp4(found.path, mp4Path)
      finalPath = remuxResult
      finalName = basename(remuxResult)
      try {
        finalSize = statSync(remuxResult).size
      } catch {
        // keep original size
      }
    }

    console.log(`[Recovery] Recording ${id}: completing with ${finalName} (${(finalSize / 1024 / 1024).toFixed(1)}MB)`)
    await backendApi.completeRecording(id, {
      localFilePath: finalPath,
      localFileName: finalName,
      fileSize: finalSize,
      duration: found.estimatedDuration,
      status: 'completed',
    })
    console.log(`[Recovery] Recording ${id}: marked as completed, kicking off ASR`)

    // 与 recording-service 实时路径保持一致：completeRecording 后立刻启动 ASR + 提交逐字稿。
    // 没有这一步，recording 会停在 analysis_status='transcribing' 永远不动（即"逐字稿生成中"）。
    runAsrAfterRecovery(id, finalPath).catch((err) => {
      console.error(`[Recovery] Recording ${id}: post-recovery ASR failed:`, (err as Error).message)
    })
  } else {
    // 本机目录存在但没有有效文件 → 确实是本机录制被中断
    console.log(`[Recovery] Recording ${id}: local dir exists but no valid file, marking as failed`)
    await backendApi.completeRecording(id, {
      status: 'failed',
      errorMsg: '录制被中断（应用重启），未找到有效的录制文件',
    })
  }
}

/**
 * Search for the recording file in the output directory.
 * Files are named like: {anchorName}_{datetime}.mp4 or .flv
 */
function findRecordingFile(
  dirPath: string | null,
  fileNameHint: string | null
): { path: string; name: string; size: number; estimatedDuration: number } | null {
  if (!dirPath || !existsSync(dirPath)) return null

  try {
    const files = readdirSync(dirPath)
    // Look for .mp4 first, then .flv — prefer the most recently modified
    const mediaFiles = files
      .filter((f) => {
        const ext = extname(f).toLowerCase()
        return ext === '.mp4' || ext === '.flv'
      })
      .map((f) => {
        const fullPath = join(dirPath, f)
        try {
          const stats = statSync(fullPath)
          return { path: fullPath, name: f, size: stats.size, mtime: stats.mtimeMs }
        } catch {
          return null
        }
      })
      .filter((f): f is NonNullable<typeof f> => f !== null && f.size >= MIN_VALID_FILE_SIZE)
      .sort((a, b) => b.mtime - a.mtime) // Most recent first

    if (mediaFiles.length === 0) return null

    // If we have a filename hint, prefer matching files
    if (fileNameHint) {
      const matched = mediaFiles.find((f) => f.name.includes(fileNameHint.replace(/[^a-zA-Z0-9\u4e00-\u9fff]/g, '')))
      if (matched) {
        return { ...matched, estimatedDuration: estimateDuration(matched.size) }
      }
    }

    // Otherwise pick the most recent file
    const best = mediaFiles[0]
    return { ...best, estimatedDuration: estimateDuration(best.size) }
  } catch {
    return null
  }
}

/** Rough duration estimate: ~500KB/s for typical live streams */
function estimateDuration(fileSize: number): number {
  return Math.round(fileSize / (500 * 1024))
}

/**
 * recoverSingleRecording 完成 recording 后立即启动 ASR。
 * 与 RecordingService.runAsrAndSubmit 行为一致：跑本地 ASR → submitAsrResult(autoAnalyze=false)。
 * 失败仅打日志，不影响其他恢复项。
 */
async function runAsrAfterRecovery(recordingId: number, mp4Path: string): Promise<void> {
  const mgr = getAsrModelManager()
  if (!mgr.isReady()) {
    const ok = await mgr.ensureModelReady()
    if (!ok) {
      console.warn(`[Recovery] Recording ${recordingId}: ASR model missing, leaving status=transcribing for next-launch recovery`)
      return
    }
  }
  console.log(`[Recovery] Recording ${recordingId}: starting LOCAL ASR for ${mp4Path}`)
  const localAsr = new LocalAsrService()
  const segments = await localAsr.transcribe(mp4Path)
  if (!segments || segments.length === 0) {
    console.warn(`[Recovery] Recording ${recordingId}: ASR produced 0 segments, marking failed`)
    await backendApi.markAsrFailed(recordingId, 'ASR 未识别到任何内容')
    return
  }
  const res = await backendApi.submitAsrResult({
    recordingId,
    segments,
    autoAnalyze: false,
  })
  console.log(`[Recovery] Recording ${recordingId}: transcript submitted, taskId=${res?.taskId} status=${res?.status}`)
}

/**
 * v1.1.0：恢复 analysis_status='transcribing' 的 ASR 孤儿（软件关闭时 ASR 没跑完的）。
 * - 本地 MP4 还在 → 续跑 ASR → 提交逐字稿（autoAnalyze=false，停在 transcribed 等手动 AI）
 * - 本地 MP4 丢失 → 调 markAsrFailed 标 failed
 * recoverOrphanedRecordings 之后调用，共享同一个触发点。
 */
export async function recoverTranscribingRecordings(): Promise<void> {
  try {
    const pending = await backendApi.listPendingAsr()
    if (!pending || pending.length === 0) {
      console.log('[AsrRecovery] No transcribing orphans found')
      return
    }
    console.log(`[AsrRecovery] Found ${pending.length} transcribing orphan(s), recovering...`)

    // 内置模型缺失时放弃，本次记录暂留 transcribing，重装客户端后下次启动再试。
    const mgr = getAsrModelManager()
    if (!mgr.isReady()) {
      await mgr.ensureModelReady()
      console.warn('[AsrRecovery] Bundled ASR model missing, leaving orphans as-is')
      return
    }

    for (const r of pending) {
      const { id, localFilePath, localFileName } = r as any
      try {
        const mp4Path = resolveMp4Path(localFilePath, localFileName)
        if (!mp4Path) {
          console.log(`[AsrRecovery] Recording ${id}: local file missing, marking failed`)
          await backendApi.markAsrFailed(id, '本地视频文件已丢失，无法重新生成逐字稿')
          continue
        }
        console.log(`[AsrRecovery] Recording ${id}: resuming ASR from ${mp4Path}`)
        const localAsr = new LocalAsrService()
        const segments = await localAsr.transcribe(mp4Path)
        if (segments.length === 0) {
          console.warn(`[AsrRecovery] Recording ${id}: ASR produced 0 segments, marking failed`)
          await backendApi.markAsrFailed(id, 'ASR 未识别到任何内容')
          continue
        }
        const res = await backendApi.submitAsrResult({
          recordingId: id,
          segments,
          autoAnalyze: false,
        })
        console.log(`[AsrRecovery] Recording ${id}: submitted, taskId=${res?.taskId} status=${res?.status}`)
      } catch (err) {
        console.error(`[AsrRecovery] Recording ${id} recovery failed:`, (err as Error).message)
      }
    }
    console.log('[AsrRecovery] ASR recovery complete')
  } catch (err) {
    console.error('[AsrRecovery] Recovery process failed:', (err as Error).message)
  }
}

/** 从后端返回的 localFilePath/localFileName 还原可用的 MP4 文件绝对路径。 */
function resolveMp4Path(localFilePath: string | null | undefined, localFileName: string | null | undefined): string | null {
  if (!localFilePath) return null
  // localFilePath 可能是完整文件路径，也可能是目录
  if (/\.mp4$/i.test(localFilePath) && existsSync(localFilePath)) {
    try {
      if (statSync(localFilePath).size >= MIN_VALID_FILE_SIZE) return localFilePath
    } catch {}
  }
  const dir = /\.(mp4|flv)$/i.test(localFilePath) ? join(localFilePath, '..') : localFilePath
  if (!existsSync(dir)) return null
  if (localFileName) {
    const candidate = join(dir, localFileName)
    if (existsSync(candidate)) {
      try {
        if (statSync(candidate).size >= MIN_VALID_FILE_SIZE) return candidate
      } catch {}
    }
  }
  return null
}
