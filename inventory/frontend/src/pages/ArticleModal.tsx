import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import { ArticleDtoStatusEnum, type ArticleDto, type CategoryDto } from '../api/generated';
import { ArticleImage } from '../components/ArticleImage';
import { Field } from '../components/Field';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/Notifications';

interface Props {
  article?: ArticleDto;
  categories: CategoryDto[];
  /** All articles, used as merge targets for inbox entries. */
  articles: ArticleDto[];
  initialEan?: string;
  onClose: () => void;
  onSaved: () => void;
}

export function ArticleModal({ article, categories, articles, initialEan, onClose, onSaved }: Props) {
  const api = useApi();
  const { notify, fail } = useNotify();
  const isInbox = article?.status === ArticleDtoStatusEnum.Unknown;
  const [name, setName] = useState(isInbox ? '' : (article?.name ?? ''));
  const [brand, setBrand] = useState(article?.brand ?? '');
  const [packageSize, setPackageSize] = useState(article?.packageSize ?? '');
  const [imageUrl, setImageUrl] = useState(article?.imageUrl ?? '');
  const [categoryId, setCategoryId] = useState<number | undefined>(article?.categoryId);
  const [minStock, setMinStock] = useState(article?.minStock ?? 0);
  const [eans, setEans] = useState<string[]>(article?.eans ?? (initialEan ? [initialEan] : []));
  const [newEan, setNewEan] = useState('');
  const [mergeTarget, setMergeTarget] = useState<number>();
  const [busy, setBusy] = useState(false);

  const lookup = async (ean: string) => {
    setBusy(true);
    try {
      const result = await api.articles.lookupProduct({ ean });
      if (result.existingArticleId && result.existingArticleId !== article?.id) {
        notify('warning', 'Diese EAN gehört bereits zu einem anderen Artikel.');
      }
      if (result.found) {
        setName(result.name ?? name);
        setBrand(result.brand ?? brand);
        setPackageSize(result.packageSize ?? packageSize);
        setImageUrl(result.imageUrl ?? imageUrl);
        notify('success', `Gefunden in ${result.source?.replaceAll('_', ' ').toLowerCase()}`);
      } else {
        notify('info', 'Keine Produktdaten gefunden.');
      }
    } catch (e) {
      await fail(e);
    } finally {
      setBusy(false);
    }
  };

  const addEan = () => {
    const code = newEan.trim();
    if (code && !eans.includes(code)) {
      setEans([...eans, code]);
    }
    setNewEan('');
  };

  const save = async () => {
    setBusy(true);
    try {
      const articleRequest = {
        name,
        brand: brand || undefined,
        packageSize: packageSize || undefined,
        imageUrl: imageUrl || undefined,
        categoryId,
        minStock,
        eans,
      };
      if (article) await api.articles.updateArticle({ id: article.id, articleRequest });
      else await api.articles.createArticle({ articleRequest });
      notify('success', 'Artikel gespeichert');
      onSaved();
    } catch (e) {
      await fail(e);
    } finally {
      setBusy(false);
    }
  };

  const merge = async () => {
    if (!article || !mergeTarget) return;
    setBusy(true);
    try {
      const target = await api.articles.mergeArticle({ id: article.id, targetId: mergeTarget });
      notify('success', `Zusammengeführt mit ${target.name}`);
      onSaved();
    } catch (e) {
      await fail(e);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      title={article ? (isInbox ? 'Unbekannten Artikel ergänzen' : 'Artikel bearbeiten') : 'Neuer Artikel'}
      onClose={onClose}
      footer={
        <>
          <button className="button" onClick={onClose}>
            Abbrechen
          </button>
          <button className={`button is-primary${busy ? ' is-loading' : ''}`} disabled={!name.trim()} onClick={save}>
            Speichern
          </button>
        </>
      }
    >
      {isInbox && (
        <div className="notification is-warning is-light">
          Dieser Artikel wurde per Scan angelegt, aber in keiner Produktdatenbank gefunden. Bitte Namen ergänzen –
          oder ihn unten mit einem vorhandenen Artikel zusammenführen.
        </div>
      )}

      <Field label="EAN / Barcodes">
        <div className="tags mb-2">
          {eans.map((e) => (
            <span key={e} className="tag is-medium">
              {e}
              <button className="delete is-small" onClick={() => setEans(eans.filter((x) => x !== e))} />
              <button className="button is-small is-ghost px-1" title="Produktdaten nachschlagen" onClick={() => lookup(e)}>
                🔎
              </button>
            </span>
          ))}
        </div>
        <div className="field has-addons">
          <div className="control is-expanded">
            <input
              className="input"
              inputMode="numeric"
              placeholder="EAN hinzufügen"
              value={newEan}
              onChange={(e) => setNewEan(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && addEan()}
            />
          </div>
          <div className="control">
            <button className="button" onClick={addEan} disabled={!newEan.trim()}>
              Hinzufügen
            </button>
          </div>
        </div>
      </Field>

      <div className="columns">
        <div className="column is-narrow">
          <ArticleImage url={imageUrl || undefined} size={96} />
        </div>
        <div className="column">
          <Field label="Name">
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} autoFocus />
          </Field>
          <Field label="Marke">
            <input className="input" value={brand} onChange={(e) => setBrand(e.target.value)} />
          </Field>
        </div>
      </div>

      <div className="columns">
        <div className="column">
          <Field label="Packungsgröße">
            <input className="input" placeholder="z. B. 500 g" value={packageSize} onChange={(e) => setPackageSize(e.target.value)} />
          </Field>
        </div>
        <div className="column">
          <Field label="Kategorie">
            <div className="select is-fullwidth">
              <select value={categoryId ?? ''} onChange={(e) => setCategoryId(e.target.value ? Number(e.target.value) : undefined)}>
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
        <>
          <hr />
          <Field label="Mit vorhandenem Artikel zusammenführen" help="Barcodes, Bestand und Historie wandern zum gewählten Artikel.">
            <div className="field has-addons">
              <div className="control is-expanded">
                <div className="select is-fullwidth">
                  <select value={mergeTarget ?? ''} onChange={(e) => setMergeTarget(e.target.value ? Number(e.target.value) : undefined)}>
                    <option value="">Artikel wählen…</option>
                    {articles
                      .filter((a) => a.id !== article!.id && a.status === ArticleDtoStatusEnum.Active)
                      .map((a) => (
                        <option key={a.id} value={a.id}>
                          {a.name}
                          {a.brand ? ` (${a.brand})` : ''}
                        </option>
                      ))}
                  </select>
                </div>
              </div>
              <div className="control">
                <button className="button is-warning" disabled={!mergeTarget || busy} onClick={merge}>
                  Zusammenführen
                </button>
              </div>
            </div>
          </Field>
        </>
      )}
    </Modal>
  );
}
