import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { errorMessage } from '../api/errors';

type Kind = 'success' | 'danger' | 'warning' | 'info';

interface Notice {
  id: number;
  kind: Kind;
  text: string;
}

interface Notifier {
  notify: (kind: Kind, text: string) => void;
  /** Shows the message of a failed API call. */
  fail: (error: unknown) => Promise<void>;
}

const NotificationContext = createContext<Notifier | null>(null);

let nextId = 1;

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [notices, setNotices] = useState<Notice[]>([]);

  const remove = useCallback((id: number) => setNotices((n) => n.filter((x) => x.id !== id)), []);

  const notify = useCallback(
    (kind: Kind, text: string) => {
      const id = nextId++;
      setNotices((n) => [...n, { id, kind, text }]);
      window.setTimeout(() => remove(id), kind === 'danger' ? 8000 : 4000);
    },
    [remove],
  );

  const fail = useCallback(async (error: unknown) => notify('danger', await errorMessage(error)), [notify]);

  return (
    <NotificationContext.Provider value={{ notify, fail }}>
      {children}
      <div className="notification-stack" role="status">
        {notices.map((n) => (
          <div key={n.id} className={`notification is-${n.kind} is-light`}>
            <button className="delete" aria-label="Meldung schließen" onClick={() => remove(n.id)} />
            {n.text}
          </div>
        ))}
      </div>
    </NotificationContext.Provider>
  );
}

export function useNotify(): Notifier {
  const notifier = useContext(NotificationContext);
  if (!notifier) {
    throw new Error('useNotify must be used inside a NotificationProvider');
  }
  return notifier;
}
