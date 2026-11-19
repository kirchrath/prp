import { useEffect, useState } from 'react';
import { useApi } from '../api/ApiContext';
import { formatDateTime } from '../api/dates';
import type { ScannerListDto } from '../api/device';
import type { LocationListDto } from '../api/stock';
import { LocationSelect } from '../components/LocationSelect';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

export function ScannersPage() {
  const api = useApi();
  const scanners = useAsync(() => api.scanners.listScanners(), [api]);
  const locations = useAsync(() => api.locations.listLocations(), [api]);

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
    content = <Empty text="Noch kein Scanner hat sich gemeldet." />;
  } else {
    content = (
      <div className="columns is-multiline">
        {scanners.data.map((s) => (
          <div key={s.id} className="column is-6">
            <ScannerCard scanner={s} locations={locations.data ?? []} onChanged={scanners.reload} />
          </div>
        ))}
      </div>
    );
  }

  return (
    <>
      <h1 className="title">Scanner</h1>
      <p className="mb-4 has-text-grey">
        ESP32-Scanner melden sich beim ersten Verbinden mit dem MQTT-Broker automatisch an. Weise ihnen hier einen Lagerort
        zu – erst dann buchen ihre Scans.
      </p>
      {content}
    </>
  );
}

function ScannerCard({ scanner, locations, onChanged }: { scanner: ScannerListDto; locations: LocationListDto[]; onChanged: () => void }) {
  const api = useApi();
  const { notify, fail } = useNotify();
  const [name, setName] = useState(scanner.name ?? '');
  const [locationId, setLocationId] = useState(scanner.locationId);
  const dirty = name !== (scanner.name ?? '') || locationId !== scanner.locationId;

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
      await api.scanners.updateScanner({ id: scanner.id, scannerSaveDto: { audit, data: { ...data, name: name || undefined, locationId } } });
    }, 'Scanner gespeichert');

  const remove = () => {
    if (!window.confirm(`Scanner ${scanner.deviceId} entfernen? Er meldet sich beim nächsten Verbinden neu an.`)) return;
    void run(async () => {
      const { audit } = await api.scanners.getScanner({ id: scanner.id });
      await api.scanners.deleteScanner({ id: scanner.id, version: audit.version });
    }, 'Scanner entfernt');
  };

  return (
    <div className="card">
      <header className="card-header">
        <p className="card-header-title">
          <span className={`tag mr-2 ${scanner.online ? 'is-success' : 'is-light'}`}>{scanner.online ? 'online' : 'offline'}</span>
          {scanner.name ?? scanner.deviceId}
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
