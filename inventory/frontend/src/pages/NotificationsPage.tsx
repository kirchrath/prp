import { useState } from 'react';
import { useApi } from '../api/ApiContext';
import { formatDate, formatDateTime } from '../api/dates';
import type { CategoryThresholdDto, DueItemDto, SettingsDto } from '../api/notification';
import { Field } from '../components/Field';
import { useNotify } from '../components/Notifications';
import { ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

const STATUS: Record<string, { label: string; tag: string }> = {
  SENT: { label: 'Gesendet', tag: 'is-success' },
  NOTHING_DUE: { label: 'Nichts fällig', tag: 'is-light' },
  FAILED: { label: 'Fehlgeschlagen', tag: 'is-danger' },
};

function DueList({ title, items, tag }: { title: string; items: DueItemDto[]; tag: string }) {
  if (items.length === 0) return null;
  return (
    <>
      <h3 className="title is-6 mt-4 mb-2">{title}</h3>
      <ul>
        {items.map((i) => (
          <li key={`${i.articleId}-${i.bestBefore.toISOString()}`}>
            <span className={`tag ${tag} mr-2`}>{formatDate(i.bestBefore)}</span>
            {i.name}
            {i.brand && <span className="has-text-grey"> ({i.brand})</span>} · {i.quantity}×
          </li>
        ))}
      </ul>
    </>
  );
}

/** Daily best-before digest via ntfy: what is due now, settings, send log. */
export function NotificationsPage() {
  const api = useApi();
  const due = useAsync(() => api.digests.getDue(), [api]);
  const settings = useAsync(() => api.notificationSettings.getNotificationSettings(), [api]);
  const digests = useAsync(() => api.digests.listDigests(), [api]);

  return (
    <>
      <h1 className="title">Benachrichtigungen</h1>
      <p className="mb-4 has-text-grey">
        Einmal täglich kommt eine Zusammenfassung abgelaufener und bald ablaufender Artikel über ntfy aufs Handy.
      </p>
      <div className="columns">
        <div className="column is-6">
          <div className="box">
            <h2 className="title is-5">Aktuell fällig</h2>
            {due.error ? (
              <ErrorBox message={due.error} onRetry={due.reload} />
            ) : !due.data ? (
              <Loading />
            ) : due.data.expired.length + due.data.dueToday.length + due.data.dueSoon.length === 0 ? (
              <p className="has-text-grey">Nichts läuft in nächster Zeit ab.</p>
            ) : (
              <>
                <DueList title="Abgelaufen" items={due.data.expired} tag="is-danger" />
                <DueList title="Läuft heute ab" items={due.data.dueToday} tag="is-warning" />
                <DueList title="Läuft bald ab" items={due.data.dueSoon} tag="is-info is-light" />
              </>
            )}
          </div>
          <div className="box">
            <h2 className="title is-5">Versandprotokoll</h2>
            {digests.error ? (
              <ErrorBox message={digests.error} onRetry={digests.reload} />
            ) : !digests.data ? (
              <Loading />
            ) : digests.data.length === 0 ? (
              <p className="has-text-grey">Noch nichts gesendet.</p>
            ) : (
              <table className="table is-fullwidth is-narrow">
                <tbody>
                  {digests.data.map((d) => (
                    <tr key={d.id}>
                      <td>{formatDate(d.day)}</td>
                      <td>
                        <span className={`tag ${STATUS[d.status]?.tag ?? ''}`}>{STATUS[d.status]?.label ?? d.status}</span>
                        {d.error && <p className="help is-danger">{d.error}</p>}
                      </td>
                      <td className="has-text-grey is-size-7">
                        {d.status === 'NOTHING_DUE' ? '' : `${d.expired} / ${d.dueToday} / ${d.dueSoon}`}
                        <br />
                        {formatDateTime(d.lastModifiedDate)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </div>
        <div className="column is-6">
          {settings.error ? (
            <ErrorBox message={settings.error} onRetry={settings.reload} />
          ) : !settings.data ? (
            <Loading />
          ) : (
            <SettingsForm
              key={settings.data.audit.version}
              settings={settings.data}
              onSaved={() => {
                settings.reload();
                due.reload();
              }}
            />
          )}
        </div>
      </div>
    </>
  );
}

function SettingsForm({ settings, onSaved }: { settings: SettingsDto; onSaved: () => void }) {
  const api = useApi();
  const { notify, fail } = useNotify();
  const categories = useAsync(() => api.categories.listCategories(), [api]);
  const [enabled, setEnabled] = useState(settings.data.enabled);
  const [sendTime, setSendTime] = useState(settings.data.sendTime.substring(0, 5));
  const [defaultDays, setDefaultDays] = useState(settings.data.defaultDays);
  const [thresholds, setThresholds] = useState<CategoryThresholdDto[]>(settings.data.categoryThresholds);
  const [busy, setBusy] = useState(false);

  const unused = (categories.data ?? []).filter((c) => !thresholds.some((t) => t.categoryId === c.id));
  const nameOf = (id: string) => categories.data?.find((c) => c.id === id)?.name ?? 'Gelöschte Kategorie';
  const setDays = (index: number, days: number) =>
    setThresholds(thresholds.map((t, i) => (i === index ? { ...t, days } : t)));

  const save = async () => {
    setBusy(true);
    try {
      await api.notificationSettings.updateNotificationSettings({
        settingsSaveDto: { audit: settings.audit, data: { enabled, sendTime, defaultDays, categoryThresholds: thresholds } },
      });
      notify('success', 'Einstellungen gespeichert');
      onSaved();
    } catch (e) {
      await fail(e);
    } finally {
      setBusy(false);
    }
  };

  const test = async () => {
    try {
      await api.notificationSettings.sendTestNotification();
      notify('success', 'Testnachricht gesendet');
    } catch (e) {
      await fail(e);
    }
  };

  return (
    <div className="box">
      <h2 className="title is-5">Einstellungen</h2>
      <div className="field">
        <label className="checkbox">
          <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} /> Tägliche
          Zusammenfassung senden
        </label>
      </div>
      <Field label="Uhrzeit">
        <input className="input" type="time" required value={sendTime} onChange={(e) => setSendTime(e.target.value)} />
      </Field>
      <Field label="„Läuft bald ab“ – Tage vor dem MHD" help="Gilt für alle Kategorien ohne eigene Schwelle.">
        <input className="input" type="number" min={0} max={365} value={defaultDays}
          onChange={(e) => setDefaultDays(Number(e.target.value))} />
      </Field>

      <p className="label">Eigene Schwelle je Kategorie</p>
      {thresholds.map((t, i) => (
        <div key={t.categoryId} className="field has-addons">
          <div className="control is-expanded">
            <input className="input is-static" readOnly value={nameOf(t.categoryId)} aria-label="Kategorie" />
          </div>
          <div className="control">
            <input className="input" type="number" min={0} max={365} value={t.days} style={{ width: '6rem' }}
              aria-label={`Tage für ${nameOf(t.categoryId)}`} onChange={(e) => setDays(i, Number(e.target.value))} />
          </div>
          <div className="control">
            <button className="button" aria-label={`Schwelle ${nameOf(t.categoryId)} entfernen`}
              onClick={() => setThresholds(thresholds.filter((x) => x !== t))}>
              ✕
            </button>
          </div>
        </div>
      ))}
      {unused.length > 0 && (
        <div className="select is-small mb-4">
          <select aria-label="Kategorie hinzufügen" value=""
            onChange={(e) => setThresholds([...thresholds, { categoryId: e.target.value, days: defaultDays }])}>
            <option value="">+ Kategorie hinzufügen</option>
            {unused.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
      )}

      <div className="buttons mt-4">
        <button className={`button is-primary ${busy ? 'is-loading' : ''}`} disabled={!sendTime} onClick={save}>
          Speichern
        </button>
        <button className="button" onClick={test}>
          Testnachricht senden
        </button>
      </div>
    </div>
  );
}
