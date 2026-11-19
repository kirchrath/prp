import { useMemo, useState } from 'react';
import { useApi } from '../api/ApiContext';
import { daysUntil, formatDate } from '../api/dates';
import type { StockBatchDto } from '../api/generated';
import { ArticleImage } from '../components/ArticleImage';
import { LocationSelect } from '../components/LocationSelect';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';
import { BatchModal } from './BatchModal';

export function StockPage() {
  const api = useApi();
  const { fail } = useNotify();
  const [locationId, setLocationId] = useState<number>();
  const [search, setSearch] = useState('');
  const [editing, setEditing] = useState<StockBatchDto>();
  const locations = useAsync(() => api.locations.listLocations(), [api]);
  const stock = useAsync(() => api.stock.listStock({ locationId }), [api, locationId]);

  const rows = useMemo(() => {
    const term = search.trim().toLowerCase();
    return (stock.data ?? []).filter(
      (b) => !term || b.articleName.toLowerCase().includes(term) || (b.articleBrand ?? '').toLowerCase().includes(term),
    );
  }, [stock.data, search]);

  const change = async (batch: StockBatchDto, delta: 1 | -1) => {
    const bookingRequest = {
      articleId: batch.articleId,
      locationId: batch.locationId,
      quantity: 1,
      bestBefore: batch.bestBefore,
    };
    try {
      if (delta > 0) await api.stock.bookIn({ bookingRequest });
      else await api.stock.bookOut({ bookingRequest });
      stock.reload();
    } catch (e) {
      await fail(e);
    }
  };

  return (
    <>
      <h1 className="title">Bestand</h1>
      <div className="columns">
        <div className="column is-5">
          <LocationSelect
            locations={locations.data ?? []}
            value={locationId}
            onChange={setLocationId}
            emptyLabel="Alle Lagerorte"
          />
        </div>
        <div className="column">
          <input className="input" type="search" placeholder="Artikel suchen…" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
      </div>

      {stock.error && <ErrorBox message={stock.error} onRetry={stock.reload} />}
      {stock.loading && !stock.data ? (
        <Loading />
      ) : rows.length === 0 ? (
        <Empty text="Kein Bestand gefunden." />
      ) : (
        <div className="table-container box p-0">
          <table className="table is-fullwidth is-hoverable is-narrow-mobile">
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
              {rows.map((b) => {
                const days = b.bestBefore ? daysUntil(b.bestBefore) : undefined;
                return (
                  <tr key={b.id}>
                    <td style={{ width: 56 }}>
                      <ArticleImage url={b.articleImageUrl} size={40} />
                    </td>
                    <td>
                      <strong>{b.articleName}</strong>
                      {b.articleBrand && <small className="has-text-grey"> · {b.articleBrand}</small>}
                    </td>
                    <td>{b.locationPath}</td>
                    <td>
                      {b.bestBefore ? (
                        <span className={`tag ${days! < 0 ? 'is-danger' : days! <= 7 ? 'is-warning' : 'is-light'}`}>
                          {formatDate(b.bestBefore)}
                        </span>
                      ) : (
                        <span className="has-text-grey">–</span>
                      )}
                    </td>
                    <td className="has-text-right has-text-weight-bold">{b.quantity}</td>
                    <td className="has-text-right" style={{ whiteSpace: 'nowrap' }}>
                      <div className="buttons are-small is-right is-flex-wrap-nowrap">
                        <button className="button is-link is-light" title="Eins entnehmen" onClick={() => change(b, -1)}>
                          −
                        </button>
                        <button className="button is-success is-light" title="Eins hinzufügen" onClick={() => change(b, 1)}>
                          +
                        </button>
                        <button className="button is-light" title="MHD / Inventur" onClick={() => setEditing(b)}>
                          ✎
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

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
