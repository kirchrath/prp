import { createContext, useContext, useMemo, useRef, type ReactNode } from 'react';
import { useAuth } from 'react-oidc-context';
import * as catalog from './catalog';
import * as device from './device';
import * as notification from './notification';
import * as recipe from './recipe';
import * as shopping from './shopping';
import * as stock from './stock';

/** Clients of all backend services; Traefik routes /api/<service> to the service. */
export interface Api {
  articles: catalog.ArticlesApi;
  categories: catalog.CategoriesApi;
  locations: stock.LocationsApi;
  stock: stock.StockApi;
  bookings: stock.BookingsApi;
  movements: stock.MovementsApi;
  shopping: shopping.ShoppingItemsApi;
  scanners: device.ScannersApi;
  firmware: device.FirmwareApi;
  firmwareUpdates: device.FirmwareUpdatesApi;
  recipes: recipe.RecipesApi;
  recipeCategories: recipe.RecipeCategoriesApi;
  recipeImports: recipe.RecipeImportsApi;
  cooking: recipe.CookingApi;
  notificationSettings: notification.NotificationSettingsApi;
  digests: notification.DigestsApi;
}

export const ApiContext = createContext<Api | null>(null);

export function createApi(token: () => string | undefined): Api {
  const params = { basePath: '', accessToken: async () => token() ?? '' };
  const catalogConfig = new catalog.Configuration(params);
  const stockConfig = new stock.Configuration(params);
  const recipeConfig = new recipe.Configuration(params);
  const deviceConfig = new device.Configuration(params);
  const notificationConfig = new notification.Configuration(params);
  return {
    articles: new catalog.ArticlesApi(catalogConfig),
    categories: new catalog.CategoriesApi(catalogConfig),
    locations: new stock.LocationsApi(stockConfig),
    stock: new stock.StockApi(stockConfig),
    bookings: new stock.BookingsApi(stockConfig),
    movements: new stock.MovementsApi(stockConfig),
    shopping: new shopping.ShoppingItemsApi(new shopping.Configuration(params)),
    scanners: new device.ScannersApi(deviceConfig),
    firmware: new device.FirmwareApi(deviceConfig),
    firmwareUpdates: new device.FirmwareUpdatesApi(deviceConfig),
    recipes: new recipe.RecipesApi(recipeConfig),
    recipeCategories: new recipe.RecipeCategoriesApi(recipeConfig),
    recipeImports: new recipe.RecipeImportsApi(recipeConfig),
    cooking: new recipe.CookingApi(recipeConfig),
    notificationSettings: new notification.NotificationSettingsApi(notificationConfig),
    digests: new notification.DigestsApi(notificationConfig),
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
