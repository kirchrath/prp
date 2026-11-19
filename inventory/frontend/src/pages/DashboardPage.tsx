import { Link } from 'react-router';
import { useApi } from '../api/ApiContext';
import { daysUntil, formatDate } from '../api/dates';
import { ArticleImage } from '../components/ArticleImage';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

function Tile({ label, value, to, color }: { label: string; value: number | string; to?: string; color?: string }) {
  const content = (
    <div className={`box stat has-text-centered ${color ?? ''}`}>
      <p className="title is-3">{value}</p>
      <p className="heading">{label}</p>
    </div>
  );
  return <div className="column is-6-mobile is-4-tablet is-2-desktop">{to ? <Link to={to}>{content}</Link> : content}</div>;
}

export function DashboardPage() {
  const api = useApi();
  const { data, error, loading, reload } = useAsync(() => api.dashboard.getDashboard({ days: 14 }), [api]);

  if (loading && !data) return <Loading />;
  if (error) return <ErrorBox message={error} onRetry={reload} />;
  if (!data) return null;

  return (
    <>
      <h1 className="title">Übersicht</h1>
      <div className="columns is-multiline is-mobile">
        <Tile label="Artikel" value={data.articleCount} to="/artikel" />
        <Tile label="Einheiten" value={data.totalUnits} to="/bestand" />
        <Tile
          label="Inbox (unbekannt)"
          value={data.inboxCount}
          to="/artikel?status=UNKNOWN"
          color={data.inboxCount > 0 ? 'has-background-warning-light' : ''}
        />
        <Tile
          label="Unter Mindestbestand"
          value={data.belowMinStockCount}
          to="/einkaufsliste"
          color={data.belowMinStockCount > 0 ? 'has-background-danger-light' : ''}
        />
        <Tile label="Einkaufsliste offen" value={data.openShoppingItems} to="/einkaufsliste" />
        <Tile label="Scanner online" value={`${data.scannersOnline}/${data.scannersTotal}`} to="/scanner" />
      </div>

      <h2 className="title is-5">Mindesthaltbarkeit in den nächsten 14 Tagen</h2>
      {data.expiringSoon.length === 0 ? (
        <Empty text="Nichts läuft in den nächsten 14 Tagen ab." />
      ) : (
        <div className="box p-0">
          <table className="table is-fullwidth is-hoverable">
            <tbody>
              {data.expiringSoon.map((b) => {
                const days = daysUntil(b.bestBefore!);
                return (
                  <tr key={b.id}>
                    <td style={{ width: 56 }}>
                      <ArticleImage url={b.articleImageUrl} size={40} />
                    </td>
                    <td>
                      <strong>{b.articleName}</strong>
                      <br />
                      <small className="has-text-grey">{b.locationPath}</small>
                    </td>
                    <td className="has-text-right">{b.quantity}×</td>
                    <td className="has-text-right">
                      <span className={`tag ${days < 0 ? 'is-danger' : days <= 3 ? 'is-warning' : 'is-info is-light'}`}>
                        {formatDate(b.bestBefore)}
                        {days < 0 ? ' (abgelaufen)' : days === 0 ? ' (heute)' : ` (${days} T)`}
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
