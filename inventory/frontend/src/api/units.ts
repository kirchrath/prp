/** Short German labels of the ingredient units. */
export const UNIT_LABELS: Record<string, string> = {
  GRAM: 'g',
  KILOGRAM: 'kg',
  MILLILITER: 'ml',
  LITER: 'l',
  PIECE: 'Stück',
  PACKAGE: 'Packung',
  TABLESPOON: 'EL',
  TEASPOON: 'TL',
  PINCH: 'Prise',
  OTHER: '',
};

/** "250 g", "1,5 l", "" without amount. */
export function formatAmount(amount?: number, unit?: string): string {
  if (amount === undefined) return '';
  return `${amount.toLocaleString('de-DE')} ${unit ? UNIT_LABELS[unit] : ''}`.trim();
}
