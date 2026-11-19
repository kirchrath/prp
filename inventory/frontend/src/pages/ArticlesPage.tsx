import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { useApi } from '../api/ApiContext';
import { ArticleDtoStatusEnum, ListArticlesStatusEnum, type ArticleDto } from '../api/generated';
import { ArticleImage } from '../components/ArticleImage';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';
import { ArticleModal } from './ArticleModal';
import { BookingModal } from './BookingModal';

export function ArticlesPage() {
  const api = useApi();
  const { notify, fail } = useNotify();
  const [params, setParams] = useSearchParams();
  const inboxOnly = params.get('status') === 'UNKNOWN';
  const [search, setSearch] = useState('');
  const [categoryId, setCategoryId] = useState<number>();
  const [editing, setEditing] = useState<ArticleDto | 'new'>();
  const [booking, setBooking] = useState<{ article: ArticleDto; direction: 'in' | 'out' }>();

  const categories = useAsync(() => api.categories.listCategories(), [api]);
  const locations = useAsync(() => api.locations.listLocations(), [api]);
  const allArticles = useAsync(() => api.articles.listArticles({}), [api]);
  const articles = useAsync(
    () =>
      api.articles.listArticles({
        search: search || undefined,
        categoryId,
        status: inboxOnly ? ListArticlesStatusEnum.Unknown : undefined,
      }),
    [api, search, categoryId, inboxOnly],
  );

  const reloadAll = () => {
    articles.reload();
    allArticles.reload();
  };

  const remove = async (a: ArticleDto) => {
    if (!window.confirm(`Artikel "${a.name}" löschen? Die Historie wird ebenfalls gelöscht.`)) return;
    try {
      await api.articles.deleteArticle({ id: a.id });
      notify('success', 'Artikel gelöscht');
      reloadAll();
    } catch (e) {
      await fail(e);
    }
  };

  return (
    <>
      <div className="level is-mobile">
        <div className="level-left">
          <h1 className="title">Artikel</h1>
        </div>
        <div className="level-right">
          <button className="button is-primary" onClick={() => setEditing('new')}>
            + Neuer Artikel
          </button>
        </div>
      </div>

      <div className="tabs is-boxed">
        <ul>
          <li className={inboxOnly ? '' : 'is-active'}>
            <a onClick={() => setParams({})}>Alle</a>
          </li>
          <li className={inboxOnly ? 'is-active' : ''}>
            <a onClick={() => setParams({ status: 'UNKNOWN' })}>Inbox (unbekannt)</a>
          </li>
        </ul>
      </div>

      <div className="columns">
        <div className="column">
          <input
            className="input"
            type="search"
            placeholder="Name, Marke oder EAN…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="column is-4">
          <div className="select is-fullwidth">
            <select value={categoryId ?? ''} onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : undefined)}>
              <option value="">Alle Kategorien</option>
              {(categories.data ?? []).map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {articles.error && <ErrorBox message={articles.error} onRetry={articles.reload} />}
      {articles.loading && !articles.data ? (
        <Loading />
      ) : (articles.data ?? []).length === 0 ? (
        <Empty text={inboxOnly ? 'Die Inbox ist leer. 🎉' : 'Keine Artikel gefunden.'} />
      ) : (
        <div className="table-container box p-0">
          <table className="table is-fullwidth is-hoverable">
            <thead>
              <tr>
                <th />
                <th>Artikel</th>
                <th>Kategorie</th>
                <th className="has-text-right">Bestand</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {articles.data!.map((a) => {
                const low = a.minStock > 0 && a.totalQuantity < a.minStock;
                return (
                  <tr key={a.id}>
                    <td style={{ width: 56 }}>
                      <ArticleImage url={a.imageUrl} size={40} />
                    </td>
                    <td className="is-clickable-row" onClick={() => setEditing(a)}>
                      <strong>{a.name}</strong>
                      {a.status === ArticleDtoStatusEnum.Unknown && <span className="tag is-warning ml-2">Inbox</span>}
                      <br />
                      <small className="has-text-grey">
                        {[a.brand, a.packageSize].filter(Boolean).join(' · ')}
                        {a.eans.length > 0 && ` · ${a.eans.join(', ')}`}
                      </small>
                    </td>
                    <td>{a.categoryName ?? <span className="has-text-grey">–</span>}</td>
                    <td className="has-text-right">
                      <span className={`tag is-medium ${low ? 'is-danger is-light' : 'is-light'}`}>
                        {a.totalQuantity}
                        {a.minStock > 0 && <small className="has-text-grey">&nbsp;/ min {a.minStock}</small>}
                      </span>
                    </td>
                    <td className="has-text-right">
                      <div className="buttons are-small is-right is-flex-wrap-nowrap">
                        <button className="button is-success is-light" onClick={() => setBooking({ article: a, direction: 'in' })}>
                          Einlagern
                        </button>
                        <button
                          className="button is-link is-light"
                          disabled={a.totalQuantity === 0}
                          onClick={() => setBooking({ article: a, direction: 'out' })}
                        >
                          Auslagern
                        </button>
                        <button className="button is-danger is-light" title="Löschen" onClick={() => remove(a)}>
                          🗑
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
        <ArticleModal
          article={editing === 'new' ? undefined : editing}
          categories={categories.data ?? []}
          articles={allArticles.data ?? []}
          onClose={() => setEditing(undefined)}
          onSaved={() => {
            setEditing(undefined);
            reloadAll();
          }}
        />
      )}
      {booking && (
        <BookingModal
          article={booking.article}
          direction={booking.direction}
          locations={locations.data ?? []}
          onClose={() => setBooking(undefined)}
          onDone={() => {
            setBooking(undefined);
            reloadAll();
          }}
        />
      )}
    </>
  );
}
