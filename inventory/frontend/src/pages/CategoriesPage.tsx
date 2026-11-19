import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import type { CategoryDto } from '../api/generated';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

export function CategoriesPage() {
  const api = useApi();
  const { fail } = useNotify();
  const categories = useAsync(() => api.categories.listCategories(), [api]);
  const [name, setName] = useState('');
  const [editing, setEditing] = useState<{ id: number; name: string }>();

  const run = async (action: () => Promise<unknown>) => {
    try {
      await action();
      categories.reload();
      return true;
    } catch (e) {
      await fail(e);
      return false;
    }
  };

  const remove = (c: CategoryDto) => {
    if (window.confirm(`Kategorie "${c.name}" löschen? Artikel bleiben erhalten.`)) {
      void run(() => api.categories.deleteCategory({ id: c.id }));
    }
  };

  return (
    <>
      <h1 className="title">Kategorien</h1>
      <form
        className="field has-addons"
        onSubmit={async (e) => {
          e.preventDefault();
          if (await run(() => api.categories.createCategory({ categoryRequest: { name } }))) setName('');
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

      {categories.error && <ErrorBox message={categories.error} onRetry={categories.reload} />}
      {categories.loading && !categories.data ? (
        <Loading />
      ) : (categories.data ?? []).length === 0 ? (
        <Empty text="Noch keine Kategorien." />
      ) : (
        <div className="box p-0">
          <table className="table is-fullwidth">
            <tbody>
              {categories.data!.map((c) => (
                <tr key={c.id}>
                  <td>
                    {editing?.id === c.id ? (
                      <form
                        className="field has-addons mb-0"
                        onSubmit={async (e) => {
                          e.preventDefault();
                          if (await run(() => api.categories.updateCategory({ id: c.id, categoryRequest: { name: editing.name } }))) {
                            setEditing(undefined);
                          }
                        }}
                      >
                        <div className="control is-expanded">
                          <input
                            className="input is-small"
                            value={editing.name}
                            onChange={(e) => setEditing({ id: c.id, name: e.target.value })}
                            autoFocus
                          />
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
                      <button className="button is-light" onClick={() => setEditing({ id: c.id, name: c.name })}>
                        ✎
                      </button>
                      <button className="button is-danger is-light" onClick={() => remove(c)}>
                        🗑
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
