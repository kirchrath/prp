/**
 * Formal check of article numbers, same rules as the services (Gtin.java): only digits, EAN-8, UPC-A (12),
 * EAN-13 or GTIN-14 with a valid check digit; UPC-E (8 digits) when it is not a valid EAN-8.
 * Normalized: UPC-A/UPC-E and GTIN-14 with a leading 0 become EAN-13, EAN-8 stays EAN-8.
 */
export const INVALID_GTIN = 'Ungültige EAN: erlaubt sind 8, 12, 13 oder 14 Ziffern mit gültiger Prüfziffer';

function hasValidCheckDigit(code: string): boolean {
  let sum = 0;
  for (let i = code.length - 2, weight = 3; i >= 0; i--, weight = 4 - weight) {
    sum += Number(code[i]) * weight;
  }
  return (10 - (sum % 10)) % 10 === Number(code[code.length - 1]);
}

const checked = (code: string) => (hasValidCheckDigit(code) ? code : undefined);

function upcE(code: string): string | undefined {
  const system = code[0];
  if (system !== '0' && system !== '1') return undefined;
  const d = code.substring(1, 7);
  const last = d[5];
  const body =
    last <= '2' ? d.substring(0, 2) + last + '0000' + d.substring(2, 5)
    : last === '3' ? d.substring(0, 3) + '00000' + d.substring(3, 5)
    : last === '4' ? d.substring(0, 4) + '00000' + d[4]
    : d.substring(0, 5) + '0000' + last;
  return checked('0' + system + body + code[7]);
}

/** Normalized code, or undefined when the code is not a formally valid GTIN. */
export function normalizeGtin(raw: string): string | undefined {
  const code = raw.trim();
  if (!/^\d+$/.test(code)) return undefined;
  switch (code.length) {
    case 8:
      return checked(code) ?? upcE(code);
    case 12:
      return checked('0' + code);
    case 13:
      return checked(code);
    case 14:
      return checked(code[0] === '0' ? code.substring(1) : code);
    default:
      return undefined;
  }
}
