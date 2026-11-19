import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router';
import { BrowserMultiFormatReader, type IScannerControls } from '@zxing/browser';
import { BarcodeFormat, DecodeHintType } from '@zxing/library';
import { useApi } from '../api/ApiContext';
import { ScanRequestDirectionEnum, ScanResultOutcomeEnum, type ScanResult } from '../api/generated';
import { errorMessage } from '../api/errors';
import { LocationSelect } from '../components/LocationSelect';
import { useAsync } from '../hooks/useAsync';

const LOCATION_KEY = 'scan.locationId';
const SAME_CODE_PAUSE_MS = 2500;

const OUTCOME_STYLE: Record<ScanResultOutcomeEnum, string> = {
  [ScanResultOutcomeEnum.InOk]: 'is-success',
  [ScanResultOutcomeEnum.InOkNewArticle]: 'is-success',
  [ScanResultOutcomeEnum.InOkUnknownArticle]: 'is-warning',
  [ScanResultOutcomeEnum.OutOk]: 'is-link',
  [ScanResultOutcomeEnum.UnknownEan]: 'is-danger',
  [ScanResultOutcomeEnum.NotInStock]: 'is-danger',
  [ScanResultOutcomeEnum.NoLocation]: 'is-danger',
  [ScanResultOutcomeEnum.Error]: 'is-danger',
};

function readStoredLocation(): number | undefined {
  try {
    const v = window.localStorage.getItem(LOCATION_KEY);
    return v ? Number(v) : undefined;
  } catch {
    return undefined;
  }
}

function feedback(success: boolean) {
  try {
    const ctx = new AudioContext();
    const osc = ctx.createOscillator();
    const gain = ctx.createGain();
    osc.frequency.value = success ? 1500 : 300;
    gain.gain.value = 0.15;
    osc.connect(gain).connect(ctx.destination);
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
  const [locationId, setLocationId] = useState<number | undefined>(readStoredLocation);
  const [direction, setDirection] = useState<ScanRequestDirectionEnum>(ScanRequestDirectionEnum.In);
  const [cameraOn, setCameraOn] = useState(false);
  const [cameraError, setCameraError] = useState<string>();
  const [manualEan, setManualEan] = useState('');
  const [history, setHistory] = useState<(ScanResult & { ean: string; at: Date })[]>([]);
  const [busy, setBusy] = useState(false);

  const videoRef = useRef<HTMLVideoElement>(null);
  const lastCode = useRef<{ code: string; at: number }>({ code: '', at: 0 });
  // the decode callback is registered once; read the latest settings through a ref
  const settings = useRef({ locationId, direction });
  settings.current = { locationId, direction };

  useEffect(() => {
    try {
      if (locationId) window.localStorage.setItem(LOCATION_KEY, String(locationId));
    } catch {
      // storage not available
    }
  }, [locationId]);

  const book = useCallback(
    async (ean: string) => {
      const { locationId: loc, direction: dir } = settings.current;
      if (!loc) return;
      setBusy(true);
      try {
        const result = await api.stock.scan({ scanRequest: { ean, locationId: loc, direction: dir } });
        const ok = result.outcome !== ScanResultOutcomeEnum.UnknownEan && result.outcome !== ScanResultOutcomeEnum.NotInStock
          && result.outcome !== ScanResultOutcomeEnum.Error && result.outcome !== ScanResultOutcomeEnum.NoLocation;
        feedback(ok);
        setHistory((h) => [{ ...result, ean, at: new Date() }, ...h].slice(0, 20));
      } catch (e) {
        feedback(false);
        setHistory((h) =>
          [{ outcome: ScanResultOutcomeEnum.Error, message: '', ean, at: new Date() } as ScanResult & { ean: string; at: Date }, ...h]
            .slice(0, 20),
        );
        void errorMessage(e).then((message) =>
          setHistory((h) => h.map((x, i) => (i === 0 ? { ...x, message } : x))),
        );
      } finally {
        setBusy(false);
      }
    },
    [api],
  );

  useEffect(() => {
    if (!cameraOn || !videoRef.current) return;
    const hints = new Map();
    hints.set(DecodeHintType.POSSIBLE_FORMATS, [
      BarcodeFormat.EAN_13,
      BarcodeFormat.EAN_8,
      BarcodeFormat.UPC_A,
      BarcodeFormat.UPC_E,
      BarcodeFormat.CODE_128,
    ]);
    const reader = new BrowserMultiFormatReader(hints);
    let controls: IScannerControls | undefined;
    let stopped = false;
    reader
      .decodeFromConstraints({ video: { facingMode: 'environment' } }, videoRef.current, (result) => {
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
        setCameraError(
          window.isSecureContext ? e.message : 'Die Kamera ist nur über HTTPS verfügbar.',
        );
        setCameraOn(false);
      });
    return () => {
      stopped = true;
      controls?.stop();
    };
  }, [cameraOn, book]);

  const isIn = direction === ScanRequestDirectionEnum.In;

  return (
    <>
      <h1 className="title">Scannen</h1>
      <div className="columns">
        <div className="column is-6">
          <div className="field">
            <label className="label">Lagerort</label>
            <LocationSelect locations={locations.data ?? []} value={locationId} onChange={setLocationId} required />
          </div>
          <div className="field">
            <label className="label">Richtung</label>
            <div className="buttons has-addons">
              <button
                className={`button is-medium ${isIn ? 'is-success is-selected' : ''}`}
                onClick={() => setDirection(ScanRequestDirectionEnum.In)}
              >
                ⬇ Einlagern
              </button>
              <button
                className={`button is-medium ${!isIn ? 'is-link is-selected' : ''}`}
                onClick={() => setDirection(ScanRequestDirectionEnum.Out)}
              >
                ⬆ Auslagern
              </button>
            </div>
          </div>

          {cameraError && <div className="notification is-danger is-light">{cameraError}</div>}
          {cameraOn ? (
            <>
              <video ref={videoRef} className="scan-video" muted playsInline />
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
              if (manualEan.trim()) {
                void book(manualEan.trim());
                setManualEan('');
              }
            }}
          >
            <div className="control is-expanded">
              <input
                className="input"
                inputMode="numeric"
                placeholder="EAN manuell eingeben"
                value={manualEan}
                onChange={(e) => setManualEan(e.target.value)}
              />
            </div>
            <div className="control">
              <button className={`button${busy ? ' is-loading' : ''}`} type="submit" disabled={!locationId || !manualEan.trim()}>
                Buchen
              </button>
            </div>
          </form>
        </div>

        <div className="column is-6">
          <h2 className="title is-5">Letzte Scans</h2>
          {history.length === 0 && <p className="has-text-grey">Noch nichts gescannt.</p>}
          {history.map((h, i) => (
            <div key={i} className={`notification is-light ${OUTCOME_STYLE[h.outcome]} py-3`}>
              <strong>{h.articleName ?? h.ean}</strong>
              {h.quantityAtLocation !== undefined && <span className="tag ml-2">Bestand am Ort: {h.quantityAtLocation}</span>}
              <br />
              <small>
                {h.message} · {h.at.toLocaleTimeString('de-DE')}
              </small>
              {h.outcome === ScanResultOutcomeEnum.InOkUnknownArticle && (
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
