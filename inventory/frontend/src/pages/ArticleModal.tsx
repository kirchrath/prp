import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import { ArticleListDtoStatusEnum, type ArticleDto, type ArticleListDto, type CategoryListDto } from '../api/catalog';
import { ArticleImage } from '../components/ArticleImage';
import { Field } from '../components/Field';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/Notifications';

interface Props {
  /** Id and loaded state of an existing article; undefined for a new one. */
  existing?: { id: string; article: ArticleDto };
  categories: CategoryListDto[];
  /** Possible merge targets for inbox entries. */
  articles: ArticleListDto[];
  onClose: () => void;
  onSaved: () => void;
}

const UNKNOWN = 'UNKNOWN';

export function ArticleModal({ existing, categories, articles, onClose, onSaved }: Props) {
  const api = useApi();
  const { notify, fail } = useNotify();
  const data = existing?.article.data;
  const isInbox = data?.status === UNKNOWN;
  const [name, setName] = useState(isInbox ? '' : (data?.name ?? ''));
  const [brand, setBrand] = useState(data?.brand ?? '');
  const [packageSize, setPackageSize] = useState(data?.packageSize ?? '');
  const [imageUrl, setImageUrl] = useState(data?.imageUrl ?? '');
  const [categoryId, setCategoryId] = useState(data?.categoryId ?? '');
  const [minStock, setMinStock] = useState(data?.minStock ?? 0);
  const [eans, setEans] = useState<string[]>(data?.eans ?? []);
  const [newEan, setNewEan] = useState('');
  const [mergeTarget, setMergeTarget] = useState('');

  const run = async (action: () => Promise<unknown>, message: string) => {
    try {
      await action();
      notify('success', message);
      onSaved();
    } catch (e) {
      await fail(e);
    }
  };

  const lookup = async (ean: string) => {
    try {
      const result = await api.articles.lookupProduct({ ean });
      if (result.existingArticleId && result.existingArticleId !== existing?.id) {
        notify('warning', 'Diese EAN gehört bereits zu einem anderen Artikel.');
      }
      if (!result.found) {
        notify('info', 'Keine Produktdaten gefunden.');
        return;
      }
      setName(result.name ?? name);
      setBrand(result.brand ?? brand);
      setPackageSize(result.packageSize ?? packageSize);
      setImageUrl(result.imageUrl ?? imageUrl);
      notify('success', 'Produktdaten übernommen');
    } catch (e) {
      await fail(e);
    }
  };

  const addEan = () => {
    const code = newEan.trim();
    if (code && !eans.includes(code)) setEans([...eans, code]);
    setNewEan('');
  };

  const save = () => {
    const articleDataDto = {
      name,
      brand: brand || undefined,
      packageSize: packageSize || undefined,
      imageUrl: imageUrl || undefined,
      categoryId: categoryId || undefined,
      minStock,
      eans,
    };
    return run(
      () =>
        existing
          ? api.articles.updateArticle({ id: existing.id, articleSaveDto: { audit: existing.article.audit, data: articleDataDto } })
          : api.articles.createArticle({ articleSaveDto: { data: articleDataDto } }),
      'Artikel gespeichert',
    );
  };

  const merge = () =>
    run(() => api.articles.mergeArticle({ id: existing!.id, targetId: mergeTarget, version: existing!.article.audit.version }),
      'Artikel zusammengeführt');

  return (
    <Modal
      title={existing ? (isInbox ? 'Unbekannten Artikel ergänzen' : 'Artikel bearbeiten') : 'Neuer Artikel'}
      onClose={onClose}
      footer={
        <>
          <button className="button" onClick={onClose}>
            Abbrechen
          </button>
          <button className="button is-primary" disabled={!name.trim()} onClick={save}>
            Speichern
          </button>
        </>
      }
    >
      {isInbox && (
        <div className="notification is-warning is-light">
          Dieser Artikel wurde per Scan angelegt, aber in keiner Produktdatenbank gefunden. Bitte Namen ergänzen – oder ihn
          unten mit einem vorhandenen Artikel zusammenführen.
        </div>
      )}

      <div className="field">
        <p className="label">EAN / Barcodes</p>
        <div className="tags mb-2">
          {eans.map((e) => (
            <span key={e} className="tag is-medium">
              {e}
              <button className="delete is-small" aria-label={`EAN ${e} entfernen`} onClick={() => setEans(eans.filter((x) => x !== e))} />
              <button className="button is-small is-ghost px-1" aria-label={`EAN ${e} nachschlagen`} onClick={() => lookup(e)}>
                🔎
              </button>
            </span>
          ))}
        </div>
        <div className="field has-addons">
          <div className="control is-expanded">
            <input className="input" inputMode="numeric" placeholder="EAN hinzufügen" value={newEan}
              onChange={(e) => setNewEan(e.target.value)} onKeyDown={(e) => e.key === 'Enter' && addEan()} />
          </div>
          <div className="control">
            <button className="button" onClick={addEan}>
              Hinzufügen
            </button>
          </div>
        </div>
      </div>

      <div className="columns">
        <div className="column is-narrow">
          <ArticleImage url={imageUrl || undefined} size={96} />
        </div>
        <div className="column">
          <Field label="Name">
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} />
          </Field>
          <Field label="Marke">
            <input className="input" value={brand} onChange={(e) => setBrand(e.target.value)} />
          </Field>
        </div>
      </div>

      <div className="columns">
        <div className="column">
          <Field label="Packungsgröße">
            <input className="input" value={packageSize} onChange={(e) => setPackageSize(e.target.value)} />
          </Field>
        </div>
        <div className="column">
          <Field label="Kategorie">
            <div className="select is-fullwidth">
              <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
                <option value="">–</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          </Field>
        </div>
        <div className="column">
          <Field label="Mindestbestand" help="0 = keine Einkaufsliste">
            <input className="input" type="number" min={0} value={minStock} onChange={(e) => setMinStock(Number(e.target.value))} />
          </Field>
        </div>
      </div>

      <Field label="Bild-URL">
        <input className="input" value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} />
      </Field>

      {isInbox && (
        <Field label="Mit vorhandenem Artikel zusammenführen" help="Barcodes, Bestand und Historie wandern zum gewählten Artikel.">
          <div className="field has-addons">
            <div className="control is-expanded">
              <div className="select is-fullwidth">
                <select aria-label="Zielartikel" value={mergeTarget} onChange={(e) => setMergeTarget(e.target.value)}>
                  <option value="">Artikel wählen…</option>
                  {articles
                    .filter((a) => a.status === ArticleListDtoStatusEnum.Active)
                    .map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.name}
                      </option>
                    ))}
                </select>
              </div>
            </div>
            <div className="control">
              <button className="button is-warning" disabled={!mergeTarget} onClick={merge}>
                Zusammenführen
              </button>
            </div>
          </div>
        </Field>
      )}
    </Modal>
  );
}
