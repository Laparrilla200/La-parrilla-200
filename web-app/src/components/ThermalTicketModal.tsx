import React from 'react';
import { Printer, X } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { printThermalHtml } from '../utils/thermalPrinter';

export const ThermalTicketModal: React.FC = () => {
  const { ticketModal, closeTicketModal, paperWidth } = useApp();
  if (!ticketModal) return null;

  return (
    <div className="modal-backdrop" onClick={closeTicketModal}>
      <div
        className="modal-panel"
        style={{ maxWidth: '460px' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
          <div>
            <div style={{ fontWeight: 800, fontSize: 16 }}>{ticketModal.title}</div>
            <div style={{ fontSize: 12, color: '#9ca3af' }}>
              Formato térmico SAT 15T2U ({paperWidth === 58 ? '48mm / Rollo 58mm' : '72mm / Rollo 80mm'})
            </div>
          </div>
          <button className="btn btn-surface btn-sm" onClick={closeTicketModal} aria-label="Cerrar">
            <X size={16} />
          </button>
        </div>

        <div
          style={{
            background: '#ffffff',
            borderRadius: 8,
            padding: 12,
            overflow: 'auto',
            maxHeight: '60vh',
            display: 'flex',
            justifyContent: 'center'
          }}
        >
          <iframe
            title={ticketModal.title}
            srcDoc={ticketModal.html}
            style={{
              width: paperWidth === 58 ? '230px' : '320px',
              minHeight: '380px',
              border: 'none',
              background: '#ffffff'
            }}
          />
        </div>

        <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
          <button
            className="btn btn-primary"
            style={{ flex: 1 }}
            onClick={() => printThermalHtml(ticketModal.html)}
          >
            <Printer size={16} /> Imprimir Ticket ({paperWidth}mm)
          </button>
          <button className="btn btn-outline" onClick={closeTicketModal}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
};
