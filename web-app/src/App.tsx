import React, { useEffect, useMemo } from 'react';
import {
  AlertTriangle,
  BarChart3,
  BookOpen,
  ChefHat,
  LogOut,
  Package,
  Settings,
  ShoppingCart,
  Sparkles,
  UserCheck
} from 'lucide-react';
import { AuthScreen } from './components/AuthScreen';
import { ThermalTicketModal } from './components/ThermalTicketModal';
import { useApp } from './context/AppContext';
import { AiAssistantPage } from './pages/AiAssistantPage';
import { ClosuresPage } from './pages/ClosuresPage';
import { InventoryPage } from './pages/InventoryPage';
import { KitchenPage } from './pages/KitchenPage';
import { MenuAdminPage } from './pages/MenuAdminPage';
import { PosPage } from './pages/PosPage';
import { SettingsPage } from './pages/SettingsPage';
import { canUserAccessWebScreen, isUserAdmin, type AppScreen } from './types/models';

const NAV_ITEMS: { id: AppScreen; label: string; icon: React.ReactNode }[] = [
  { id: 'POS', label: 'POS / Caja', icon: <ShoppingCart size={16} /> },
  { id: 'KITCHEN', label: 'Cocina (KDS)', icon: <ChefHat size={16} /> },
  { id: 'INVENTORY', label: 'Inventario', icon: <Package size={16} /> },
  { id: 'MENU_ADMIN', label: 'Menú & Extras', icon: <BookOpen size={16} /> },
  { id: 'CLOSURES', label: 'Cierres', icon: <BarChart3 size={16} /> },
  { id: 'AI_ASSISTANT', label: 'Gemini AI', icon: <Sparkles size={16} /> },
  { id: 'SETTINGS', label: 'Ajustes', icon: <Settings size={16} /> }
];

export const App: React.FC = () => {
  const {
    loggedUser,
    simulateStaff,
    setSimulateStaff,
    currentScreen,
    setCurrentScreen,
    unpaidOrders,
    orders,
    criticalSupplies,
    config,
    toast,
    logout
  } = useApp();

  const allowedScreens = useMemo(
    () => NAV_ITEMS.filter((item) => canUserAccessWebScreen(loggedUser, item.id, simulateStaff)),
    [loggedUser, simulateStaff]
  );

  // Ensure active screen is always permitted for the logged user
  useEffect(() => {
    if (!loggedUser || allowedScreens.length === 0) return;
    const isAllowed = allowedScreens.some((s) => s.id === currentScreen);
    if (!isAllowed) {
      setCurrentScreen(allowedScreens[0].id);
    }
  }, [loggedUser, allowedScreens, currentScreen, setCurrentScreen]);

  if (!loggedUser) {
    return (
      <>
        <AuthScreen />
        {toast && <div className="toast-banner">{toast}</div>}
      </>
    );
  }

  const kitchenPendingCount = orders.filter((o) => o.status === 'EN_COCINA').length;
  const actualAdmin = isUserAdmin(loggedUser);

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand-group">
          <div className="brand-badge">P200</div>
          <div>
            <div style={{ fontWeight: 900, fontSize: 15, letterSpacing: 0.4 }}>
              {config.businessName || 'LA PARRILLA 200'}
            </div>
            <div style={{ fontSize: 11.5, color: '#9ca3af', display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
              <span style={{ color: '#ffb300', fontWeight: 700 }}>
                Tasa: {config.exchangeRateUsdToBs.toFixed(2)} Bs/$
              </span>
              <span>•</span>
              <span>
                {loggedUser.displayName} ({actualAdmin && !simulateStaff ? 'Admin' : 'Personal'})
              </span>
            </div>
          </div>
        </div>

        <nav className="nav-tabs" aria-label="Navegación principal">
          {allowedScreens.map((item) => {
            const active = currentScreen === item.id;
            const isAi = item.id === 'AI_ASSISTANT';
            return (
              <button
                key={item.id}
                className={`nav-tab ${active ? (isAi ? 'ai-active' : 'active') : ''}`}
                onClick={() => setCurrentScreen(item.id)}
              >
                {item.icon}
                <span>{item.label}</span>
                {item.id === 'POS' && unpaidOrders.length > 0 && (
                  <span className="badge-pill" style={{ background: '#ffb300', color: '#000' }}>
                    {unpaidOrders.length}
                  </span>
                )}
                {item.id === 'KITCHEN' && kitchenPendingCount > 0 && (
                  <span className="badge-pill" style={{ background: '#ff5722', color: '#fff' }}>
                    {kitchenPendingCount}
                  </span>
                )}
                {item.id === 'INVENTORY' && criticalSupplies.length > 0 && (
                  <span className="badge-pill" style={{ background: '#ef4444', color: '#fff' }}>
                    {criticalSupplies.length}
                  </span>
                )}
              </button>
            );
          })}
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          {actualAdmin && criticalSupplies.length > 0 && (
            <button
              className="btn btn-danger-outline btn-sm"
              onClick={() => setCurrentScreen('INVENTORY')}
              title="Ver insumos con stock crítico"
            >
              <AlertTriangle size={14} /> {criticalSupplies.length} Críticos
            </button>
          )}
          {actualAdmin && (
            <button
              className="btn btn-surface btn-sm"
              onClick={() => setSimulateStaff(!simulateStaff)}
              title="Cambiar entre vista Administrador y vista Personal"
            >
              <UserCheck size={14} /> {simulateStaff ? 'Modo Personal' : 'Admin'}
            </button>
          )}
          <button
            className="btn btn-outline btn-sm"
            onClick={logout}
            title="Cerrar sesión manualmente"
          >
            <LogOut size={14} /> Cerrar Sesión
          </button>
        </div>
      </header>

      <main className="main-container">
        {currentScreen === 'POS' && <PosPage />}
        {currentScreen === 'KITCHEN' && <KitchenPage />}
        {currentScreen === 'INVENTORY' && <InventoryPage />}
        {currentScreen === 'MENU_ADMIN' && <MenuAdminPage />}
        {currentScreen === 'CLOSURES' && <ClosuresPage />}
        {currentScreen === 'AI_ASSISTANT' && <AiAssistantPage />}
        {currentScreen === 'SETTINGS' && <SettingsPage />}
      </main>

      <ThermalTicketModal />
      {toast && <div className="toast-banner">{toast}</div>}
    </div>
  );
};

export default App;
