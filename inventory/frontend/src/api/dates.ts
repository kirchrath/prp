import { parseDate, serializeDate } from './generated/runtime';

/** Date input value ("2027-01-31") -> Date at local midnight, as the generated client expects. */
export function fromInputDate(value: string): Date | undefined {
  return value ? parseDate(value) : undefined;
}

export function toInputDate(value?: Date | null): string {
  return value ? serializeDate(value) : '';
}

export function formatDate(value?: Date | null): string {
  return value ? value.toLocaleDateString('de-DE') : '–';
}

export function formatDateTime(value?: Date | null): string {
  return value ? value.toLocaleString('de-DE', { dateStyle: 'short', timeStyle: 'short' }) : '–';
}

/** Days from today until the given date (negative = expired). */
export function daysUntil(value: Date): number {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return Math.round((value.getTime() - today.getTime()) / 86_400_000);
}
