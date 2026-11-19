import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import { daysUntil, formatDate } from '../api/dates';
import type { StockBatchListDto } from '../api/stock';
import { ArticleImage } from '../components/ArticleImage';
import { LocationSelect } from '../components/LocationSelect';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';
import { BatchModal } from './BatchModal';

function expiryTag(bestBefore: Date): string {
  const days = daysUntil(bestBefore);
  if (days < 0) return 'is-danger';
  return days <= 7 ? 'is-warning' : 'is-light';
}

export function StockPage() {
  const api = useApi();
  const { fail } = useNotify();
  const [locationId, setLocationId] = useState<string>();
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<StockBatchListDto>();
  const locations = useAsync(() => api.locations.listLocations(), [api]);
  const stock = useAsync(() => api.stock.listBatches({ locationId }), [api, locationId]);

  const term = search.trim().toLowerCase();
  const rows = (stock.data ?? []).filter(
    (b) => b.articleName.toLowerCase().includes(term) || (b.articleBrand ?? '').toLowerCase().includes(term),
  );

  const change = async (batch: StockBatchListDto, delta: 1 | -1) => {
    const manualBookingDto = { articleId: batch.articleId, locationId: batch.locationId, quantity: 1, bestBefore: batch.bestBefore };
    try {
      await (delta > 0 ? api.stock.bookIn({ manualBookingDto }) : api.stock.bookOut({ manualBookingDto }));
      stock.reload();
    } catch (e) {
      await fail(e);
    }
  };

  let content;
  if (stock.error) {
    content = <ErrorBox message={stock.error} onRetry={stock.reload} />;
  } else if (!stock.data) {
    content = <Loading />;
  } else if (rows.length === 0) {
    content = <Empty text="Kein Bestand gefunden." />;
  } else {
    content = (
      <div className="table-container box p-0">
        <table className="table is-fullwidth is-hoverable">
          <thead>
            <tr>
              <th />
              <th>Artikel</th>
              <th>Lagerort</th>
              <th>MHD</th>
              <th className="has-text-right">Menge</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {rows.map((b) => (
              <tr key={b.id}>
                <td style={{ width: 56 }}>
                  <ArticleImage url={b.articleImageUrl} size={40} />
                </td>
                <td>
                  <strong>{b.articleName}</strong>
                  {b.articleBrand && <small className="has-text-grey"> · {b.articleBrand}</small>}
                </td>
                <td>{b.locationPath}</td>
                <td>{b.bestBefore ? <span className={`tag ${expiryTag(b.bestBefore)}`}>{formatDate(b.bestBefore)}</span> : '–'}</td>
                <td className="has-text-right has-text-weight-bold">{b.quantity}</td>
                <td className="has-text-right">
                  <div className="buttons are-small is-right is-flex-wrap-nowrap">
                    <button className="button is-link is-light" aria-label={`${b.articleName} eins entnehmen`} onClick={() => change(b, -1)}>
                      −
                    </button>
                    <button className="button is-success is-light" aria-label={`${b.articleName} eins hinzufügen`} onClick={() => change(b, 1)}>
                      +
                    </button>
                    <button className="button is-light" aria-label={`${b.articleName} bearbeiten`} onClick={() => setEditing(b)}>
                      ✎
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <>
      <h1 className="title">Bestand</h1>
      <div className="columns">
        <div className="column is-5">
          <LocationSelect locations={locations.data ?? []} value={locationId} onChange={setLocationId} emptyLabel="Alle Lagerorte" />
        </div>
        <div className="column">
          <input className="input" type="search" placeholder="Artikel suchen…" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
      </div>
      {content}
      {editing && (
        <BatchModal
          batch={editing}
          onClose={() => setEditing(undefined)}
          onDone={() => {
            setEditing(undefined);
            stock.reload();
          }}
        />
      )}
    </>
  );
}
