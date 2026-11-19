import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import type { ArticleListDto } from '../api/catalog';
import { fromInputDate } from '../api/dates';
import type { LocationListDto } from '../api/stock';
import { Field } from '../components/Field';
import { LocationSelect } from '../components/LocationSelect';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/Notifications';

interface Props {
  direction: 'in' | 'out';
  article: ArticleListDto;
  locations: LocationListDto[];
  onClose: () => void;
  onDone: () => void;
}

/** Manual booking of several units, optionally with best-before date. */
export function BookingModal({ direction, article, locations, onClose, onDone }: Props) {
  const api = useApi();
  const { notify, fail } = useNotify();
  const [locationId, setLocationId] = useState<string>();
  const [quantity, setQuantity] = useState(1);
  const [bestBefore, setBestBefore] = useState('');
  const isIn = direction === 'in';

  const submit = async () => {
    try {
      const manualBookingDto = { articleId: article.id, locationId: locationId!, quantity, bestBefore: fromInputDate(bestBefore) };
      const result = isIn ? await api.stock.bookIn({ manualBookingDto }) : await api.stock.bookOut({ manualBookingDto });
      notify('success', `${article.name}: ${isIn ? 'eingelagert' : 'ausgelagert'} – jetzt ${result.quantity} am Ort`);
      onDone();
    } catch (e) {
      await fail(e);
    }
  };

  return (
    <Modal
      title={`${article.name} ${isIn ? 'einlagern' : 'auslagern'}`}
      onClose={onClose}
      footer={
        <>
          <button className="button" onClick={onClose}>
            Abbrechen
          </button>
          <button className={`button ${isIn ? 'is-success' : 'is-link'}`} disabled={!locationId || quantity < 1} onClick={submit}>
            {isIn ? 'Einlagern' : 'Auslagern'}
          </button>
        </>
      }
    >
      <Field label="Lagerort">
        <LocationSelect locations={locations} value={locationId} onChange={setLocationId} />
      </Field>
      <Field label="Menge">
        <input className="input" type="number" min={1} value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} />
      </Field>
      <Field label="Mindesthaltbarkeit (optional)" help={isIn ? undefined : 'Leer lassen: ältestes MHD zuerst (FIFO)'}>
        <input className="input" type="date" value={bestBefore} onChange={(e) => setBestBefore(e.target.value)} />
      </Field>
    </Modal>
  );
}
