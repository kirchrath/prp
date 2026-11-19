import type { ReactElement } from 'react';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { vi } from 'vitest';
import { ApiContext, type Api } from '../api/ApiContext';
import { NotificationProvider } from '../components/Notifications';

type Mocked<T> = { [K in keyof T]: ReturnType<typeof vi.fn> };
export type FakeApi = { [K in keyof Api]: Mocked<Api[K]> };

/** All API methods as mocks resolving to sensible empty defaults. */
export function fakeApi(): FakeApi {
  const ok = (value?: unknown) => vi.fn().mockResolvedValue(value);
  return {
    articles: {
      listArticles: ok([]), getArticle: ok(), getArticleByEan: ok(), lookupProduct: ok(), createArticle: ok(),
      updateArticle: ok(), deleteArticle: ok(), mergeArticle: ok(),
    },
    categories: { listCategories: ok([]), getCategory: ok(), createCategory: ok(), updateCategory: ok(), deleteCategory: ok() },
    locations: { listLocations: ok([]), getLocation: ok(), createLocation: ok(), updateLocation: ok(), deleteLocation: ok() },
    stock: {
      listBatches: ok([]), getBatch: ok(), correctBatch: ok(), assignBestBefore: ok(), bookIn: ok({ quantity: 1 }),
      bookOut: ok({ quantity: 0 }), listLevels: ok([]), getSummary: ok({ totalUnits: 0, expiringSoon: [] }),
    },
    bookings: { createBookingRaw: ok(), getBooking: ok() },
    movements: { listMovements: ok({ items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 }) },
    shopping: {
      listShoppingItems: ok([]), getShoppingItem: ok(), createShoppingItem: ok(), updateShoppingItem: ok(),
      deleteShoppingItem: ok(), deleteDoneShoppingItems: ok(),
    },
    scanners: { listScanners: ok([]), getScanner: ok(), updateScanner: ok(), deleteScanner: ok() },
    recipes: {
      listRecipes: ok([]), listRecipeTags: ok([]), getRecipe: ok(), createRecipe: ok(), updateRecipe: ok(), deleteRecipe: ok(),
    },
    recipeCategories: {
      listRecipeCategories: ok([]), getRecipeCategory: ok(), createRecipeCategory: ok(), updateRecipeCategory: ok(),
      deleteRecipeCategory: ok(),
    },
    recipeImports: { importRecipeFromUrl: ok(), importRecipeFromText: ok() },
    cooking: { listSuggestions: ok([]), createShoppingRequest: ok(), getShoppingRequest: ok() },
    notificationSettings: {
      getNotificationSettings: ok({ audit: audit(0), data: { enabled: true, sendTime: '08:00:00', defaultDays: 3, categoryThresholds: [] } }),
      updateNotificationSettings: ok(), sendTestNotification: ok(),
    },
    digests: { getDue: ok({ expired: [], dueToday: [], dueSoon: [] }), listDigests: ok([]) },
  } as unknown as FakeApi;
}

export function renderWith(ui: ReactElement, api: FakeApi, route = '/') {
  return render(
    <MemoryRouter initialEntries={[route]}>
      <NotificationProvider>
        <ApiContext.Provider value={api as unknown as Api}>{ui}</ApiContext.Provider>
      </NotificationProvider>
    </MemoryRouter>,
  );
}

export const audit = (version = 0) => ({
  createdDate: new Date(2026, 0, 1),
  lastModifiedDate: new Date(2026, 0, 1),
  version,
  deleted: false,
});

/** An error as thrown by the generated clients. */
export function apiError(status: number, detail?: string) {
  const body = detail ? JSON.stringify({ detail }) : 'x';
  return Object.assign(new Error('Response returned an error code'), { response: new Response(body, { status }) });
}

export const loc = (id: string, name: string, parentId?: string) => ({ id, name, parentId, path: name });
