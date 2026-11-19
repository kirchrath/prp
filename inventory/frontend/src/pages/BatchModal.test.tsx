import { fireEvent, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { apiError, audit, fakeApi, renderWith } from '../test/utils';
import { BatchModal } from './BatchModal';

const batch = { id: 'b1', articleId: 'a', articleName: 'Joghurt', locationId: 'k', locationPath: 'Kühlschrank', bestBefore: new Date(2027, 1, 3), quantity: 3 };

describe('BatchModal', () => {
  it('assigns a best-before date and corrects the quantity', async () => {
    const api = fakeApi();
    api.stock.getBatch.mockResolvedValue({ audit: audit(2) });
    const onDone = vi.fn();
    renderWith(<BatchModal batch={batch} onClose={vi.fn()} onDone={onDone} />, api);

    expect(screen.getByDisplayValue('2027-02-03')).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('MHD'), { target: { value: '2027-03-01' } });
    fireEvent.change(screen.getByLabelText('Menge'), { target: { value: '4' } });
    expect(screen.getByText('MHD speichern')).toBeDisabled();
    fireEvent.change(screen.getByLabelText('Menge'), { target: { value: '0' } });
    expect(screen.getByText('MHD speichern')).toBeDisabled();
    fireEvent.change(screen.getByLabelText('Menge'), { target: { value: '2' } });
    await userEvent.click(screen.getByText('MHD speichern'));
    expect(api.stock.assignBestBefore).toHaveBeenCalledWith({
      id: 'b1', batchBestBeforeDto: { audit: audit(2), quantity: 2, bestBefore: new Date(2027, 2, 1) },
    });

    expect(screen.getByText('Korrigieren')).toBeDisabled();
    fireEvent.change(screen.getByLabelText('Gezählte Menge'), { target: { value: '-1' } });
    expect(screen.getByText('Korrigieren')).toBeDisabled();
    fireEvent.change(screen.getByLabelText('Gezählte Menge'), { target: { value: '1' } });
    await userEvent.click(screen.getByText('Korrigieren'));
    expect(api.stock.correctBatch).toHaveBeenCalledWith({ id: 'b1', batchCorrectionDto: { audit: audit(2), quantity: 1 } });
    expect(onDone).toHaveBeenCalledTimes(2);
  });

  it('reports conflicts', async () => {
    const api = fakeApi();
    api.stock.getBatch.mockRejectedValue(apiError(409));
    renderWith(<BatchModal batch={{ ...batch, bestBefore: undefined }} onClose={vi.fn()} onDone={vi.fn()} />, api);
    expect(screen.getByText('–')).toBeInTheDocument();
    await userEvent.click(screen.getByText('MHD speichern'));
    expect(await screen.findByText(/neu laden/)).toBeInTheDocument();
  });
});
