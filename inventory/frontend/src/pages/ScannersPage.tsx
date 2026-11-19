import { useEffect, useState } from 'react';
import { useApi } from '../api/ApiContext';
import { formatDateTime } from '../api/dates';
import type { ArticleListDto } from '../api/catalog';
import type { ButtonDto, ScannerListDto } from '../api/device';
import type { LocationListDto } from '../api/stock';
import { LocationSelect } from '../components/LocationSelect';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

export function ScannersPage() {
  const api = useApi();
  const scanners = useAsync(() => api.scanners.listScanners(), [api]);
  const locations = useAsync(() => api.locations.listLocations(), [api]);
  const articles = useAsync(() => api.articles.listArticles({}), [api]);

  // refresh the online state periodically
  useEffect(() => {
    const timer = window.setInterval(scanners.reload, 15_000);
    return () => window.clearInterval(timer);
  }, [scanners.reload]);

  let content;
  if (scanners.error) {
    content = <ErrorBox message={scanners.error} onRetry={scanners.reload} />;
  } else if (!scanners.data) {
    content = <Loading />;
  } else if (scanners.data.length === 0) {
    content = <Empty text="Noch kein Gerät hat sich gemeldet." />;
  } else {
    content = (
      <div className="columns is-multiline">
        {scanners.data.map((s) => (
          <div key={s.id} className="column is-6">
            <ScannerCard scanner={s} locations={locations.data ?? []} articles={articles.data ?? []} onChanged={scanners.reload} />
          </div>
        ))}
      </div>
    );
  }

  return (
    <>
      <h1 className="title">Geräte</h1>
      <p className="mb-4 has-text-grey">
        Scanner und Knopfleisten (ESP32) melden sich beim ersten Verbinden mit dem MQTT-Broker automatisch an. Weise ihnen
        hier einen Lagerort zu – erst dann buchen sie. Bei Knopfleisten bucht jeder Taster einen festen Artikel: kurz
        drücken = 1 einlagern, lang drücken (1 s) = 1 entnehmen.
      </p>
      {content}
    </>
  );
}

const BUTTONS = [1, 2, 3, 4, 5, 6, 7, 8];

const sameButtons = (a: ButtonDto[], b: ButtonDto[]) =>
  a.length === b.length && a.every((x) => b.some((y) => y.button === x.button && y.articleId === x.articleId));

interface CardProps {
  scanner: ScannerListDto;
  locations: LocationListDto[];
  articles: ArticleListDto[];
  onChanged: () => void;
}

function ScannerCard({ scanner, locations, articles, onChanged }: CardProps) {
  const api = useApi();
  const { notify, fail } = useNotify();
  const [name, setName] = useState(scanner.name ?? '');
  const [locationId, setLocationId] = useState(scanner.locationId);
  const [buttons, setButtons] = useState<ButtonDto[]>(scanner.buttons);
  const isBox = scanner.kind === 'BUTTONS';
  const label = isBox ? 'Knopfleiste' : 'Scanner';
  const dirty = name !== (scanner.name ?? '') || locationId !== scanner.locationId || !sameButtons(buttons, scanner.buttons);
  const sorted = [...articles].sort((a, b) => a.name.localeCompare(b.name, 'de'));

  const assign = (button: number, articleId: string) =>
    setButtons([...buttons.filter((b) => b.button !== button), ...(articleId ? [{ button, articleId }] : [])]);

  const run = async (action: () => Promise<unknown>, message: string) => {
    try {
      await action();
      notify('success', message);
      onChanged();
    } catch (e) {
      await fail(e);
    }
  };

  const save = () =>
    run(async () => {
      const { audit, data } = await api.scanners.getScanner({ id: scanner.id });
      await api.scanners.updateScanner({
        id: scanner.id,
        scannerSaveDto: { audit, data: { ...data, name: name || undefined, locationId, buttons } },
      });
    }, `${label} gespeichert`);

  const remove = () => {
    if (!window.confirm(`${label} ${scanner.deviceId} entfernen? Das Gerät meldet sich beim nächsten Verbinden neu an.`)) return;
    void run(async () => {
      const { audit } = await api.scanners.getScanner({ id: scanner.id });
      await api.scanners.deleteScanner({ id: scanner.id, version: audit.version });
    }, `${label} entfernt`);
  };

  return (
    <div className="card">
      <header className="card-header">
        <p className="card-header-title">
          <span className={`tag mr-2 ${scanner.online ? 'is-success' : 'is-light'}`}>{scanner.online ? 'online' : 'offline'}</span>
          {scanner.name ?? scanner.deviceId}
          <span className="tag is-info is-light ml-2">{label}</span>
        </p>
      </header>
      <div className="card-content">
        <p className="is-size-7 has-text-grey mb-3">
          Geräte-ID <code>{scanner.deviceId}</code> · FW {scanner.firmwareVersion ?? '–'} · {scanner.ipAddress ?? '–'}
          <br />
          Zuletzt gesehen: {formatDateTime(scanner.lastSeen)}
        </p>
        <div className="field">
          <label className="label is-small">
            Name
            <input className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder={scanner.deviceId} />
          </label>
        </div>
        <div className="field">
          <p className="label is-small">Lagerort</p>
          <LocationSelect locations={locations} value={locationId} onChange={setLocationId} emptyLabel="– nicht zugeordnet –" />
        </div>
        {isBox &&
          BUTTONS.map((button) => (
            <div key={button} className="field is-horizontal">
              <div className="field-label is-small">
                <label className="label" htmlFor={`${scanner.id}-b${button}`}>
                  Taster {button}
                </label>
              </div>
              <div className="field-body">
                <div className="select is-small is-fullwidth">
                  <select id={`${scanner.id}-b${button}`} value={buttons.find((b) => b.button === button)?.articleId ?? ''}
                    onChange={(e) => assign(button, e.target.value)}>
                    <option value="">– frei –</option>
                    {sorted.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.name}
                        {a.brand ? ` (${a.brand})` : ''}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>
          ))}
      </div>
      <footer className="card-footer">
        <button className="card-footer-item button is-white" disabled={!dirty} onClick={save}>
          Speichern
        </button>
        <button className="card-footer-item button is-white has-text-danger" onClick={remove}>
          Entfernen
        </button>
      </footer>
    </div>
  );
}
