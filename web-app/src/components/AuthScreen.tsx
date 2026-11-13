import React, { useState } from 'react';
import { Eye, EyeOff, Flame, Info, Lock, LogIn, User } from 'lucide-react';
import { useApp } from '../context/AppContext';

export const AuthScreen: React.FC = () => {
  const { login, systemUsers } = useApp();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password.trim() || loading) return;
    setLoading(true);
    setError(null);
    const res = await login(username, password);
    setLoading(false);
    if (!res.ok) {
      setError(res.error || 'Error de autenticación');
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20,
        background: 'radial-gradient(circle at top, #2a1010 0%, #121212 60%)'
      }}
    >
      <div style={{ width: '100%', maxWidth: 440 }}>
        <div style={{ textAlign: 'center', marginBottom: 20 }}>
          <div
            className="brand-badge"
            style={{ width: 76, height: 76, margin: '0 auto 12px', fontSize: 28 }}
          >
            <Flame size={36} />
          </div>
          <h1 style={{ margin: 0, fontSize: 28, fontWeight: 900, letterSpacing: 1 }}>
            LA PARRILLA 200
          </h1>
          <p style={{ margin: '6px 0 0', fontSize: 13, color: '#d4d4d8' }}>
            Portal Web POS • Cocina KDS • Inventario en la Nube
          </p>
        </div>

        <form onSubmit={handleSubmit} className="card" style={{ padding: 24 }}>
          <h2 style={{ margin: '0 0 4px', fontSize: 17, fontWeight: 800 }}>Acceso de Personal</h2>
          <p style={{ margin: '0 0 18px', fontSize: 12.5, color: '#9ca3af' }}>
            Tu sesión permanecerá abierta hasta que presiones «Cerrar Sesión».
          </p>

          <div style={{ marginBottom: 14 }}>
            <label className="label">Usuario (Ej. admin, cajero, cocina)</label>
            <div style={{ position: 'relative' }}>
              <User
                size={16}
                style={{ position: 'absolute', left: 12, top: 13, color: '#e53935' }}
              />
              <input
                className="input"
                style={{ paddingLeft: 38 }}
                value={username}
                onChange={(e) => {
                  setUsername(e.target.value.toLowerCase().replace(/[^a-z0-9_.]/g, ''));
                  setError(null);
                }}
                placeholder="admin"
                autoComplete="username"
              />
            </div>
          </div>

          <div style={{ marginBottom: 14 }}>
            <label className="label">Contraseña / Clave</label>
            <div style={{ position: 'relative' }}>
              <Lock
                size={16}
                style={{ position: 'absolute', left: 12, top: 13, color: '#e53935' }}
              />
              <input
                className="input"
                style={{ paddingLeft: 38, paddingRight: 40 }}
                type={showPass ? 'text' : 'password'}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  setError(null);
                }}
                placeholder="••••••••"
                autoComplete="current-password"
              />
              <button
                type="button"
                onClick={() => setShowPass(!showPass)}
                style={{
                  position: 'absolute',
                  right: 8,
                  top: 7,
                  background: 'transparent',
                  border: 'none',
                  color: '#9ca3af',
                  cursor: 'pointer',
                  padding: 4
                }}
              >
                {showPass ? <Eye size={18} /> : <EyeOff size={18} />}
              </button>
            </div>
          </div>

          {error && (
            <div
              style={{
                background: 'rgba(183, 28, 28, 0.3)',
                border: '1px solid #ef4444',
                borderRadius: 8,
                padding: 10,
                color: '#fca5a5',
                fontSize: 12.5,
                textAlign: 'center',
                marginBottom: 14
              }}
            >
              {error}
            </div>
          )}

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', height: 46, borderRadius: 23, fontSize: 15 }}
            disabled={loading || !username.trim() || !password.trim()}
          >
            <LogIn size={18} />
            {loading ? 'Ingresando...' : 'Ingresar al Sistema'}
          </button>

          {systemUsers.length > 0 && (
            <div style={{ marginTop: 18 }}>
              <div style={{ fontSize: 11.5, fontWeight: 700, color: '#9ca3af', marginBottom: 8 }}>
                Selección Rápida de Usuario:
              </div>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                {systemUsers.slice(0, 4).map((u) => (
                  <button
                    key={u.username}
                    type="button"
                    className={`chip ${username === u.username ? 'active' : ''}`}
                    onClick={() => {
                      setUsername(u.username);
                      setError(null);
                    }}
                  >
                    {u.role?.toUpperCase() === 'ADMIN' ? '👑' : '👤'} {u.username}
                  </button>
                ))}
              </div>
            </div>
          )}
        </form>

        <div
          className="card"
          style={{
            marginTop: 14,
            background: '#222226',
            display: 'flex',
            alignItems: 'center',
            gap: 12,
            padding: 12
          }}
        >
          <Info size={22} color="#e53935" style={{ flexShrink: 0 }} />
          <div>
            <div style={{ fontSize: 12, fontWeight: 700 }}>Acceso Inicial por Defecto:</div>
            <div style={{ fontSize: 13, fontWeight: 900, color: '#ffb300' }}>
              Usuario: admin | Clave: admin200
            </div>
            <div style={{ fontSize: 11, color: '#9ca3af' }}>
              Puedes administrar usuarios, claves y permisos en Ajustes.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
