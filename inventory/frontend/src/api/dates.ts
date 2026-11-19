/** Date input value ("2027-01-31") -> Date at local midnight, as the generated clients expect. */
export function fromInputDate(value: string): Date | undefined {
  if (!value) return undefined;
  const [year, month, day] = value.split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function toInputDate(value?: Date): string {
  if (!value) return '';
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`;
}

export function formatDate(value?: Date): string {
  return value ? value.toLocaleDateString('de-DE') : '–';
}

export function formatDateTime(value?: Date): string {
  return value ? value.toLocaleString('de-DE', { dateStyle: 'short', timeStyle: 'short' }) : '–';
}

/** Days from today until the given date (negative = expired). */
export function daysUntil(value: Date, today = new Date()): number {
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  return Math.round((value.getTime() - start.getTime()) / 86_400_000);
}
