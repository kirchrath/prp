import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import type { RecipeDataDto, RecipeDto } from '../api/recipe';
import { ArticleImage } from '../components/ArticleImage';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';
import { ImportModal } from './ImportModal';
import { RecipeEditor } from './RecipeEditor';

type Editing = { existing: { id: string; recipe: RecipeDto } } | { draft?: RecipeDataDto };

export function RecipesPage() {
  const api = useApi();
  const { fail } = useNotify();
  const [search, setSearch] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [tag, setTag] = useState('');
  const [editing, setEditing] = useState<Editing>();
  const [importing, setImporting] = useState(false);
  const categories = useAsync(() => api.recipeCategories.listRecipeCategories(), [api]);
  const tags = useAsync(() => api.recipes.listRecipeTags(), [api]);
  const articles = useAsync(() => api.articles.listArticles({}), [api]);
  const recipes = useAsync(
    () => api.recipes.listRecipes({ search: search || undefined, categoryId: categoryId || undefined, tag: tag || undefined }),
    [api, search, categoryId, tag],
  );

  const open = async (id: string) => {
    try {
      setEditing({ existing: { id, recipe: await api.recipes.getRecipe({ id }) } });
    } catch (e) {
      await fail(e);
    }
  };

  let content;
  if (recipes.error) {
    content = <ErrorBox message={recipes.error} onRetry={recipes.reload} />;
  } else if (!recipes.data) {
    content = <Loading />;
  } else if (recipes.data.length === 0) {
    content = <Empty text="Keine Rezepte gefunden. Lege eins an oder importiere eins per Link." />;
  } else {
    content = (
      <div className="box p-0">
        <table className="table is-fullwidth is-hoverable">
          <tbody>
            {recipes.data.map((r) => (
              <tr key={r.id}>
                <td style={{ width: 72 }}>
                  <ArticleImage url={r.imageUrl} size={56} />
                </td>
                <td>
                  <button className="button is-ghost p-0 has-text-weight-bold" onClick={() => open(r.id)}>
                    {r.title}
                  </button>
                  <br />
                  <small className="has-text-grey">
                    {[r.categoryName, `${r.servings} Pers.`, r.totalMinutes && `${r.totalMinutes} min`].filter(Boolean).join(' · ')}
                  </small>
                  <div className="tags mt-1">
                    {r.tags.map((t) => (
                      <span key={t} className="tag is-light">
                        {t}
                      </span>
                    ))}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  const done = () => {
    setEditing(undefined);
    recipes.reload();
    tags.reload();
  };

  return (
    <>
      <div className="level is-mobile">
        <div className="level-left">
          <h1 className="title">Rezepte</h1>
        </div>
        <div className="level-right buttons">
          <button className="button" onClick={() => setImporting(true)}>
            Importieren
          </button>
          <button className="button is-primary" onClick={() => setEditing({})}>
            + Neues Rezept
          </button>
        </div>
      </div>
      <div className="columns">
        <div className="column">
          <input className="input" type="search" placeholder="Rezept oder Zutat suchen…" value={search}
            onChange={(e) => setSearch(e.target.value)} />
        </div>
        <div className="column is-3">
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
        <div className="column is-3">
          <div className="select is-fullwidth">
            <select aria-label="Tag" value={tag} onChange={(e) => setTag(e.target.value)}>
              <option value="">Alle Tags</option>
              {(tags.data ?? []).map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>
      {content}
      {importing && (
        <ImportModal
          onClose={() => setImporting(false)}
          onDraft={(draft) => {
            setImporting(false);
            setEditing({ draft });
          }}
        />
      )}
      {editing && (
        <RecipeEditor
          existing={'existing' in editing ? editing.existing : undefined}
          draft={'draft' in editing ? editing.draft : undefined}
          categories={categories.data ?? []}
          articles={articles.data ?? []}
          onClose={() => setEditing(undefined)}
          onSaved={done}
        />
      )}
    </>
  );
}
