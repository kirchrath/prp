import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import { formatDate, fromInputDate, toInputDate } from '../api/dates';
import type { StockBatchListDto } from '../api/stock';
import { Field } from '../components/Field';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/Notifications';

/** Assign a best-before date to (part of) a batch or correct its counted quantity. */
export function BatchModal({ batch, onClose, onDone }: { batch: StockBatchListDto; onClose: () => void; onDone: () => void }) {
  const api = useApi();
  const { notify, fail } = useNotify();
  const [mhdQuantity, setMhdQuantity] = useState(batch.quantity);
  const [bestBefore, setBestBefore] = useState(toInputDate(batch.bestBefore));
  const [count, setCount] = useState(batch.quantity);

  /** Loads the current audit (optimistic locking) and runs the change. */
  const run = async (action: (audit: Awaited<ReturnType<typeof api.stock.getBatch>>['audit']) => Promise<unknown>, message: string) => {
    try {
      const { audit } = await api.stock.getBatch({ id: batch.id });
      await action(audit);
      notify('success', message);
      onDone();
    } catch (e) {
      await fail(e);
    }
  };

  return (
    <Modal title={`${batch.articleName} – ${batch.locationPath}`} onClose={onClose}>
      <p className="mb-4">
        Aktuell: <strong>{batch.quantity}</strong> Stück, MHD <strong>{formatDate(batch.bestBefore)}</strong>
      </p>

      <h3 className="title is-6">MHD zuweisen</h3>
      <div className="columns is-mobile">
        <div className="column is-4">
          <Field label="Menge">
            <input className="input" type="number" min={1} max={batch.quantity} value={mhdQuantity}
              onChange={(e) => setMhdQuantity(Number(e.target.value))} />
          </Field>
        </div>
        <div className="column">
          <Field label="MHD">
            <input className="input" type="date" value={bestBefore} onChange={(e) => setBestBefore(e.target.value)} />
          </Field>
        </div>
      </div>
      <button
        className="button is-primary mb-5"
        disabled={mhdQuantity < 1 || mhdQuantity > batch.quantity}
        onClick={() =>
          run((audit) => api.stock.assignBestBefore({
            id: batch.id,
            batchBestBeforeDto: { audit, quantity: mhdQuantity, bestBefore: fromInputDate(bestBefore) },
          }), 'MHD gespeichert')
        }
      >
        MHD speichern
      </button>

      <h3 className="title is-6">Bestand korrigieren (Inventur)</h3>
      <div className="field has-addons">
        <div className="control">
          <input className="input" type="number" min={0} aria-label="Gezählte Menge" value={count}
            onChange={(e) => setCount(Number(e.target.value))} />
        </div>
        <div className="control">
          <button
            className="button is-warning"
            disabled={count < 0 || count === batch.quantity}
            onClick={() =>
              run((audit) => api.stock.correctBatch({ id: batch.id, batchCorrectionDto: { audit, quantity: count } }), 'Bestand korrigiert')
            }
          >
            Korrigieren
          </button>
        </div>
      </div>
    </Modal>
  );
}
