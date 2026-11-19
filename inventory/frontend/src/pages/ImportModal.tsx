import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import type { RecipeDataDto } from '../api/recipe';
import { Modal } from '../components/Modal';
import { useNotify } from '../components/Notifications';

/** Imports a recipe from a link or a free text; the result is opened as draft in the editor. */
export function ImportModal({ onClose, onDraft }: { onClose: () => void; onDraft: (draft: RecipeDataDto) => void }) {
  const api = useApi();
  const { fail } = useNotify();
  const [mode, setMode] = useState<'url' | 'text'>('url');
  const [url, setUrl] = useState('');
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);
  const input = mode === 'url' ? url.trim() : text.trim();

  const submit = async () => {
    setBusy(true);
    try {
      onDraft(
        mode === 'url'
          ? await api.recipeImports.importRecipeFromUrl({ urlImportDto: { url: input } })
          : await api.recipeImports.importRecipeFromText({ textImportDto: { text: input } }),
      );
    } catch (e) {
      await fail(e);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      title="Rezept importieren"
      onClose={onClose}
      footer={
        <>
          <button className="button" onClick={onClose}>
            Abbrechen
          </button>
          <button className={`button is-primary${busy ? ' is-loading' : ''}`} disabled={!input || busy} onClick={submit}>
            Importieren
          </button>
        </>
      }
    >
      <div className="tabs">
        <ul>
          <li className={mode === 'url' ? 'is-active' : ''}>
            <a onClick={() => setMode('url')}>Link</a>
          </li>
          <li className={mode === 'text' ? 'is-active' : ''}>
            <a onClick={() => setMode('text')}>Freitext</a>
          </li>
        </ul>
      </div>
      {mode === 'url' ? (
        <input className="input" type="url" placeholder="https://…" aria-label="Link zum Rezept" value={url}
          onChange={(e) => setUrl(e.target.value)} />
      ) : (
        <textarea className="textarea" rows={10} placeholder="Rezept hier einfügen…" aria-label="Rezepttext" value={text}
          onChange={(e) => setText(e.target.value)} />
      )}
      <p className="help mt-3">
        Die KI bringt das Rezept in Form und schlägt passende Artikel vor. Du prüfst das Ergebnis, bevor es gespeichert wird.
        {busy && ' Das kann einige Sekunden dauern …'}
      </p>
    </Modal>
  );
}
