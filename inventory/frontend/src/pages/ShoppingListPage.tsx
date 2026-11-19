import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import type { ShoppingListItemDto } from '../api/generated';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

export function ShoppingListPage() {
  const api = useApi();
  const { fail } = useNotify();
  const items = useAsync(() => api.shopping.listShoppingItems(), [api]);
  const [text, setText] = useState('');
  const [quantity, setQuantity] = useState(1);

  const run = async (action: () => Promise<unknown>) => {
    try {
      await action();
      items.reload();
    } catch (e) {
      await fail(e);
    }
  };

  const toggle = (item: ShoppingListItemDto) =>
    run(() => api.shopping.updateShoppingItem({ id: item.id, shoppingListItemUpdate: { done: !item.done } }));

  const anyDone = (items.data ?? []).some((i) => i.done);

  return (
    <>
      <div className="level is-mobile">
        <div className="level-left">
          <h1 className="title">Einkaufsliste</h1>
        </div>
        <div className="level-right">
          <button
            className="button is-light"
            disabled={!anyDone}
            onClick={() => run(() => api.shopping.clearDoneShoppingItems())}
          >
            Erledigte entfernen
          </button>
        </div>
      </div>

      <form
        className="field has-addons"
        onSubmit={(e) => {
          e.preventDefault();
          if (!text.trim()) return;
          void run(() =>
            api.shopping.addShoppingItem({ shoppingListItemRequest: { text: text.trim(), quantity } }),
          ).then(() => {
            setText('');
            setQuantity(1);
          });
        }}
      >
        <div className="control" style={{ width: '5rem' }}>
          <input className="input" type="number" min={1} value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} />
        </div>
        <div className="control is-expanded">
          <input className="input" placeholder="Eintrag hinzufügen…" value={text} onChange={(e) => setText(e.target.value)} />
        </div>
        <div className="control">
          <button className="button is-primary" type="submit" disabled={!text.trim()}>
            +
          </button>
        </div>
      </form>

      {items.error && <ErrorBox message={items.error} onRetry={items.reload} />}
      {items.loading && !items.data ? (
        <Loading />
      ) : (items.data ?? []).length === 0 ? (
        <Empty text="Die Einkaufsliste ist leer." />
      ) : (
        <div className="box p-0">
          {items.data!.map((item) => (
            <div key={item.id} className="is-flex is-align-items-center px-4 py-3" style={{ borderBottom: '1px solid var(--bulma-border-weak)' }}>
              <label className="checkbox is-flex-grow-1 is-flex is-align-items-center">
                <input type="checkbox" className="mr-3" checked={item.done} onChange={() => toggle(item)} style={{ transform: 'scale(1.4)' }} />
                <span className={item.done ? 'has-text-grey' : ''} style={{ textDecoration: item.done ? 'line-through' : undefined }}>
                  <strong>{item.quantity}×</strong> {item.label}
                  {item.brand && <small className="has-text-grey"> · {item.brand}</small>}
                </span>
              </label>
              {item.automatic && (
                <span className="tag is-info is-light mr-2" title="Automatisch wegen Mindestbestand">
                  auto
                </span>
              )}
              <button className="delete" aria-label="Entfernen" onClick={() => run(() => api.shopping.deleteShoppingItem({ id: item.id }))} />
            </div>
          ))}
        </div>
      )}
      <p className="help mt-3">
        Einträge mit „auto“ entstehen, sobald ein Artikel unter seinen Mindestbestand fällt, und verschwinden wieder,
        wenn er aufgefüllt ist.
      </p>
    </>
  );
}
