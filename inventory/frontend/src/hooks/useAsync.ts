import { useCallback, useEffect, useState, type DependencyList } from 'react';
import { errorMessage } from '../api/errors';

export interface AsyncState<T> {
  data: T | undefined;
  error: string | undefined;
  loading: boolean;
  reload: () => void;
}

/** Runs an async loader on mount / when deps change and exposes its state. */
export function useAsync<T>(loader: () => Promise<T>, deps: DependencyList): AsyncState<T> {
  const [data, setData] = useState<T>();
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(true);
  const [version, setVersion] = useState(0);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const load = useCallback(loader, deps);

  useEffect(() => {
    let active = true;
    setLoading(true);
    load().then(
      (result) => {
        if (active) {
          setData(result);
          setError(undefined);
          setLoading(false);
        }
      },
      async (e) => {
        const message = await errorMessage(e);
        if (active) {
          setError(message);
          setLoading(false);
        }
      },
    );
    return () => {
      active = false;
    };
  }, [load, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);
  return { data, error, loading, reload };
}
