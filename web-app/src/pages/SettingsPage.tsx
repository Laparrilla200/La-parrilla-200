import React, { useMemo, useState } from 'react';
import {
  KeyRound,
  Monitor,
  Plus,
  Printer,
  Save,
  Settings as SettingsIcon,
  ShieldCheck,
  Trash2,
  UserCog,
  Users,
  X
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { isUserAdmin, type BusinessConfigDoc, type SystemUserDoc } from '../types/models';
import { printThermalHtml } from '../utils/thermalPrinter';

export const SettingsPage: React.FC = () => {
  const {
    config,
    paperWidth,
    setPaperWidth,
    systemUsers,
    loggedUser,
    simulateStaff,
    setSimulateStaff,
    saveConfig,
    saveUser,
    deleteUser,
    showToast
  } = useApp();

  const actualAdmin = isUserAdmin(loggedUser);

  const [rateInput, setRateInput] = useState(config.exchangeRateUsdToBs.toFixed(2));
  const [formConfig, setFormConfig] = useState<BusinessConfigDoc>(config);
  const [userModal, setUserModal] = useState<{ mode: 'NEW' | 'EDIT'; user: SystemUserDoc; prevUsername?: string } | null>(null);
  const [adminQuickModal, setAdminQuickModal] = useState(false);

  // Sync form when remote config updates
  React.useEffect(() => {
    setFormConfig(config);
    setRateInput(config.exchangeRateUsdToBs.toFixed(2));
  }, [config]);

  const adminUser = useMemo(
    () =>
      systemUsers.find((u) => isUserAdmin(u)) || {
        id: 'admin',
        username: 'admin',
        password: 'admin200',
        displayName: 'Administrador Principal',
        role: 'ADMIN',
        createdAt: Date.now(),
        isActive: true,
        canUsePosApp: true,
        canUseKitchenApp: true,
        canUseBillingApp: true,
        canUseInventoryApp: true,
        canUseClosuresApp: true,
        canUseMenuApp: true,
        canUseSettingsApp: true,
        canAccessPc: true,
        canUsePosPc: true,
        canUseKitchenPc: true,
        canUseBillingPc: true,
        canUseInventoryPc: true,
        canUseReportsPc: true
      },
    [systemUsers]
  );

  const handleSaveRate = async () => {
    const val = parseFloat(rateInput.replace(',', '.'));
    if (isNaN(val) || val <= 0) {
      showToast('Ingresa una tasa válida en Bs/$');
      return;
    }
    await saveConfig({ ...config, exchangeRateUsdToBs: val });
  };

  const handleSaveBusinessConfig = async (e: React.FormEvent) => {
    e.preventDefault();
    await saveConfig({
      ...formConfig,
      printerWidthMm: paperWidth === 58 ? 48 : 72
    });
  };

  const handleTestThermalPrint = () => {
    const w = paperWidth === 58 ? 48 : 72;
    const html = `<!DOCTYPE html><html><head><meta charset="utf-8" /><style>
      @page { size: ${paperWidth}mm auto; margin: 0mm !important; }
      * { margin: 0; padding: 0; box-sizing: border-box; color: #000 !important; }
      body { font-family: 'Courier New', monospace; font-size: ${paperWidth === 58 ? '12px' : '14px'}; background: #fff; }
      .ticket { width: ${w}mm; max-width: ${w}mm; margin: 0; padding: 0; }
      .center { text-align: center; } .bold { font-weight: 900; }
      .dashed { border-top: 1px dashed #000; margin: 5px 0; }
    </style></head><body><div class="ticket">
      <div class="center bold">${formConfig.businessName}</div>
      <div class="center">RIF: ${formConfig.rif} | Telf: ${formConfig.phone}</div>
      <div class="dashed"></div>
      <div class="center bold">PRUEBA TÉRMICA ${paperWidth}MM (WEB PC)</div>
      <div class="dashed"></div>
      <div>• Impresión sin marcos ni bordes: OK</div>
      <div>• Ancho útil ${w}mm SAT 15T2U: OK</div>
      <div>• Sincronización Firebase en la nube: OK</div>
      <div class="dashed"></div>
      <div class="center">${formConfig.ticketFooter}</div>
    </div></body></html>`;
    printThermalHtml(html);
  };

  const openNewUserModal = () => {
    setUserModal({
      mode: 'NEW',
      user: {
        id: '',
        username: '',
        password: '',
        displayName: '',
        role: 'STAFF',
        createdAt: Date.now(),
        isActive: true,
        canUsePosApp: true,
        canUseKitchenApp: true,
        canUseBillingApp: true,
        canUseInventoryApp: false,
        canUseClosuresApp: false,
        canUseMenuApp: false,
        canUseSettingsApp: false,
        canAccessPc: true,
        canUsePosPc: true,
        canUseKitchenPc: true,
        canUseBillingPc: true,
        canUseInventoryPc: false,
        canUseReportsPc: false
      }
    });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      {/* Header */}
      <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <SettingsIcon size={24} color="#e53935" />
        <div>
          <h2 style={{ margin: 0, fontSize: 19, fontWeight: 900 }}>Panel de Configuración y Permisos</h2>
          <div style={{ fontSize: 12, color: '#9ca3af' }}>
            Tasa de cambio, impresión térmica, control de personal (App + PC) y enlace Web Netlify
          </div>
        </div>
      </div>

      {/* Exchange Rate */}
      <div className="card">
        <h3 style={{ margin: '0 0 10px', fontSize: 15, fontWeight: 800, color: '#ffb300' }}>
          Tasa de Cambio Oficial (Bs / $)
        </h3>
        <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
          <div style={{ flex: '1 1 220px' }}>
            <input
              type="number"
              step="0.01"
              className="input"
              value={rateInput}
              onChange={(e) => setRateInput(e.target.value)}
            />
          </div>
          <button className="btn btn-primary" onClick={handleSaveRate}>
            <Save size={16} /> Guardar Tasa
          </button>
        </div>
      </div>

      {/* Business & Thermal Printer Config */}
      <form className="card" onSubmit={handleSaveBusinessConfig}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10, marginBottom: 12 }}>
          <div>
            <h3 style={{ margin: 0, fontSize: 15, fontWeight: 800 }}>
              Negocio e Impresión Térmica (SAT 15T2U / 58mm & 80mm)
            </h3>
            <div style={{ fontSize: 12, color: '#9ca3af' }}>
              Personaliza los datos de factura y selecciona el ancho de rollo predeterminado en esta PC
            </div>
          </div>

          <div style={{ display: 'flex', gap: 6 }}>
            <button
              type="button"
              className={`chip ${paperWidth === 58 ? 'active' : ''}`}
              onClick={() => setPaperWidth(58)}
            >
              Rollo 58mm
            </button>
            <button
              type="button"
              className={`chip ${paperWidth === 80 ? 'active' : ''}`}
              onClick={() => setPaperWidth(80)}
            >
              Rollo 80mm
            </button>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 10 }}>
          <div>
            <label className="label">Nombre del Negocio</label>
            <input
              className="input"
              value={formConfig.businessName}
              onChange={(e) => setFormConfig({ ...formConfig, businessName: e.target.value })}
            />
          </div>
          <div>
            <label className="label">RIF</label>
            <input
              className="input"
              value={formConfig.rif}
              onChange={(e) => setFormConfig({ ...formConfig, rif: e.target.value })}
            />
          </div>
          <div>
            <label className="label">Teléfono</label>
            <input
              className="input"
              value={formConfig.phone}
              onChange={(e) => setFormConfig({ ...formConfig, phone: e.target.value })}
            />
          </div>
          <div>
            <label className="label">Dirección</label>
            <input
              className="input"
              value={formConfig.address}
              onChange={(e) => setFormConfig({ ...formConfig, address: e.target.value })}
            />
          </div>
        </div>

        <div style={{ marginTop: 10 }}>
          <label className="label">Mensaje de Encabezado del Ticket</label>
          <input
            className="input"
            value={formConfig.ticketHeader}
            onChange={(e) => setFormConfig({ ...formConfig, ticketHeader: e.target.value })}
          />
        </div>

        <div style={{ marginTop: 10 }}>
          <label className="label">Mensaje de Despedida / Pie de Ticket</label>
          <textarea
            className="textarea"
            rows={2}
            value={formConfig.ticketFooter}
            onChange={(e) => setFormConfig({ ...formConfig, ticketFooter: e.target.value })}
          />
        </div>

        <div style={{ display: 'flex', gap: 10, marginTop: 14, flexWrap: 'wrap' }}>
          <button type="submit" className="btn btn-primary">
            <Save size={16} /> Guardar Cambios
          </button>
          <button type="button" className="btn btn-outline" onClick={handleTestThermalPrint}>
            <Printer size={16} /> Probar Impresión {paperWidth}mm
          </button>
        </div>
      </form>

      {/* Cloud / Web URL Card */}
      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Monitor size={20} color="#38bdf8" />
            <h3 style={{ margin: 0, fontSize: 15, fontWeight: 800 }}>Modo Web / Netlify en la Nube</h3>
          </div>
          <span className="badge-pill" style={{ background: '#1b5e20', color: '#fff' }}>
            SINCRONIZADO CON ANDROID
          </span>
        </div>
        <p style={{ fontSize: 12.5, color: '#9ca3af', margin: '8px 0 0' }}>
          Esta aplicación web se conecta directamente en tiempo real a la misma base de datos Firebase que la app
          Android. Puedes abrirla desde cualquier PC, tablet o teléfono en{' '}
          <strong style={{ color: '#38bdf8' }}>{window.location.origin}</strong>.
        </p>
      </div>

      {/* Users & Permissions Management */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10, marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Users size={20} color="#e53935" />
            <div>
              <h3 style={{ margin: 0, fontSize: 16, fontWeight: 800 }}>Gestión de Personal, Claves y Permisos</h3>
              <div style={{ fontSize: 12, color: '#9ca3af' }}>
                Define exactamente qué módulos puede usar cada perfil en la App Móvil y en la PC / Web
              </div>
            </div>
          </div>

          {actualAdmin && (
            <button className="btn btn-primary" onClick={openNewUserModal}>
              <Plus size={16} /> Nuevo Personal
            </button>
          )}
        </div>

        {/* Admin Quick Card + View Switcher */}
        {actualAdmin && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 10, marginBottom: 14 }}>
            <div style={{ background: '#261d1d', border: '1px solid #d32f2f', borderRadius: 12, padding: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 10 }}>
              <div>
                <div style={{ fontWeight: 800, fontSize: 13 }}>👑 Cuenta Administrador</div>
                <div style={{ fontSize: 12, color: '#ffb300', fontWeight: 700 }}>
                  Usuario: @{adminUser.username} | Clave: {adminUser.password}
                </div>
              </div>
              <button className="btn btn-primary btn-sm" onClick={() => setAdminQuickModal(true)}>
                <KeyRound size={14} /> Cambiar Admin
              </button>
            </div>

            <div style={{ background: '#222226', border: '1px solid #383842', borderRadius: 12, padding: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 10 }}>
              <div>
                <div style={{ fontWeight: 800, fontSize: 13 }}>Simular Vista de Personal</div>
                <div style={{ fontSize: 12, color: simulateStaff ? '#43a047' : '#ffb300', fontWeight: 700 }}>
                  {simulateStaff ? '🍳 Viendo como Personal (POS / Cocina)' : '👑 Viendo como Administrador'}
                </div>
              </div>
              <button
                className={`btn btn-sm ${simulateStaff ? 'btn-primary' : 'btn-green'}`}
                onClick={() => setSimulateStaff(!simulateStaff)}
              >
                <ShieldCheck size={14} /> {simulateStaff ? 'Volver a Admin' : 'Ver como Personal'}
              </button>
            </div>
          </div>
        )}

        {/* User List */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {systemUsers.map((u) => {
            const uAdmin = isUserAdmin(u);
            const appPerms = [
              u.canUsePosApp && 'POS',
              u.canUseKitchenApp && 'Cocina',
              u.canUseBillingApp && 'Cobros',
              u.canUseInventoryApp && 'Stock',
              u.canUseClosuresApp && 'Cierres',
              u.canUseMenuApp && 'Menú',
              u.canUseSettingsApp && 'Ajustes'
            ].filter(Boolean);

            const pcPerms = !u.canAccessPc
              ? []
              : [
                  u.canUsePosPc && 'POS',
                  u.canUseKitchenPc && 'Cocina',
                  u.canUseBillingPc && 'Cobros',
                  u.canUseInventoryPc && 'Stock',
                  u.canUseReportsPc && 'Cierres'
                ].filter(Boolean);

            return (
              <div
                key={u.username}
                style={{
                  background: '#222226',
                  border: '1px solid #383842',
                  borderRadius: 12,
                  padding: 12,
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  gap: 12,
                  flexWrap: 'wrap'
                }}
              >
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                    <strong style={{ fontSize: 14.5 }}>{u.displayName || u.username}</strong>
                    <span style={{ fontSize: 12, color: '#ffb300', fontWeight: 700 }}>@{u.username}</span>
                    <span
                      className="badge-pill"
                      style={{
                        background: uAdmin ? 'rgba(211,47,47,0.22)' : 'rgba(67,160,71,0.22)',
                        color: uAdmin ? '#fca5a5' : '#86efac'
                      }}
                    >
                      {uAdmin ? '👑 ADMIN' : '🍳 PERSONAL'}
                    </span>
                  </div>
                  {!uAdmin && (
                    <div style={{ fontSize: 11.5, color: '#9ca3af', marginTop: 4 }}>
                      <div>📱 App: {appPerms.length ? appPerms.join(', ') : 'Sin módulos'}</div>
                      <div style={{ color: u.canAccessPc ? '#86efac' : '#fca5a5' }}>
                        💻 PC/Web:{' '}
                        {!u.canAccessPc
                          ? '🚫 Acceso bloqueado en PC'
                          : pcPerms.length
                          ? pcPerms.join(', ')
                          : 'Solo lectura'}
                      </div>
                    </div>
                  )}
                </div>

                {actualAdmin && (
                  <div style={{ display: 'flex', gap: 6 }}>
                    <button
                      className="btn btn-surface btn-sm"
                      onClick={() => setUserModal({ mode: 'EDIT', user: { ...u }, prevUsername: u.username })}
                    >
                      <UserCog size={14} /> Editar Permisos
                    </button>
                    {u.username !== loggedUser?.username && (
                      <button
                        className="btn btn-danger-outline btn-sm"
                        onClick={() => {
                          if (window.confirm(`¿Eliminar al usuario @${u.username}?`)) {
                            deleteUser(u.username);
                          }
                        }}
                      >
                        <Trash2 size={14} />
                      </button>
                    )}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Add/Edit User Modal */}
      {userModal && (
        <UserEditorModal
          mode={userModal.mode}
          initialUser={userModal.user}
          onClose={() => setUserModal(null)}
          onSave={async (user) => {
            await saveUser(user, userModal.prevUsername || null);
            setUserModal(null);
          }}
        />
      )}

      {/* Quick Admin Password Modal */}
      {adminQuickModal && (
        <AdminCredsModal
          adminUser={adminUser}
          onClose={() => setAdminQuickModal(false)}
          onSave={async (updated) => {
            await saveUser(updated, adminUser.username);
            setAdminQuickModal(false);
          }}
        />
      )}
    </div>
  );
};

const UserEditorModal: React.FC<{
  mode: 'NEW' | 'EDIT';
  initialUser: SystemUserDoc;
  onClose: () => void;
  onSave: (u: SystemUserDoc) => Promise<void>;
}> = ({ mode, initialUser, onClose, onSave }) => {
  const [user, setUser] = useState<SystemUserDoc>(initialUser);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const isAdmin = user.role.toUpperCase() === 'ADMIN';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const cleanUser = user.username.trim().toLowerCase().replace(/[^a-z0-9_]/g, '');
    if (!cleanUser || !user.password.trim()) {
      setError('Usuario y clave son obligatorios.');
      return;
    }
    setSaving(true);
    try {
      await onSave({
        ...user,
        id: cleanUser,
        username: cleanUser,
        displayName: user.displayName.trim() || cleanUser,
        canUsePosApp: isAdmin ? true : user.canUsePosApp,
        canUseKitchenApp: isAdmin ? true : user.canUseKitchenApp,
        canUseBillingApp: isAdmin ? true : user.canUseBillingApp,
        canUseInventoryApp: isAdmin ? true : user.canUseInventoryApp,
        canUseClosuresApp: isAdmin ? true : user.canUseClosuresApp,
        canUseMenuApp: isAdmin ? true : user.canUseMenuApp,
        canUseSettingsApp: isAdmin ? true : user.canUseSettingsApp,
        canAccessPc: isAdmin ? true : user.canAccessPc,
        canUsePosPc: isAdmin ? true : user.canUsePosPc,
        canUseKitchenPc: isAdmin ? true : user.canUseKitchenPc,
        canUseBillingPc: isAdmin ? true : user.canUseBillingPc,
        canUseInventoryPc: isAdmin ? true : user.canUseInventoryPc,
        canUseReportsPc: isAdmin ? true : user.canUseReportsPc
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <form className="modal-panel modal-panel-xl" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
          <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900 }}>
            {mode === 'NEW' ? 'Agregar Nuevo Personal' : `Permisos de @${initialUser.username}`}
          </h3>
          <button type="button" className="btn btn-surface btn-sm" onClick={onClose}>
            <X size={16} />
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 10 }}>
          <div>
            <label className="label">Nombre del Colaborador</label>
            <input
              className="input"
              value={user.displayName}
              onChange={(e) => setUser({ ...user, displayName: e.target.value })}
              placeholder="Ej: María Gómez"
            />
          </div>
          <div>
            <label className="label">Usuario de Acceso</label>
            <input
              className="input"
              value={user.username}
              onChange={(e) => setUser({ ...user, username: e.target.value.toLowerCase().replace(/[^a-z0-9_]/g, '') })}
              placeholder="Ej: maria"
            />
          </div>
          <div>
            <label className="label">Clave / Contraseña</label>
            <input
              className="input"
              value={user.password}
              onChange={(e) => setUser({ ...user, password: e.target.value })}
              placeholder="Ej: 1234"
            />
          </div>
          <div>
            <label className="label">Rol Principal</label>
            <select
              className="select"
              value={user.role}
              onChange={(e) => setUser({ ...user, role: e.target.value })}
            >
              <option value="STAFF">🍳 Personal (Permisos Granulares)</option>
              <option value="ADMIN">👑 Administrador (Control Total)</option>
            </select>
          </div>
        </div>

        {!isAdmin && (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 12, marginTop: 16 }}>
            <div style={{ background: '#222226', border: '1px solid rgba(56,189,248,0.4)', borderRadius: 12, padding: 12 }}>
              <div style={{ fontWeight: 800, fontSize: 13, color: '#38bdf8', marginBottom: 8 }}>
                📱 Permisos en la App Android
              </div>
              <PermCheck label="Tomar Pedidos (POS)" checked={user.canUsePosApp} onChange={(v) => setUser({ ...user, canUsePosApp: v })} />
              <PermCheck label="Monitor de Cocina (KDS)" checked={user.canUseKitchenApp} onChange={(v) => setUser({ ...user, canUseKitchenApp: v })} />
              <PermCheck label="Cobrar Cuentas y Facturar" checked={user.canUseBillingApp} onChange={(v) => setUser({ ...user, canUseBillingApp: v })} />
              <PermCheck label="Inventario e Insumos" checked={user.canUseInventoryApp} onChange={(v) => setUser({ ...user, canUseInventoryApp: v })} />
              <PermCheck label="Cierres de Caja" checked={user.canUseClosuresApp} onChange={(v) => setUser({ ...user, canUseClosuresApp: v })} />
              <PermCheck label="Editar Menú y Platos" checked={user.canUseMenuApp} onChange={(v) => setUser({ ...user, canUseMenuApp: v })} />
              <PermCheck label="Ajustes del Sistema" checked={user.canUseSettingsApp} onChange={(v) => setUser({ ...user, canUseSettingsApp: v })} />
            </div>

            <div style={{ background: '#222226', border: '1px solid rgba(134,239,172,0.4)', borderRadius: 12, padding: 12 }}>
              <div style={{ fontWeight: 800, fontSize: 13, color: '#86efac', marginBottom: 8 }}>
                💻 Permisos en la PC / Web
              </div>
              <PermCheck label="Permitir Acceso a PC / Web" checked={user.canAccessPc} onChange={(v) => setUser({ ...user, canAccessPc: v })} />
              {user.canAccessPc && (
                <div style={{ paddingLeft: 12, borderLeft: '2px solid #383842', marginTop: 6 }}>
                  <PermCheck label="Tomar Pedidos en PC" checked={user.canUsePosPc} onChange={(v) => setUser({ ...user, canUsePosPc: v })} />
                  <PermCheck label="Monitor de Cocina en PC" checked={user.canUseKitchenPc} onChange={(v) => setUser({ ...user, canUseKitchenPc: v })} />
                  <PermCheck label="Cobros y Cuentas en PC" checked={user.canUseBillingPc} onChange={(v) => setUser({ ...user, canUseBillingPc: v })} />
                  <PermCheck label="Inventario en PC" checked={user.canUseInventoryPc} onChange={(v) => setUser({ ...user, canUseInventoryPc: v })} />
                  <PermCheck label="Cierres y Finanzas en PC" checked={user.canUseReportsPc} onChange={(v) => setUser({ ...user, canUseReportsPc: v })} />
                </div>
              )}
            </div>
          </div>
        )}

        {error && <div style={{ color: '#fca5a5', fontSize: 12.5, marginTop: 10 }}>{error}</div>}

        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 16 }}>
          <button type="button" className="btn btn-outline" onClick={onClose}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            <Save size={16} /> {saving ? 'Guardando...' : 'Guardar Usuario'}
          </button>
        </div>
      </form>
    </div>
  );
};

const PermCheck: React.FC<{ label: string; checked: boolean; onChange: (v: boolean) => void }> = ({
  label,
  checked,
  onChange
}) => (
  <label style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '5px 0', cursor: 'pointer', fontSize: 13 }}>
    <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} />
    <span>{label}</span>
  </label>
);

const AdminCredsModal: React.FC<{
  adminUser: SystemUserDoc;
  onClose: () => void;
  onSave: (u: SystemUserDoc) => Promise<void>;
}> = ({ adminUser, onClose, onSave }) => {
  const [username, setUsername] = useState(adminUser.username);
  const [password, setPassword] = useState(adminUser.password);
  const [saving, setSaving] = useState(false);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) return;
    setSaving(true);
    try {
      await onSave({
        ...adminUser,
        id: username.trim().toLowerCase(),
        username: username.trim().toLowerCase(),
        password: password.trim(),
        role: 'ADMIN'
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <form className="modal-panel" onClick={(e) => e.stopPropagation()} onSubmit={handleSave}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <h3 style={{ margin: 0, fontSize: 17, fontWeight: 900 }}>Cambiar Credenciales Administrador</h3>
          <button type="button" className="btn btn-surface btn-sm" onClick={onClose}>
            <X size={16} />
          </button>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <div>
            <label className="label">Usuario Admin</label>
            <input className="input" value={username} onChange={(e) => setUsername(e.target.value.toLowerCase())} />
          </div>
          <div>
            <label className="label">Nueva Clave Admin</label>
            <input className="input" value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
        </div>
        <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 16 }}>
          <button type="button" className="btn btn-outline" onClick={onClose}>
            Cancelar
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Guardando...' : 'Actualizar Admin'}
          </button>
        </div>
      </form>
    </div>
  );
};
