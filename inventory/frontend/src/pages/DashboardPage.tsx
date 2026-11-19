import { Link } from 'react-router';
import { useApi } from '../api/ApiContext';
import { ArticleListDtoStatusEnum } from '../api/catalog';
import { daysUntil, formatDate } from '../api/dates';
import { ArticleImage } from '../components/ArticleImage';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

function Tile({ label, value, to, highlight }: { label: string; value: number | string; to: string; highlight?: string }) {
  return (
    <div className="column is-6-mobile is-4-tablet is-2-desktop">
      <Link to={to}>
        <div className={`box stat has-text-centered ${highlight ?? ''}`}>
          <p className="title is-3">{value}</p>
          <p className="heading">{label}</p>
        </div>
      </Link>
    </div>
  );
}

function expiryTag(days: number): string {
  if (days < 0) return 'is-danger';
  return days <= 3 ? 'is-warning' : 'is-info is-light';
}

function expiryText(days: number): string {
  if (days < 0) return 'abgelaufen';
  return days === 0 ? 'heute' : `${days} T`;
}

/** Key figures composed from all services. */
export function DashboardPage() {
  const api = useApi();
  const { data, error, loading, reload } = useAsync(
    () =>
      Promise.all([
        api.articles.listArticles({}),
        api.stock.getSummary({ days: 14 }),
        api.shopping.listShoppingItems(),
        api.scanners.listScanners(),
      ]),
    [api],
  );

  if (error) return <ErrorBox message={error} onRetry={reload} />;
  if (loading || !data) return <Loading />;
  const [articles, summary, items, scanners] = data;
  const inbox = articles.filter((a) => a.status === ArticleListDtoStatusEnum.Unknown).length;
  const belowMin = items.filter((i) => i.automatic && !i.done).length;
  const open = items.filter((i) => !i.done).length;
  const online = scanners.filter((s) => s.online).length;

  return (
    <>
      <h1 className="title">Übersicht</h1>
      <div className="columns is-multiline is-mobile">
        <Tile label="Artikel" value={articles.length} to="/artikel" />
        <Tile label="Einheiten" value={summary.totalUnits} to="/bestand" />
        <Tile label="Inbox (unbekannt)" value={inbox} to="/artikel?status=UNKNOWN" highlight={inbox > 0 ? 'has-background-warning-light' : ''} />
        <Tile label="Unter Mindestbestand" value={belowMin} to="/einkaufsliste" highlight={belowMin > 0 ? 'has-background-danger-light' : ''} />
        <Tile label="Einkaufsliste offen" value={open} to="/einkaufsliste" />
        <Tile label="Scanner online" value={`${online}/${scanners.length}`} to="/scanner" />
      </div>

      <h2 className="title is-5">Mindesthaltbarkeit in den nächsten 14 Tagen</h2>
      {summary.expiringSoon.length === 0 ? (
        <Empty text="Nichts läuft in den nächsten 14 Tagen ab." />
      ) : (
        <div className="box p-0">
          <table className="table is-fullwidth is-hoverable">
            <tbody>
              {summary.expiringSoon.map((b) => {
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
                      <span className={`tag ${expiryTag(days)}`}>
                        {formatDate(b.bestBefore)} ({expiryText(days)})
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
