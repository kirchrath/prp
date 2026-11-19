import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import { formatDate, formatDateTime } from '../api/dates';
import { LocationSelect } from '../components/LocationSelect';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

const SOURCE_LABEL: Record<string, string> = { SCANNER: 'Scanner', BUTTON: 'Knopfleiste', MOBILE: 'Kamera', UI: 'Web', SYSTEM: 'System' };
const TYPE_LABEL: Record<string, string> = { IN: 'Eingelagert', OUT: 'Ausgelagert', CORRECTION: 'Korrektur', MERGE: 'Zusammengeführt' };

export function HistoryPage() {
  const api = useApi();
  const [locationId, setLocationId] = useState<string>();
  const [page, setPage] = useState(0);
  const locations = useAsync(() => api.locations.listLocations(), [api]);
  const scanners = useAsync(() => api.scanners.listScanners(), [api]);
  const movements = useAsync(() => api.movements.listMovements({ locationId, page, size: 50 }), [api, locationId, page]);

  const scannerName = (id?: string) => {
    const s = scanners.data?.find((x) => x.id === id);
    return s ? (s.name ?? s.deviceId) : undefined;
  };

  let content;
  if (movements.error) {
    content = <ErrorBox message={movements.error} onRetry={movements.reload} />;
  } else if (!movements.data) {
    content = <Loading />;
  } else if (movements.data.items.length === 0) {
    content = <Empty text="Keine Buchungen." />;
  } else {
    const result = movements.data;
    content = (
      <>
        <div className="table-container box p-0">
          <table className="table is-fullwidth is-narrow is-hoverable">
            <thead>
              <tr>
                <th>Zeit</th>
                <th>Artikel</th>
                <th>Lagerort</th>
                <th className="has-text-right">Menge</th>
                <th>Art</th>
                <th>Quelle</th>
              </tr>
            </thead>
            <tbody>
              {result.items.map((m) => {
                const who = scannerName(m.scannerId) ?? m.username;
                return (
                  <tr key={m.id}>
                    <td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(m.createdDate)}</td>
                    <td>
                      {m.articleName}
                      {m.bestBefore && <small className="has-text-grey"> · MHD {formatDate(m.bestBefore)}</small>}
                    </td>
                    <td>{m.locationPath}</td>
                    <td className={`has-text-right has-text-weight-bold ${m.delta > 0 ? 'has-text-success' : 'has-text-link'}`}>
                      {m.delta > 0 ? `+${m.delta}` : m.delta}
                    </td>
                    <td>{TYPE_LABEL[m.type]}</td>
                    <td>
                      {SOURCE_LABEL[m.source]}
                      {who && <small className="has-text-grey"> · {who}</small>}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <nav className="pagination is-centered" aria-label="Seiten">
          <button className="pagination-previous" disabled={page === 0} onClick={() => setPage(page - 1)}>
            Neuere
          </button>
          <button className="pagination-next" disabled={page + 1 >= result.totalPages} onClick={() => setPage(page + 1)}>
            Ältere
          </button>
          <span className="pagination-list">
            Seite {page + 1} / {result.totalPages}
          </span>
        </nav>
      </>
    );
  }

  return (
    <>
      <h1 className="title">Historie</h1>
      <div className="columns">
        <div className="column is-5">
          <LocationSelect
            locations={locations.data ?? []}
            value={locationId}
            onChange={(id) => {
              setLocationId(id);
              setPage(0);
            }}
            emptyLabel="Alle Lagerorte"
          />
        </div>
      </div>
      {content}
    </>
  );
}
