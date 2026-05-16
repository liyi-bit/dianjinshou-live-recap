export function openNativeDatePicker(event: Event) {
  const input = event.currentTarget as (HTMLInputElement & { showPicker?: () => void }) | null
  if (!input) return
  input.focus()
  try {
    input.showPicker?.()
  } catch {
    // Some Chromium builds throw if the picker is already open.
  }
}
