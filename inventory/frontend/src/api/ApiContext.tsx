import { createContext, useContext, useMemo, useRef, type ReactNode } from 'react';
import { useAuth } from 'react-oidc-context';
import {
  ArticlesApi,
  CategoriesApi,
  Configuration,
  DashboardApi,
  LocationsApi,
  MovementsApi,
  ScannersApi,
  ShoppingListApi,
  StockApi,
} from './generated';

export interface Api {
  articles: ArticlesApi;
  categories: CategoriesApi;
  dashboard: DashboardApi;
  locations: LocationsApi;
  movements: MovementsApi;
  scanners: ScannersApi;
  shopping: ShoppingListApi;
  stock: StockApi;
}

const ApiContext = createContext<Api | null>(null);

function createApi(token: () => string | undefined): Api {
  const configuration = new Configuration({
    // relative URLs: nginx (or the Vite dev server) proxies /api to the backend
    basePath: '',
    accessToken: async () => token() ?? '',
  });
  return {
    articles: new ArticlesApi(configuration),
    categories: new CategoriesApi(configuration),
    dashboard: new DashboardApi(configuration),
    locations: new LocationsApi(configuration),
    movements: new MovementsApi(configuration),
    scanners: new ScannersApi(configuration),
    shopping: new ShoppingListApi(configuration),
    stock: new StockApi(configuration),
  };
}

/** API clients that always send the current access token (refreshed silently by oidc-client-ts). */
export function AuthenticatedApiProvider({ children }: { children: ReactNode }) {
  const auth = useAuth();
  const tokenRef = useRef<string | undefined>(undefined);
  tokenRef.current = auth.user?.access_token;
  const api = useMemo(() => createApi(() => tokenRef.current), []);
  return <ApiContext.Provider value={api}>{children}</ApiContext.Provider>;
}

export function AnonymousApiProvider({ children }: { children: ReactNode }) {
  const api = useMemo(() => createApi(() => undefined), []);
  return <ApiContext.Provider value={api}>{children}</ApiContext.Provider>;
}

export function useApi(): Api {
  const api = useContext(ApiContext);
  if (!api) {
    throw new Error('useApi must be used inside an ApiProvider');
  }
  return api;
}
