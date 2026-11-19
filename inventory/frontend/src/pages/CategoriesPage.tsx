import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import type { CategoryListDto } from '../api/catalog';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

export function CategoriesPage() {
  const api = useApi();
  const { fail } = useNotify();
  const categories = useAsync(() => api.categories.listCategories(), [api]);
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

  const rename = (id: string, newName: string) =>
    run(async () => {
      const { audit } = await api.categories.getCategory({ id });
      await api.categories.updateCategory({ id, categorySaveDto: { audit, data: { name: newName } } });
    }, () => setEditing(undefined));

  const remove = (c: CategoryListDto) => {
    if (!window.confirm(`Kategorie "${c.name}" löschen? Artikel bleiben erhalten.`)) return;
    void run(async () => {
      const { audit } = await api.categories.getCategory({ id: c.id });
      await api.categories.deleteCategory({ id: c.id, version: audit.version });
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
      <h1 className="title">Kategorien</h1>
      <form
        className="field has-addons"
        onSubmit={(e) => {
          e.preventDefault();
          void run(() => api.categories.createCategory({ categorySaveDto: { data: { name } } }), () => setName(''));
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
