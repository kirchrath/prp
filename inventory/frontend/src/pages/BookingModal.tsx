import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import { fromInputDate } from '../api/dates';
import type { ArticleDto, LocationDto } from '../api/generated';
import { Field } from '../components/Field';
import { LocationSelect } from '../components/LocationSelect';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/Notifications';

interface Props {
  direction: 'in' | 'out';
  article: ArticleDto;
  locations: LocationDto[];
  initialLocationId?: number;
  onClose: () => void;
  onDone: () => void;
}

/** Manual booking of several units, optionally with best-before date. */
export function BookingModal({ direction, article, locations, initialLocationId, onClose, onDone }: Props) {
  const api = useApi();
  const { notify, fail } = useNotify();
  const [locationId, setLocationId] = useState<number | undefined>(initialLocationId);
  const [quantity, setQuantity] = useState(1);
  const [bestBefore, setBestBefore] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    if (!locationId) return;
    setBusy(true);
    try {
      const bookingRequest = { articleId: article.id, locationId, quantity, bestBefore: fromInputDate(bestBefore) };
      const result =
        direction === 'in' ? await api.stock.bookIn({ bookingRequest }) : await api.stock.bookOut({ bookingRequest });
      notify('success', `${article.name}: ${direction === 'in' ? 'eingelagert' : 'ausgelagert'} – jetzt ${result.quantity} am Ort`);
      onDone();
    } catch (e) {
      await fail(e);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      title={`${article.name} ${direction === 'in' ? 'einlagern' : 'auslagern'}`}
      onClose={onClose}
      footer={
        <>
          <button className="button" onClick={onClose}>
            Abbrechen
          </button>
          <button
            className={`button ${direction === 'in' ? 'is-success' : 'is-link'}${busy ? ' is-loading' : ''}`}
            disabled={!locationId || quantity < 1}
            onClick={submit}
          >
            {direction === 'in' ? 'Einlagern' : 'Auslagern'}
          </button>
        </>
      }
    >
      <Field label="Lagerort">
        <LocationSelect locations={locations} value={locationId} onChange={setLocationId} required />
      </Field>
      <Field label="Menge">
        <input className="input" type="number" min={1} value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} />
      </Field>
      <Field
        label="Mindesthaltbarkeit (optional)"
        help={direction === 'out' ? 'Leer lassen: ältestes MHD zuerst (FIFO)' : undefined}
      >
        <input className="input" type="date" value={bestBefore} onChange={(e) => setBestBefore(e.target.value)} />
      </Field>
    </Modal>
  );
}
