import React, { useMemo, useState } from 'react';
import {
  BarChart3,
  CreditCard,
  DollarSign,
  Lock,
  Printer,
  Smartphone,
  X
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import type { ClosureDoc } from '../types/models';

export const ClosuresPage: React.FC = () => {
  const { orders, closures, config, paperWidth, generateClosure, reprintClosure } = useApp();
  const [filterType, setFilterType] = useState<'TODOS' | 'DIARIO' | 'SEMANAL' | 'MENSUAL'>('TODOS');
  const [pendingClosureType, setPendingClosureType] = useState<'DIARIO' | 'SEMANAL' | 'MENSUAL' | null>(null);
  const [closureNotes, setClosureNotes] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);

  const todayStats = useMemo(() => {
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    const startMs = start.getTime();
    const todaysOrders = orders.filter((o) => o.timestamp >= startMs && o.status !== 'CANCELADO');

    return {
      count: todaysOrders.length,
      paidCount: todaysOrders.filter((o) => o.isPaid).length,
      unpaidCount: todaysOrders.filter((o) => !o.isPaid).length,
      totalUsd: todaysOrders.reduce((s, o) => s + (o.totalUsd || 0), 0),
      totalBs: todaysOrders.reduce((s, o) => s + (o.totalBs || 0), 0),
      usdCash: todaysOrders.reduce((s, o) => s + (o.amountUsdPaid || 0), 0),
      pagoMovilBs: todaysOrders.reduce((s, o) => s + (o.amountPagoMovilBs || 0), 0),
      puntoVentaBs: todaysOrders.reduce((s, o) => s + (o.amountPuntoVentaBs || 0), 0)
    };
  }, [orders]);

  const filteredClosures = useMemo(() => {
    if (filterType === 'TODOS') return closures;
    return closures.filter((c) => c.closureType?.toUpperCase() === filterType);
  }, [closures, filterType]);

  const handleConfirmGenerate = async () => {
    if (!pendingClosureType || isGenerating) return;
    setIsGenerating(true);
    try {
      await generateClosure(pendingClosureType, closureNotes.trim());
      setPendingClosureType(null);
      setClosureNotes('');
    } finally {
      setIsGenerating(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      {/* Header */}
      <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <BarChart3 size={24} color="#e53935" />
          <div>
            <h2 style={{ margin: 0, fontSize: 19, fontWeight: 900 }}>Cierres de Caja & Finanzas</h2>
            <div style={{ fontSize: 12, color: '#9ca3af' }}>
              Cuadre en vivo por método de pago e historial permanente sincronizado
            </div>
          </div>
        </div>
        <span className="badge-pill" style={{ background: '#2c2c30', color: '#fff', padding: '6px 12px', fontSize: 12 }}>
          {todayStats.count} pedidos hoy ({todayStats.paidCount} pagados • {todayStats.unpaidCount} pendientes)
        </span>
      </div>

      {/* Live Today Card */}
      <div className="card card-crimson">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8, marginBottom: 12 }}>
          <span style={{ fontWeight: 800, fontSize: 16 }}>Turno de Hoy (En Vivo)</span>
          <span style={{ fontWeight: 800, fontSize: 13, color: '#ffb300' }}>
            Tasa: {config.exchangeRateUsdToBs.toFixed(2)} Bs/$
          </span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 12, marginBottom: 14 }}>
          <div style={{ background: '#222226', padding: 14, borderRadius: 12 }}>
            <div style={{ fontSize: 12, color: '#9ca3af' }}>Total Dólares Hoy</div>
            <div style={{ fontSize: 26, fontWeight: 900, color: '#fff' }}>${todayStats.totalUsd.toFixed(2)}</div>
          </div>
          <div style={{ background: '#222226', padding: 14, borderRadius: 12 }}>
            <div style={{ fontSize: 12, color: '#9ca3af' }}>Total Bolívares Hoy</div>
            <div style={{ fontSize: 24, fontWeight: 900, color: '#ffb300' }}>{todayStats.totalBs.toFixed(2)} Bs</div>
          </div>
        </div>

        <div style={{ borderTop: '1px solid #33333c', paddingTop: 12, display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 10 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#1e2420', padding: '10px 12px', borderRadius: 10 }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: '#a5d6a7' }}>
              <DollarSign size={16} /> Efectivo USD ($)
            </span>
            <strong style={{ fontSize: 15 }}>${todayStats.usdCash.toFixed(2)}</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#1b2430', padding: '10px 12px', borderRadius: 10 }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: '#90caf9' }}>
              <Smartphone size={16} /> Pago Móvil (Bs)
            </span>
            <strong style={{ fontSize: 15 }}>{todayStats.pagoMovilBs.toFixed(2)} Bs</strong>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', background: '#2a2219', padding: '10px 12px', borderRadius: 10 }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, color: '#ffcc80' }}>
              <CreditCard size={16} /> Punto de Venta (Bs)
            </span>
            <strong style={{ fontSize: 15 }}>{todayStats.puntoVentaBs.toFixed(2)} Bs</strong>
          </div>
        </div>
      </div>

      {/* Generate Closures */}
      <div className="card">
        <div style={{ marginBottom: 12 }}>
          <div style={{ fontWeight: 800, fontSize: 15 }}>Generar Cierre e Imprimir Ticket ({paperWidth}mm)</div>
          <div style={{ fontSize: 12, color: '#9ca3af' }}>
            Calcula automáticamente el cuadre del período, lo guarda en Firebase y abre la impresión térmica
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <button
            className="btn btn-primary"
            style={{ flex: '1 1 180px' }}
            onClick={() => {
              setClosureNotes('');
              setPendingClosureType('DIARIO');
            }}
          >
            <Lock size={16} /> Cierre Diario
          </button>
          <button
            className="btn btn-outline"
            style={{ flex: '1 1 140px' }}
            onClick={() => {
              setClosureNotes('');
              setPendingClosureType('SEMANAL');
            }}
          >
            Cierre Semanal
          </button>
          <button
            className="btn btn-outline"
            style={{ flex: '1 1 140px' }}
            onClick={() => {
              setClosureNotes('');
              setPendingClosureType('MENSUAL');
            }}
          >
            Cierre Mensual
          </button>
        </div>
      </div>

      {/* History */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10, marginBottom: 12 }}>
          <h3 style={{ margin: 0, fontSize: 16, fontWeight: 800 }}>Historial de Cierres Emitidos</h3>
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
            {(['TODOS', 'DIARIO', 'SEMANAL', 'MENSUAL'] as const).map((t) => (
              <button key={t} className={`chip ${filterType === t ? 'active' : ''}`} onClick={() => setFilterType(t)}>
                {t}
              </button>
            ))}
          </div>
        </div>

        {filteredClosures.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '28px 10px', color: '#9ca3af', fontSize: 13 }}>
            No hay registros de cierres guardados para este filtro aún.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {filteredClosures.map((c) => (
              <ClosureHistoryRow key={c.id} closure={c} onReprint={() => reprintClosure(c)} />
            ))}
          </div>
        )}
      </div>

      {/* Confirmation Modal */}
      {pendingClosureType && (
        <div className="modal-backdrop" onClick={() => !isGenerating && setPendingClosureType(null)}>
          <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <h3 style={{ margin: 0, fontSize: 18, fontWeight: 900 }}>¿Generar Cierre {pendingClosureType}?</h3>
              <button className="btn btn-surface btn-sm" onClick={() => setPendingClosureType(null)} disabled={isGenerating}>
                <X size={16} />
              </button>
            </div>

            <p style={{ fontSize: 13, color: '#d1d5db', lineHeight: 1.5, marginTop: 0 }}>
              Esta acción totalizará las ventas del período (efectivo USD, Pago Móvil y Punto de Venta), guardará el
              cierre permanente en la nube y emitirá el comprobante térmico de {paperWidth}mm.
            </p>

            <div style={{ background: '#222226', borderRadius: 10, padding: 12, marginBottom: 12 }}>
              <div style={{ fontSize: 12, color: '#9ca3af' }}>Ventas registradas hoy hasta ahora:</div>
              <div style={{ fontSize: 16, fontWeight: 900, color: '#ffb300' }}>
                ${todayStats.totalUsd.toFixed(2)} USD ({todayStats.totalBs.toFixed(2)} Bs)
              </div>
            </div>

            <label className="label">Notas u observaciones del cierre (opcional)</label>
            <textarea
              className="textarea"
              rows={2}
              value={closureNotes}
              onChange={(e) => setClosureNotes(e.target.value)}
              placeholder="Ej: Cuadre turno noche sin novedades"
            />

            <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 16 }}>
              <button className="btn btn-outline" onClick={() => setPendingClosureType(null)} disabled={isGenerating}>
                Cancelar
              </button>
              <button className="btn btn-primary" onClick={handleConfirmGenerate} disabled={isGenerating}>
                <Printer size={16} /> {isGenerating ? 'Generando...' : `Generar e Imprimir ${paperWidth}mm`}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const ClosureHistoryRow: React.FC<{ closure: ClosureDoc; onReprint: () => void }> = ({ closure, onReprint }) => {
  const dateText = new Date(closure.timestamp).toLocaleString('es-VE', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  });

  return (
    <div
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
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 4 }}>
          <span className="badge-pill" style={{ background: '#d32f2f', color: '#fff' }}>
            {closure.closureType}
          </span>
          <strong style={{ fontSize: 15 }}>
            Cierre #{String(closure.closureNumber).padStart(4, '0')}
          </strong>
        </div>
        <div style={{ fontSize: 12, color: '#9ca3af' }}>
          {dateText} • Pedidos: {closure.orderCount} • Resp: {closure.closedBy}
        </div>
        {closure.notes && (
          <div style={{ fontSize: 12, color: '#d1d5db', marginTop: 2 }}>Nota: {closure.notes}</div>
        )}
        <div style={{ display: 'flex', gap: 10, alignItems: 'baseline', marginTop: 6, flexWrap: 'wrap' }}>
          <strong style={{ fontSize: 16, color: '#fff' }}>${(closure.totalSalesUsd || 0).toFixed(2)}</strong>
          <span style={{ fontSize: 13, fontWeight: 800, color: '#ffb300' }}>
            ({(closure.totalSalesBs || 0).toFixed(2)} Bs)
          </span>
          <span style={{ fontSize: 11.5, color: '#9ca3af' }}>
            Efec: ${(closure.totalUsdCash || 0).toFixed(2)} • PM: {(closure.totalPagoMovilBs || 0).toFixed(0)} Bs •
            PV: {(closure.totalPuntoVentaBs || 0).toFixed(0)} Bs
          </span>
        </div>
      </div>

      <button className="btn btn-surface" onClick={onReprint} title="Reimprimir ticket de cierre">
        <Printer size={16} /> Reimprimir
      </button>
    </div>
  );
};
