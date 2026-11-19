import { lazy, Suspense, useState } from 'react';
import { NavLink, Navigate, Route, Routes } from 'react-router';
import { config } from './config';
import { UserMenu } from './components/AuthGate';
import { DashboardPage } from './pages/DashboardPage';
import { StockPage } from './pages/StockPage';
import { ArticlesPage } from './pages/ArticlesPage';
import { ShoppingListPage } from './pages/ShoppingListPage';
import { LocationsPage } from './pages/LocationsPage';
import { articleCategories, CategoriesPage, recipeCategories } from './pages/CategoriesPage';
import { CookPage } from './pages/CookPage';
import { RecipesPage } from './pages/RecipesPage';
import { HistoryPage } from './pages/HistoryPage';
import { ScannersPage } from './pages/ScannersPage';
import { Loading } from './components/Status';

// the barcode library is large: load it only when the scan page is opened
const ScanPage = lazy(() => import('./pages/ScanPage').then((m) => ({ default: m.ScanPage })));

const NAV = [
  { to: '/', label: 'Übersicht', end: true },
  { to: '/bestand', label: 'Bestand' },
  { to: '/artikel', label: 'Artikel' },
  { to: '/scannen', label: 'Scannen' },
  { to: '/kochen', label: 'Kochen' },
  { to: '/rezepte', label: 'Rezepte' },
  { to: '/einkaufsliste', label: 'Einkaufsliste' },
  { to: '/historie', label: 'Historie' },
];

const SETTINGS = [
  { to: '/lagerorte', label: 'Lagerorte' },
  { to: '/kategorien', label: 'Artikelkategorien' },
  { to: '/rezeptkategorien', label: 'Rezeptkategorien' },
  { to: '/scanner', label: 'Scanner' },
];

export function App() {
  const [menuOpen, setMenuOpen] = useState(false);
  const close = () => setMenuOpen(false);
  const navClass = ({ isActive }: { isActive: boolean }) => `navbar-item${isActive ? ' is-active' : ''}`;

  return (
    <>
      <nav className="navbar is-primary" aria-label="Hauptnavigation">
        <div className="container">
          <div className="navbar-brand">
            <NavLink to="/" className="navbar-item has-text-weight-bold" onClick={close}>
              📦 Vorrat
            </NavLink>
            <a
              role="button"
              className={`navbar-burger${menuOpen ? ' is-active' : ''}`}
              aria-label="Menü"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen(!menuOpen)}
            >
              <span aria-hidden="true" />
              <span aria-hidden="true" />
              <span aria-hidden="true" />
              <span aria-hidden="true" />
            </a>
          </div>
          <div className={`navbar-menu${menuOpen ? ' is-active' : ''}`}>
            <div className="navbar-start">
              {NAV.map((n) => (
                <NavLink key={n.to} to={n.to} end={n.end} className={navClass} onClick={close}>
                  {n.label}
                </NavLink>
              ))}
              <div className="navbar-item has-dropdown is-hoverable">
                <span className="navbar-link">Einstellungen</span>
                <div className="navbar-dropdown">
                  {SETTINGS.map((n) => (
                    <NavLink key={n.to} to={n.to} className={navClass} onClick={close}>
                      {n.label}
                    </NavLink>
                  ))}
                </div>
              </div>
            </div>
            <div className="navbar-end">{config.oidc && <UserMenu />}</div>
          </div>
        </div>
      </nav>
      <main className="section pt-5">
        <div className="container">
          <Routes>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/bestand" element={<StockPage />} />
            <Route path="/artikel" element={<ArticlesPage />} />
            <Route
              path="/scannen"
              element={
                <Suspense fallback={<Loading />}>
                  <ScanPage />
                </Suspense>
              }
            />
            <Route path="/einkaufsliste" element={<ShoppingListPage />} />
            <Route path="/historie" element={<HistoryPage />} />
            <Route path="/lagerorte" element={<LocationsPage />} />
            <Route path="/kategorien" element={<CategoriesPage title="Artikelkategorien" store={articleCategories} />} />
            <Route path="/rezeptkategorien" element={<CategoriesPage title="Rezeptkategorien" store={recipeCategories} />} />
            <Route path="/kochen" element={<CookPage />} />
            <Route path="/rezepte" element={<RecipesPage />} />
            <Route path="/scanner" element={<ScannersPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </div>
      </main>
    </>
  );
}
