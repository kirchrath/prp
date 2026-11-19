import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import { formatDate, formatDateTime } from '../api/dates';
import { MovementDtoSourceEnum, MovementDtoTypeEnum } from '../api/generated';
import { LocationSelect } from '../components/LocationSelect';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

const SOURCE_LABEL: Record<MovementDtoSourceEnum, string> = {
  [MovementDtoSourceEnum.Scanner]: 'Scanner',
  [MovementDtoSourceEnum.Mobile]: 'Kamera',
  [MovementDtoSourceEnum.Ui]: 'Web',
};

const TYPE_LABEL: Record<MovementDtoTypeEnum, string> = {
  [MovementDtoTypeEnum.In]: 'Eingelagert',
  [MovementDtoTypeEnum.Out]: 'Ausgelagert',
  [MovementDtoTypeEnum.Correction]: 'Korrektur',
};

export function HistoryPage() {
  const api = useApi();
  const [locationId, setLocationId] = useState<number>();
  const [page, setPage] = useState(0);
  const locations = useAsync(() => api.locations.listLocations(), [api]);
  const movements = useAsync(() => api.movements.listMovements({ locationId, page, size: 50 }), [api, locationId, page]);

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

      {movements.error && <ErrorBox message={movements.error} onRetry={movements.reload} />}
      {movements.loading && !movements.data ? (
        <Loading />
      ) : (movements.data?.items ?? []).length === 0 ? (
        <Empty text="Keine Buchungen." />
      ) : (
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
                {movements.data!.items.map((m) => (
                  <tr key={m.id}>
                    <td style={{ whiteSpace: 'nowrap' }}>{formatDateTime(m.createdAt)}</td>
                    <td>
                      {m.articleName}
                      {m.bestBefore && <small className="has-text-grey"> · MHD {formatDate(m.bestBefore)}</small>}
                    </td>
                    <td>{m.locationPath ?? '–'}</td>
                    <td className={`has-text-right has-text-weight-bold ${m.delta > 0 ? 'has-text-success' : 'has-text-link'}`}>
                      {m.delta > 0 ? `+${m.delta}` : m.delta}
                    </td>
                    <td>{TYPE_LABEL[m.type]}</td>
                    <td>
                      {SOURCE_LABEL[m.source]}
                      {(m.scannerName || m.username) && (
                        <small className="has-text-grey"> · {m.scannerName ?? m.username}</small>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav className="pagination is-centered" aria-label="Seiten">
            <button className="pagination-previous" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Neuere
            </button>
            <button
              className="pagination-next"
              disabled={page + 1 >= movements.data!.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Ältere
            </button>
            <ul className="pagination-list">
              <li>
                <span className="pagination-ellipsis">
                  Seite {page + 1} / {Math.max(movements.data!.totalPages, 1)}
                </span>
              </li>
            </ul>
          </nav>
        </>
      )}
    </>
  );
}
