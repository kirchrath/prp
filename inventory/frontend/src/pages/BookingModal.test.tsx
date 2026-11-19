import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, fakeApi, loc, renderWith } from '../test/utils';
import { BookingModal } from './BookingModal';

const article = { id: 'a1', name: 'Reis', minStock: 0, status: 'ACTIVE', eans: [] } as never;

describe('BookingModal', () => {
  it('books in with best-before date', async () => {
    const api = fakeApi();
    api.stock.bookIn.mockResolvedValue({ quantity: 4 });
    const onDone = vi.fn();
    renderWith(<BookingModal direction="in" article={article} locations={[loc('k', 'Keller')]} onClose={vi.fn()} onDone={onDone} />, api);
    expect(screen.getByText('Einlagern')).toBeDisabled();
    await userEvent.selectOptions(screen.getByLabelText('Lagerort'), 'k');
    fireEvent.change(screen.getByLabelText('Menge'), { target: { value: '0' } });
    expect(screen.getByText('Einlagern')).toBeDisabled();
    fireEvent.change(screen.getByLabelText('Menge'), { target: { value: '2' } });
    fireEvent.change(screen.getByLabelText('Mindesthaltbarkeit (optional)'), { target: { value: '2027-01-02' } });
    await userEvent.click(screen.getByText('Einlagern'));
    expect(api.stock.bookIn).toHaveBeenCalledWith({
      manualBookingDto: { articleId: 'a1', locationId: 'k', quantity: 2, bestBefore: new Date(2027, 0, 2) },
    });
    expect(await screen.findByText(/jetzt 4 am Ort/)).toBeInTheDocument();
    expect(onDone).toHaveBeenCalled();
  });

  it('books out and reports errors', async () => {
    const api = fakeApi();
    api.stock.bookOut.mockRejectedValueOnce(apiError(409, 'Nicht genug')).mockResolvedValueOnce({ quantity: 0 });
    const onClose = vi.fn();
    renderWith(<BookingModal direction="out" article={article} locations={[loc('k', 'Keller')]} onClose={onClose} onDone={vi.fn()} />, api);
    expect(screen.getByText(/FIFO/)).toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText('Lagerort'), 'k');
    await userEvent.click(screen.getByText('Auslagern'));
    expect(await screen.findByText('Nicht genug')).toBeInTheDocument();
    await userEvent.click(screen.getByText('Auslagern'));
    expect(await screen.findByText(/ausgelagert/)).toBeInTheDocument();
    await userEvent.click(screen.getByText('Abbrechen'));
    expect(onClose).toHaveBeenCalled();
  });
});
