import { describe, expect, it } from 'vitest';
import { formatAmount } from './units';

describe('formatAmount', () => {
  it('formats amounts with units', () => {
    expect(formatAmount(undefined, 'GRAM')).toBe('');
    expect(formatAmount(1.5, 'LITER')).toBe('1,5 l');
    expect(formatAmount(2, 'OTHER')).toBe('2');
    expect(formatAmount(3)).toBe('3');
  });
});
