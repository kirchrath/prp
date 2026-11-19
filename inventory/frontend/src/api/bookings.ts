import type { Api } from './ApiContext';
import { BookingDataDtoStatusEnum, type BookingDataDto, type ScanBookingDtoDirectionEnum } from './stock';

export const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

/**
 * Books one scanned unit and waits until the stock service has a final result: unknown barcodes stay
 * PENDING_ARTICLE until the catalog service created the article.
 */
export async function scanAndWait(api: Api, ean: string, locationId: string, direction: 'IN' | 'OUT',
  attempts = 30, wait: (ms: number) => Promise<void> = sleep): Promise<BookingDataDto> {
  const created = await api.bookings.createBookingRaw({
    scanBookingDto: { ean, locationId, direction: direction as ScanBookingDtoDirectionEnum },
  });
  const location = created.raw.headers.get('Location') ?? '';
  const id = location.substring(location.lastIndexOf('/') + 1);
  let booking = (await api.bookings.getBooking({ id })).data;
  for (let i = 1; i < attempts && booking.status === BookingDataDtoStatusEnum.PendingArticle; i++) {
    await wait(1000);
    booking = (await api.bookings.getBooking({ id })).data;
  }
  return booking;
}
