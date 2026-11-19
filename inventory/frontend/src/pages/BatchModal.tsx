import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import { formatDate, fromInputDate, toInputDate } from '../api/dates';
import type { StockBatchDto } from '../api/generated';
import { Field } from '../components/Field';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/Notifications';

/** Assign a best-before date to (part of) a batch or correct its counted quantity. */
export function BatchModal({ batch, onClose, onDone }: { batch: StockBatchDto; onClose: () => void; onDone: () => void }) {
  const api = useApi();
  const { notify, fail } = useNotify();
  const [mhdQuantity, setMhdQuantity] = useState(batch.quantity);
  const [bestBefore, setBestBefore] = useState(toInputDate(batch.bestBefore));
  const [count, setCount] = useState(batch.quantity);
  const [busy, setBusy] = useState(false);

  const run = async (action: () => Promise<unknown>, message: string) => {
    setBusy(true);
    try {
      await action();
      notify('success', message);
      onDone();
    } catch (e) {
      await fail(e);
    } finally {
      setBusy(false);
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
            <input
              className="input"
              type="number"
              min={1}
              max={batch.quantity}
              value={mhdQuantity}
              onChange={(e) => setMhdQuantity(Number(e.target.value))}
            />
          </Field>
        </div>
        <div className="column">
          <Field label="MHD">
            <input className="input" type="date" value={bestBefore} onChange={(e) => setBestBefore(e.target.value)} />
          </Field>
        </div>
      </div>
      <button
        className={`button is-primary mb-5${busy ? ' is-loading' : ''}`}
        disabled={mhdQuantity < 1 || mhdQuantity > batch.quantity}
        onClick={() =>
          run(
            () =>
              api.stock.assignBestBefore({
                id: batch.id,
                bestBeforeRequest: { quantity: mhdQuantity, bestBefore: fromInputDate(bestBefore) },
              }),
            'MHD gespeichert',
          )
        }
      >
        MHD speichern
      </button>

      <h3 className="title is-6">Bestand korrigieren (Inventur)</h3>
      <div className="field has-addons">
        <div className="control">
          <input className="input" type="number" min={0} value={count} onChange={(e) => setCount(Number(e.target.value))} />
        </div>
        <div className="control">
          <button
            className={`button is-warning${busy ? ' is-loading' : ''}`}
            disabled={count < 0 || count === batch.quantity}
            onClick={() =>
              run(() => api.stock.correctBatch({ id: batch.id, correctionRequest: { quantity: count } }), 'Bestand korrigiert')
            }
          >
            Korrigieren
          </button>
        </div>
      </div>
    </Modal>
  );
}
