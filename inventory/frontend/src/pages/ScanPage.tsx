import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { BrowserMultiFormatReader, type IScannerControls } from '@zxing/browser';
import { BarcodeFormat, DecodeHintType } from '@zxing/library';
import { useApi } from '../api/ApiContext';
import { scanAndWait } from '../api/bookings';
import { errorMessage } from '../api/errors';
import { BookingDataDtoStatusEnum } from '../api/stock';
import { LocationSelect } from '../components/LocationSelect';
import { useAsync } from '../hooks/useAsync';

const LOCATION_KEY = 'scan.locationId';
const SAME_CODE_PAUSE_MS = 2500;

const STYLE: Partial<Record<string, string>> = {
  [BookingDataDtoStatusEnum.InOk]: 'is-success',
  [BookingDataDtoStatusEnum.InOkNewArticle]: 'is-success',
  [BookingDataDtoStatusEnum.InOkUnknownArticle]: 'is-warning',
  [BookingDataDtoStatusEnum.OutOk]: 'is-link',
};

interface Entry {
  ean: string;
  at: Date;
  status: string;
  article?: string;
  quantity?: number;
  message: string;
}

function readStoredLocation(): string | undefined {
  try {
    return window.localStorage.getItem(LOCATION_KEY) ?? undefined;
  } catch {
    return undefined;
  }
}

function storeLocation(id: string | undefined) {
  try {
    window.localStorage.setItem(LOCATION_KEY, id ?? '');
  } catch {
    // storage not available
  }
}

function feedback(success: boolean) {
  try {
    const ctx = new AudioContext();
    const osc = ctx.createOscillator();
    osc.frequency.value = success ? 1500 : 300;
    osc.connect(ctx.destination);
    osc.start();
    osc.stop(ctx.currentTime + (success ? 0.12 : 0.4));
    osc.onended = () => void ctx.close();
  } catch {
    // audio not available
  }
  navigator.vibrate?.(success ? 80 : [100, 60, 100]);
}

/** Scan barcodes with the phone camera; each scan books exactly one unit. */
export function ScanPage() {
  const api = useApi();
  const locations = useAsync(() => api.locations.listLocations(), [api]);
  const [locationId, setLocationId] = useState(readStoredLocation);
  const [direction, setDirection] = useState<'IN' | 'OUT'>('IN');
  const [cameraOn, setCameraOn] = useState(false);
  const [cameraError, setCameraError] = useState<string>();
  const [manualEan, setManualEan] = useState('');
  const [history, setHistory] = useState<Entry[]>([]);

  const videoRef = useRef<HTMLVideoElement>(null);
  const lastCode = useRef({ code: '', at: 0 });
  // the decode callback is registered once; it reads the latest settings through this ref
  const settings = useRef({ locationId, direction });
  settings.current = { locationId, direction };

  const book = useCallback(
    async (ean: string) => {
      const { locationId: loc, direction: dir } = settings.current;
      let entry: Entry;
      try {
        const b = await scanAndWait(api, ean, loc!, dir);
        entry = { ean, at: new Date(), status: b.status, article: b.articleName, quantity: b.quantityAtLocation, message: b.message };
      } catch (e) {
        entry = { ean, at: new Date(), status: 'ERROR', message: await errorMessage(e) };
      }
      feedback(STYLE[entry.status] !== undefined);
      setHistory((h) => [entry, ...h].slice(0, 20));
    },
    [api],
  );

  useEffect(() => {
    if (!cameraOn) return;
    const hints = new Map([[DecodeHintType.POSSIBLE_FORMATS, [BarcodeFormat.EAN_13, BarcodeFormat.EAN_8,
      BarcodeFormat.UPC_A, BarcodeFormat.UPC_E, BarcodeFormat.CODE_128]]]);
    const reader = new BrowserMultiFormatReader(hints);
    let controls: IScannerControls | undefined;
    let stopped = false;
    reader
      .decodeFromConstraints({ video: { facingMode: 'environment' } }, videoRef.current!, (result) => {
        if (!result) return;
        const code = result.getText();
        const now = Date.now();
        if (code === lastCode.current.code && now - lastCode.current.at < SAME_CODE_PAUSE_MS) return;
        lastCode.current = { code, at: now };
        void book(code);
      })
      .then((c) => {
        controls = c;
        if (stopped) c.stop();
      })
      .catch((e: Error) => {
        setCameraError(window.isSecureContext ? e.message : 'Die Kamera ist nur über HTTPS verfügbar.');
        setCameraOn(false);
      });
    return () => {
      stopped = true;
      controls?.stop();
    };
  }, [cameraOn, book]);

  const isIn = direction === 'IN';

  return (
    <>
      <h1 className="title">Scannen</h1>
      <div className="columns">
        <div className="column is-6">
          <div className="field">
            <p className="label">Lagerort</p>
            <LocationSelect
              locations={locations.data ?? []}
              value={locationId}
              onChange={(id) => {
                setLocationId(id);
                storeLocation(id);
              }}
            />
          </div>
          <div className="field">
            <p className="label">Richtung</p>
            <div className="buttons has-addons">
              <button className={`button is-medium ${isIn ? 'is-success is-selected' : ''}`} onClick={() => setDirection('IN')}>
                ⬇ Einlagern
              </button>
              <button className={`button is-medium ${isIn ? '' : 'is-link is-selected'}`} onClick={() => setDirection('OUT')}>
                ⬆ Auslagern
              </button>
            </div>
          </div>

          {cameraError && <div className="notification is-danger is-light">{cameraError}</div>}
          {cameraOn ? (
            <>
              <video ref={videoRef} className="scan-video" muted playsInline data-testid="video" />
              <button className="button is-fullwidth mt-2" onClick={() => setCameraOn(false)}>
                Kamera stoppen
              </button>
            </>
          ) : (
            <button
              className="button is-primary is-medium is-fullwidth"
              disabled={!locationId}
              onClick={() => {
                setCameraError(undefined);
                setCameraOn(true);
              }}
            >
              📷 Kamera starten
            </button>
          )}

          <form
            className="field has-addons mt-4"
            onSubmit={(e) => {
              e.preventDefault();
              void book(manualEan.trim());
              setManualEan('');
            }}
          >
            <div className="control is-expanded">
              <input className="input" inputMode="numeric" placeholder="EAN manuell eingeben" value={manualEan}
                onChange={(e) => setManualEan(e.target.value)} />
            </div>
            <div className="control">
              <button className="button" type="submit" disabled={!locationId || !manualEan.trim()}>
                Buchen
              </button>
            </div>
          </form>
        </div>

        <div className="column is-6">
          <h2 className="title is-5">Letzte Scans</h2>
          {history.length === 0 && <p className="has-text-grey">Noch nichts gescannt.</p>}
          {history.map((h, i) => (
            <div key={i} className={`notification is-light ${STYLE[h.status] ?? 'is-danger'} py-3`}>
              <strong>{h.article ?? h.ean}</strong>
              {h.quantity !== undefined && <span className="tag ml-2">Bestand am Ort: {h.quantity}</span>}
              <br />
              <small>
                {h.message} · {h.at.toLocaleTimeString('de-DE')}
              </small>
              {h.status === BookingDataDtoStatusEnum.InOkUnknownArticle && (
                <>
                  {' '}
                  · <Link to="/artikel?status=UNKNOWN">In der Inbox ergänzen</Link>
                </>
              )}
            </div>
          ))}
        </div>
      </div>
    </>
  );
}
