import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import type { ShoppingItemListDto } from '../api/shopping';
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

  const toggle = (item: ShoppingListItem) =>
    run(async () => {
      const { audit, data } = await api.shopping.getShoppingItem({ id: item.id });
      await api.shopping.updateShoppingItem({ id: item.id, shoppingItemSaveDto: { audit, data: { ...data, done: !data.done } } });
    });

  const remove = (item: ShoppingListItem) =>
    run(async () => {
      const { audit } = await api.shopping.getShoppingItem({ id: item.id });
      await api.shopping.deleteShoppingItem({ id: item.id, version: audit.version });
    });

  let content;
  if (items.error) {
    content = <ErrorBox message={items.error} onRetry={items.reload} />;
  } else if (!items.data) {
    content = <Loading />;
  } else if (items.data.length === 0) {
    content = <Empty text="Die Einkaufsliste ist leer." />;
  } else {
    content = (
      <div className="box p-0">
        {items.data.map((item) => (
          <div key={item.id} className="is-flex is-align-items-center px-4 py-3" style={{ borderBottom: '1px solid var(--bulma-border-weak)' }}>
            <label className="checkbox is-flex-grow-1">
              <input type="checkbox" className="mr-3" checked={item.done} onChange={() => toggle(item)} />
              <span className={item.done ? 'has-text-grey' : ''} style={{ textDecoration: item.done ? 'line-through' : undefined }}>
                <strong>{item.quantity}×</strong> {item.label}
                {item.brand && <small className="has-text-grey"> · {item.brand}</small>}
                {item.note && <small className="has-text-grey"> · {item.note}</small>}
              </span>
            </label>
            {item.automatic && <span className="tag is-info is-light mr-2" title="Automatisch wegen Mindestbestand">auto</span>}
            <button className="delete" aria-label={`${item.label} entfernen`} onClick={() => remove(item)} />
          </div>
        ))}
      </div>
    );
  }

  return (
    <>
      <div className="level is-mobile">
        <div className="level-left">
          <h1 className="title">Einkaufsliste</h1>
        </div>
        <div className="level-right">
          <button className="button is-light" disabled={!items.data?.some((i) => i.done)}
            onClick={() => run(() => api.shopping.deleteDoneShoppingItems())}>
            Erledigte entfernen
          </button>
        </div>
      </div>

      <form
        className="field has-addons"
        onSubmit={(e) => {
          e.preventDefault();
          void run(() =>
            api.shopping.createShoppingItem({
              shoppingItemSaveDto: { data: { text: text.trim(), quantity, automatic: false, done: false } },
            }),
          );
          setText('');
          setQuantity(1);
        }}
      >
        <div className="control" style={{ width: '5rem' }}>
          <input className="input" type="number" min={1} aria-label="Menge" value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} />
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

      {content}
      <p className="help mt-3">
        Einträge mit „auto“ entstehen, sobald ein Artikel unter seinen Mindestbestand fällt, und verschwinden wieder, wenn er
        aufgefüllt ist.
      </p>
    </>
  );
}

type ShoppingListItem = ShoppingItemListDto;
