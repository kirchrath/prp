import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { useApi } from '../api/ApiContext';
import { ArticleListDtoStatusEnum, ListArticlesStatusEnum, type ArticleDto, type ArticleListDto } from '../api/catalog';
import { ArticleImage } from '../components/ArticleImage';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';
import { ArticleModal } from './ArticleModal';
import { BookingModal } from './BookingModal';

type Editing = { id: string; article: ArticleDto } | 'new';

export function ArticlesPage() {
  const api = useApi();
  const { notify, fail } = useNotify();
  const [params, setParams] = useSearchParams();
  const inboxOnly = params.get('status') === 'UNKNOWN';
  const [search, setSearch] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [editing, setEditing] = useState<Editing>();
  const [booking, setBooking] = useState<{ article: ArticleListDto; direction: 'in' | 'out' }>();

  const categories = useAsync(() => api.categories.listCategories(), [api]);
  const locations = useAsync(() => api.locations.listLocations(), [api]);
  const levels = useAsync(() => api.stock.listLevels(), [api]);
  const allArticles = useAsync(() => api.articles.listArticles({}), [api]);
  const articles = useAsync(
    () =>
      api.articles.listArticles({
        search: search || undefined,
        categoryId: categoryId || undefined,
        status: inboxOnly ? ListArticlesStatusEnum.Unknown : undefined,
      }),
    [api, search, categoryId, inboxOnly],
  );

  const quantity = (id: string) => levels.data?.find((l) => l.articleId === id)?.quantity ?? 0;

  const reloadAll = () => {
    articles.reload();
    allArticles.reload();
    levels.reload();
  };

  const edit = async (a: ArticleListDto) => {
    try {
      setEditing({ id: a.id, article: await api.articles.getArticle({ id: a.id }) });
    } catch (e) {
      await fail(e);
    }
  };

  const remove = async (a: ArticleListDto) => {
    if (!window.confirm(`Artikel "${a.name}" löschen?`)) return;
    try {
      const { audit } = await api.articles.getArticle({ id: a.id });
      await api.articles.deleteArticle({ id: a.id, version: audit.version });
      notify('success', 'Artikel gelöscht');
      reloadAll();
    } catch (e) {
      await fail(e);
    }
  };

  let content;
  if (articles.error) {
    content = <ErrorBox message={articles.error} onRetry={articles.reload} />;
  } else if (!articles.data) {
    content = <Loading />;
  } else if (articles.data.length === 0) {
    content = <Empty text={inboxOnly ? 'Die Inbox ist leer. 🎉' : 'Keine Artikel gefunden.'} />;
  } else {
    content = (
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
            {articles.data.map((a) => {
              const total = quantity(a.id);
              const low = a.minStock > 0 && total < a.minStock;
              return (
                <tr key={a.id}>
                  <td style={{ width: 56 }}>
                    <ArticleImage url={a.imageUrl} size={40} />
                  </td>
                  <td>
                    <button className="button is-ghost p-0 has-text-weight-bold" onClick={() => edit(a)}>
                      {a.name}
                    </button>
                    {a.status === ArticleListDtoStatusEnum.Unknown && <span className="tag is-warning ml-2">Inbox</span>}
                    <br />
                    <small className="has-text-grey">{[a.brand, a.packageSize, ...a.eans].filter(Boolean).join(' · ')}</small>
                  </td>
                  <td>{a.categoryName ?? '–'}</td>
                  <td className="has-text-right">
                    <span className={`tag is-medium ${low ? 'is-danger is-light' : 'is-light'}`}>
                      {total}
                      {a.minStock > 0 && <small className="has-text-grey">&nbsp;/ min {a.minStock}</small>}
                    </span>
                  </td>
                  <td className="has-text-right">
                    <div className="buttons are-small is-right is-flex-wrap-nowrap">
                      <button className="button is-success is-light" onClick={() => setBooking({ article: a, direction: 'in' })}>
                        Einlagern
                      </button>
                      <button className="button is-link is-light" disabled={total === 0} onClick={() => setBooking({ article: a, direction: 'out' })}>
                        Auslagern
                      </button>
                      <button className="button is-danger is-light" aria-label={`${a.name} löschen`} onClick={() => remove(a)}>
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
    );
  }

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
          <input className="input" type="search" placeholder="Name, Marke oder EAN…" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <div className="column is-4">
          <div className="select is-fullwidth">
            <select aria-label="Kategorie" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
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

      {content}

      {editing && (
        <ArticleModal
          existing={editing === 'new' ? undefined : editing}
          categories={categories.data ?? []}
          articles={(allArticles.data ?? []).filter((a) => editing === 'new' || a.id !== editing.id)}
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
