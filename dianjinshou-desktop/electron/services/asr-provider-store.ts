/**
 * Compatibility shim for older renderer code that queried an ASR provider.
 * The desktop client now supports local sherpa-onnx ASR only.
 */
export type AsrProvider = 'local'

export function getAsrProvider(): AsrProvider {
  return 'local'
}

export function setAsrProvider(v: AsrProvider | string): void {
  if (v !== 'local') {
    console.warn('[asr-provider] Cloud ASR is no longer supported; keeping local ASR')
  }
}
