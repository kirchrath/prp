import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import type { ArticleListDto } from '../api/catalog';
import type { CategoryListDto, IngredientDtoUnitEnum, RecipeDataDto, RecipeDto } from '../api/recipe';
import { UNIT_LABELS } from '../api/units';
import { Field } from '../components/Field';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/Notifications';

interface Row {
  name: string;
  amount: string;
  unit: string;
  articleId: string;
  basic: boolean;
}

interface Props {
  /** Saved recipe to edit … */
  existing?: { id: string; recipe: RecipeDto };
  /** … or an unsaved draft (new or imported). */
  draft?: RecipeDataDto;
  categories: CategoryListDto[];
  articles: ArticleListDto[];
  onClose: () => void;
  onSaved: () => void;
}

const EMPTY_ROW: Row = { name: '', amount: '', unit: '', articleId: '', basic: false };

export function RecipeEditor({ existing, draft, categories, articles, onClose, onSaved }: Props) {
  const api = useApi();
  const { notify, fail } = useNotify();
  const data = existing?.recipe.data ?? draft;
  const [title, setTitle] = useState(data?.title ?? '');
  const [description, setDescription] = useState(data?.description ?? '');
  const [servings, setServings] = useState(data?.servings ?? 4);
  const [prep, setPrep] = useState(data?.prepMinutes?.toString() ?? '');
  const [cook, setCook] = useState(data?.cookMinutes?.toString() ?? '');
  const [imageUrl, setImageUrl] = useState(data?.imageUrl ?? '');
  const [categoryId, setCategoryId] = useState(data?.categoryId ?? '');
  const [tags, setTags] = useState((data?.tags ?? []).join(', '));
  const [steps, setSteps] = useState((data?.steps ?? []).join('\n'));
  const [rows, setRows] = useState<Row[]>(
    (data?.ingredients ?? []).map((i) => ({
      name: i.name,
      amount: i.amount?.toString() ?? '',
      unit: i.unit ?? '',
      articleId: i.articleId ?? '',
      basic: i.basic,
    })),
  );

  const change = (index: number, patch: Partial<Row>) => setRows(rows.map((r, i) => (i === index ? { ...r, ...patch } : r)));

  const run = async (action: () => Promise<unknown>, message: string) => {
    try {
      await action();
      notify('success', message);
      onSaved();
    } catch (e) {
      await fail(e);
    }
  };

  const save = () => {
    const recipeDataDto: RecipeDataDto = {
      title,
      description: description || undefined,
      servings,
      prepMinutes: prep ? Number(prep) : undefined,
      cookMinutes: cook ? Number(cook) : undefined,
      imageUrl: imageUrl || undefined,
      sourceUrl: data?.sourceUrl,
      categoryId: categoryId || undefined,
      tags: tags.split(',').map((t) => t.trim()).filter(Boolean),
      steps: steps.split('\n').map((s) => s.trim()).filter(Boolean),
      ingredients: rows
        .filter((r) => r.name.trim())
        .map((r) => ({
          name: r.name,
          amount: r.amount ? Number(r.amount) : undefined,
          unit: r.amount && r.unit ? (r.unit as IngredientDtoUnitEnum) : undefined,
          articleId: r.articleId || undefined,
          basic: r.basic,
        })),
    };
    return run(
      () =>
        existing
          ? api.recipes.updateRecipe({ id: existing.id, recipeSaveDto: { audit: existing.recipe.audit, data: recipeDataDto } })
          : api.recipes.createRecipe({ recipeSaveDto: { data: recipeDataDto } }),
      'Rezept gespeichert',
    );
  };

  const remove = () => {
    if (!window.confirm(`Rezept "${title}" löschen?`)) return;
    void run(() => api.recipes.deleteRecipe({ id: existing!.id, version: existing!.recipe.audit.version }), 'Rezept gelöscht');
  };

  return (
    <Modal
      title={existing ? 'Rezept bearbeiten' : 'Neues Rezept'}
      onClose={onClose}
      footer={
        <>
          {existing && (
            <button className="button is-danger is-light mr-auto" onClick={remove}>
              Löschen
            </button>
          )}
          <button className="button" onClick={onClose}>
            Abbrechen
          </button>
          <button className="button is-primary" disabled={!title.trim() || servings < 1} onClick={save}>
            Speichern
          </button>
        </>
      }
    >
      {data?.sourceUrl && (
        <p className="mb-3">
          Quelle: <a href={data.sourceUrl} target="_blank" rel="noreferrer">{data.sourceUrl}</a>
        </p>
      )}
      <Field label="Titel">
        <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} />
      </Field>
      <Field label="Beschreibung">
        <textarea className="textarea" rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
      </Field>
      <div className="columns is-multiline">
        <div className="column is-3">
          <Field label="Personen">
            <input className="input" type="number" min={1} value={servings} onChange={(e) => setServings(Number(e.target.value))} />
          </Field>
        </div>
        <div className="column is-3">
          <Field label="Vorbereitung (min)">
            <input className="input" type="number" min={0} value={prep} onChange={(e) => setPrep(e.target.value)} />
          </Field>
        </div>
        <div className="column is-3">
          <Field label="Kochen/Backen (min)">
            <input className="input" type="number" min={0} value={cook} onChange={(e) => setCook(e.target.value)} />
          </Field>
        </div>
        <div className="column is-3">
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
      </div>
      <Field label="Tags" help="Durch Komma getrennt, z. B. vegetarisch, schnell">
        <input className="input" value={tags} onChange={(e) => setTags(e.target.value)} />
      </Field>
      <Field label="Bild-URL">
        <input className="input" value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} />
      </Field>

      <p className="label">Zutaten</p>
      <p className="help mb-2">
        Mengen gelten für die angegebene Personenzahl. „Basis“ = immer im Haus (Salz, Wasser …), zählt nie als fehlend.
      </p>
      {rows.map((r, i) => (
        <div key={i} className="columns is-mobile is-multiline is-variable is-1 mb-0">
          <div className="column is-12-mobile is-3-tablet">
            <input className="input is-small" aria-label={`Zutat ${i + 1}`} placeholder="Zutat" value={r.name}
              onChange={(e) => change(i, { name: e.target.value })} />
          </div>
          <div className="column is-3-mobile is-2-tablet">
            <input className="input is-small" type="number" min={0} step="any" aria-label={`Menge ${i + 1}`} value={r.amount}
              onChange={(e) => change(i, { amount: e.target.value })} />
          </div>
          <div className="column is-3-mobile is-2-tablet">
            <div className="select is-small is-fullwidth">
              <select aria-label={`Einheit ${i + 1}`} value={r.unit} onChange={(e) => change(i, { unit: e.target.value })}>
                <option value="">–</option>
                {Object.entries(UNIT_LABELS).map(([unit, label]) => (
                  <option key={unit} value={unit}>
                    {label || 'sonstige'}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="column is-6-mobile is-3-tablet">
            <div className="select is-small is-fullwidth">
              <select aria-label={`Artikel ${i + 1}`} value={r.articleId} onChange={(e) => change(i, { articleId: e.target.value })}>
                <option value="">– kein Artikel –</option>
                {articles.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="column is-narrow">
            <label className="checkbox is-size-7">
              <input type="checkbox" checked={r.basic} onChange={(e) => change(i, { basic: e.target.checked })} /> Basis
            </label>
            <button className="delete ml-2" aria-label={`Zutat ${i + 1} entfernen`} onClick={() => setRows(rows.filter((_, j) => j !== i))} />
          </div>
        </div>
      ))}
      <button className="button is-small mb-4" onClick={() => setRows([...rows, EMPTY_ROW])}>
        + Zutat
      </button>

      <Field label="Zubereitung" help="Ein Schritt pro Zeile">
        <textarea className="textarea" rows={6} value={steps} onChange={(e) => setSteps(e.target.value)} />
      </Field>
    </Modal>
  );
}
