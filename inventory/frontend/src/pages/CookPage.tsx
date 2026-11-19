import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import type { RecipeSuggestionDto } from '../api/recipe';
import { formatAmount } from '../api/units';
import { ArticleImage } from '../components/ArticleImage';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

/** What can I cook for x persons? */
export function CookPage() {
  const api = useApi();
  const { notify, fail } = useNotify();
  const [servings, setServings] = useState(2);
  const [categoryId, setCategoryId] = useState('');
  const categories = useAsync(() => api.recipeCategories.listRecipeCategories(), [api]);
  const suggestions = useAsync(
    () => api.cooking.listSuggestions({ servings: Math.max(servings, 1), categoryId: categoryId || undefined }),
    [api, servings, categoryId],
  );

  const shop = async (s: RecipeSuggestionDto) => {
    try {
      await api.cooking.createShoppingRequest({ id: s.recipeId, servings });
      notify('success', `Fehlendes für ${s.title} steht auf der Einkaufsliste`);
    } catch (e) {
      await fail(e);
    }
  };

  let content;
  if (suggestions.error) {
    content = <ErrorBox message={suggestions.error} onRetry={suggestions.reload} />;
  } else if (!suggestions.data) {
    content = <Loading />;
  } else if (suggestions.data.length === 0) {
    content = <Empty text="Noch keine Rezepte vorhanden." />;
  } else {
    content = suggestions.data.map((s) => (
      <div key={s.recipeId} className="box">
        <article className="media">
          <div className="media-left">
            <ArticleImage url={s.imageUrl} size={64} />
          </div>
          <div className="media-content">
            <p>
              <strong>{s.title}</strong>{' '}
              {s.cookable ? (
                <span className="tag is-success">kochbar</span>
              ) : (
                <span className="tag is-warning">{s.missing.length} fehlt</span>
              )}
              <br />
              <small className="has-text-grey">{[s.categoryName, s.totalMinutes && `${s.totalMinutes} min`].filter(Boolean).join(' · ')}</small>
            </p>
            {!s.cookable && (
              <>
                <ul className="mt-2">
                  {s.missing.map((m, i) => (
                    <li key={i}>
                      {m.name} {formatAmount(m.amount, m.unit)}
                      {m.articleName && (
                        <small className="has-text-grey">
                          {' '}
                          · {m.packages}× {m.articleName}
                        </small>
                      )}
                    </li>
                  ))}
                </ul>
                <button className="button is-small is-link is-light mt-2" onClick={() => shop(s)}>
                  Fehlendes auf die Einkaufsliste
                </button>
              </>
            )}
          </div>
        </article>
      </div>
    ));
  }

  return (
    <>
      <h1 className="title">Was kann ich kochen?</h1>
      <div className="columns is-mobile">
        <div className="column is-narrow">
          <div className="field has-addons">
            <div className="control">
              <input className="input" type="number" min={1} max={100} aria-label="Personen" style={{ width: '5rem' }}
                value={servings} onChange={(e) => setServings(Number(e.target.value))} />
            </div>
            <div className="control">
              <span className="button is-static">Personen</span>
            </div>
          </div>
        </div>
        <div className="column">
          <div className="select is-fullwidth">
            <select aria-label="Rezeptkategorie" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
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
    </>
  );
}
