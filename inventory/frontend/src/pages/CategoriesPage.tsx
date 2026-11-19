import { useState } from 'react';
import { useApi, type Api } from '../api/ApiContext';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

interface Category {
  id: string;
  name: string;
}

/** Access to the categories of one service. */
export interface CategoryStore {
  list(): Promise<Category[]>;
  version(id: string): Promise<{ audit: { version: number } }>;
  create(name: string): Promise<unknown>;
  rename(id: string, name: string): Promise<unknown>;
  remove(id: string, version: number): Promise<unknown>;
}

/** Categories of articles (catalog service). */
export const articleCategories = (api: Api): CategoryStore => ({
  list: () => api.categories.listCategories(),
  version: (id) => api.categories.getCategory({ id }),
  create: (name) => api.categories.createCategory({ categorySaveDto: { data: { name } } }),
  rename: async (id, name) => {
    const { audit } = await api.categories.getCategory({ id });
    return api.categories.updateCategory({ id, categorySaveDto: { audit, data: { name } } });
  },
  remove: (id, version) => api.categories.deleteCategory({ id, version }),
});

/** Categories of recipes (recipe service). */
export const recipeCategories = (api: Api): CategoryStore => ({
  list: () => api.recipeCategories.listRecipeCategories(),
  version: (id) => api.recipeCategories.getRecipeCategory({ id }),
  create: (name) => api.recipeCategories.createRecipeCategory({ categorySaveDto: { data: { name } } }),
  rename: async (id, name) => {
    const { audit } = await api.recipeCategories.getRecipeCategory({ id });
    return api.recipeCategories.updateRecipeCategory({ id, categorySaveDto: { audit, data: { name } } });
  },
  remove: (id, version) => api.recipeCategories.deleteRecipeCategory({ id, version }),
});

export function CategoriesPage({ title, store: makeStore }: { title: string; store: (api: Api) => CategoryStore }) {
  const api = useApi();
  const store = makeStore(api);
  const { fail } = useNotify();
  const categories = useAsync(() => store.list(), [api]);
  const [name, setName] = useState('');
  const [editing, setEditing] = useState<{ id: string; name: string }>();

  const run = async (action: () => Promise<unknown>, after: () => void) => {
    try {
      await action();
      after();
      categories.reload();
    } catch (e) {
      await fail(e);
    }
  };

  const rename = (id: string, newName: string) => run(() => store.rename(id, newName), () => setEditing(undefined));

  const remove = (c: Category) => {
    if (!window.confirm(`Kategorie "${c.name}" löschen? Zugeordnete Einträge bleiben erhalten.`)) return;
    void run(async () => {
      const { audit } = await store.version(c.id);
      await store.remove(c.id, audit.version);
    }, () => undefined);
  };

  let content;
  if (categories.error) {
    content = <ErrorBox message={categories.error} onRetry={categories.reload} />;
  } else if (!categories.data) {
    content = <Loading />;
  } else if (categories.data.length === 0) {
    content = <Empty text="Noch keine Kategorien." />;
  } else {
    content = (
      <div className="box p-0">
        <table className="table is-fullwidth">
          <tbody>
            {categories.data.map((c) => (
              <tr key={c.id}>
                <td>
                  {editing?.id === c.id ? (
                    <form
                      className="field has-addons mb-0"
                      onSubmit={(e) => {
                        e.preventDefault();
                        void rename(c.id, editing.name);
                      }}
                    >
                      <div className="control is-expanded">
                        <input className="input is-small" aria-label="Neuer Name" value={editing.name}
                          onChange={(e) => setEditing({ id: c.id, name: e.target.value })} />
                      </div>
                      <div className="control">
                        <button className="button is-small is-primary" type="submit">
                          OK
                        </button>
                      </div>
                    </form>
                  ) : (
                    c.name
                  )}
                </td>
                <td className="has-text-right">
                  <div className="buttons are-small is-right">
                    <button className="button is-light" aria-label={`${c.name} umbenennen`} onClick={() => setEditing({ id: c.id, name: c.name })}>
                      ✎
                    </button>
                    <button className="button is-danger is-light" aria-label={`${c.name} löschen`} onClick={() => remove(c)}>
                      🗑
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
      <h1 className="title">{title}</h1>
      <form
        className="field has-addons"
        onSubmit={(e) => {
          e.preventDefault();
          void run(() => store.create(name), () => setName(''));
        }}
      >
        <div className="control is-expanded">
          <input className="input" placeholder="Neue Kategorie, z. B. Putzmittel" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="control">
          <button className="button is-primary" type="submit" disabled={!name.trim()}>
            Anlegen
          </button>
        </div>
      </form>
      {content}
    </>
  );
}
