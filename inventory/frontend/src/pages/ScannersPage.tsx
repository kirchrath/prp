import { useEffect, useState } from 'react';
import { useApi } from '../api/ApiContext';
import { formatDateTime } from '../api/dates';
import type { LocationDto, ScannerDto } from '../api/generated';
import { LocationSelect } from '../components/LocationSelect';
import { useNotify } from '../components/Notifications';
import { Empty, ErrorBox, Loading } from '../components/Status';
import { useAsync } from '../hooks/useAsync';

export function ScannersPage() {
  const api = useApi();
  const scanners = useAsync(() => api.scanners.listScanners(), [api]);
  const locations = useAsync(() => api.locations.listLocations(), [api]);

  // refresh online state periodically
  useEffect(() => {
    const timer = window.setInterval(scanners.reload, 15_000);
    return () => window.clearInterval(timer);
  }, [scanners.reload]);

  return (
    <>
      <h1 className="title">Scanner</h1>
      <p className="mb-4 has-text-grey">
        ESP32-Scanner melden sich beim ersten Verbinden mit dem MQTT-Broker automatisch an. Weise ihnen hier einen
        Lagerort zu – erst dann buchen ihre Scans.
      </p>
      {scanners.error && <ErrorBox message={scanners.error} onRetry={scanners.reload} />}
      {scanners.loading && !scanners.data ? (
        <Loading />
      ) : (scanners.data ?? []).length === 0 ? (
        <Empty text="Noch kein Scanner hat sich gemeldet." />
      ) : (
        <div className="columns is-multiline">
          {scanners.data!.map((s) => (
            <div key={s.id} className="column is-6">
              <ScannerCard scanner={s} locations={locations.data ?? []} onChanged={scanners.reload} />
            </div>
          ))}
        </div>
      )}
    </>
  );
}

function ScannerCard({ scanner, locations, onChanged }: { scanner: ScannerDto; locations: LocationDto[]; onChanged: () => void }) {
  const api = useApi();
  const { notify, fail } = useNotify();
  const [name, setName] = useState(scanner.name ?? '');
  const [locationId, setLocationId] = useState<number | undefined>(scanner.locationId);
  const dirty = name !== (scanner.name ?? '') || locationId !== scanner.locationId;

  const save = async () => {
    try {
      await api.scanners.updateScanner({ id: scanner.id, scannerUpdateRequest: { name: name || undefined, locationId } });
      notify('success', 'Scanner gespeichert');
      onChanged();
    } catch (e) {
      await fail(e);
    }
  };

  const remove = async () => {
    if (!window.confirm(`Scanner ${scanner.deviceId} entfernen? Er meldet sich beim nächsten Verbinden neu an.`)) return;
    try {
      await api.scanners.deleteScanner({ id: scanner.id });
      onChanged();
    } catch (e) {
      await fail(e);
    }
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
          Geräte-ID <code>{scanner.deviceId}</code>
          {scanner.firmwareVersion && <> · FW {scanner.firmwareVersion}</>}
          {scanner.ipAddress && <> · {scanner.ipAddress}</>}
          <br />
          Zuletzt gesehen: {formatDateTime(scanner.lastSeen)}
        </p>
        <div className="field">
          <label className="label is-small">Name</label>
          <input className="input" value={name} onChange={(e) => setName(e.target.value)} placeholder={scanner.deviceId} />
        </div>
        <div className="field">
          <label className="label is-small">Lagerort</label>
          <LocationSelect locations={locations} value={locationId} onChange={setLocationId} emptyLabel="– nicht zugeordnet –" />
        </div>
      </div>
      <footer className="card-footer">
        <a className={`card-footer-item${dirty ? '' : ' has-text-grey-light'}`} onClick={dirty ? save : undefined}>
          Speichern
        </a>
        <a className="card-footer-item has-text-danger" onClick={remove}>
          Entfernen
        </a>
      </footer>
    </div>
  );
}
