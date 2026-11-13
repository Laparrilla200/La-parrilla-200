import type { BusinessConfigDoc, CartOrderItem, ClosureDoc, OrderDoc, PaperWidth } from '../types/models';

function escapeHtml(text: string): string {
  return (text || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function formatDateTime(ts: number): string {
  const d = new Date(ts || Date.now());
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function getTicketCss(paperWidth: PaperWidth): string {
  const ticketWidthMm = paperWidth === 58 ? 48 : 72;
  const fontSize = paperWidth === 58 ? '12px' : '14px';
  return `
    @page {
      size: ${paperWidth}mm auto;
      margin: 0mm !important;
    }
    * {
      margin: 0;
      padding: 0;
      box-sizing: border-box;
      color: #000000 !important;
      text-shadow: none !important;
    }
    html, body {
      margin: 0 !important;
      padding: 0 !important;
      background: #ffffff !important;
      color: #000000 !important;
      font-family: 'Courier New', Courier, monospace;
      font-size: ${fontSize};
      line-height: 1.25;
      -webkit-print-color-adjust: exact !important;
      print-color-adjust: exact !important;
    }
    .ticket {
      width: ${ticketWidthMm}mm;
      max-width: ${ticketWidthMm}mm;
      margin: 0;
      padding: 0;
      background: #ffffff;
      color: #000000;
    }
    .center { text-align: center; }
    .bold { font-weight: 900; }
    .title { font-size: 1.25em; font-weight: 900; text-transform: uppercase; }
    .subtitle { font-size: 1.15em; font-weight: 900; }
    .row {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 4px;
      margin: 2px 0;
    }
    .dashed {
      border-top: 1px dashed #000000;
      margin: 5px 0;
    }
    .dashed-thick {
      border-top: 2px dashed #000000;
      margin: 6px 0;
    }
    table {
      width: 100%;
      border-collapse: collapse;
      margin: 4px 0;
    }
    th {
      border-top: 1px dashed #000000;
      border-bottom: 1px dashed #000000;
      padding: 3px 0;
      text-align: left;
      font-weight: 900;
    }
    td {
      padding: 3px 0;
      vertical-align: top;
    }
    .col-qty { width: 18%; font-weight: 900; text-align: left; }
    .col-prod { width: 55%; font-weight: 900; word-break: break-word; }
    .col-notes { width: 27%; text-align: right; font-weight: 900; white-space: nowrap; }
    .obs-box {
      border-top: 1px dashed #000000;
      border-bottom: 1px dashed #000000;
      padding: 4px 0;
      margin: 5px 0;
      font-weight: 900;
      text-transform: uppercase;
    }
    .mod-line {
      font-weight: 900;
      font-size: 0.95em;
      padding-left: 6px;
    }
    @media print {
      .no-print { display: none !important; }
      html, body {
        width: ${ticketWidthMm}mm !important;
        margin: 0 !important;
        padding: 0 !important;
      }
      .ticket {
        width: ${ticketWidthMm}mm !important;
        margin: 0 !important;
        padding: 0 !important;
      }
    }
  `;
}

export function generateKitchenTicketHtml(
  order: OrderDoc,
  items: CartOrderItem[],
  paperWidth: PaperWidth = 58
): string {
  const rowsHtml = items
    .map((item) => {
      const mods = item.excludedIngredientNames?.length
        ? item.excludedIngredientNames.map((ex) => `<div class="mod-line">SIN ${escapeHtml(ex.toUpperCase())}</div>`).join('')
        : '';
      const note = item.specialInstructions
        ? `<div class="mod-line">NOTA: ${escapeHtml(item.specialInstructions.toUpperCase())}</div>`
        : '';
      return `
        <tr>
          <td class="col-qty">${item.quantity}x</td>
          <td class="col-prod">
            ${escapeHtml(item.dishName.toUpperCase())}
            ${mods}
            ${note}
          </td>
          <td class="col-notes">${mods || note ? 'MOD' : 'OK'}</td>
        </tr>
      `;
    })
    .join('');

  const specialAlerts = items
    .filter((i) => (i.excludedIngredientNames?.length || 0) > 0 || Boolean(i.specialInstructions))
    .map((i) => {
      const parts: string[] = [];
      if (i.excludedIngredientNames?.length) {
        parts.push(...i.excludedIngredientNames.map((e) => `SIN ${e.toUpperCase()}`));
      }
      if (i.specialInstructions) parts.push(i.specialInstructions.toUpperCase());
      return `<div>• ${escapeHtml(i.dishName.toUpperCase())}: ${escapeHtml(parts.join(' | '))}</div>`;
    })
    .join('');

  const orderNum = String(order.orderNumber).padStart(4, '0');
  return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <title>Comanda #${orderNum}</title>
  <style>${getTicketCss(paperWidth)}</style>
</head>
<body>
  <div class="ticket">
    <div class="center title">*** COMANDA COCINA ***</div>
    <div class="dashed-thick"></div>
    <div class="row subtitle"><span>ORDEN: #${orderNum}</span><span>${escapeHtml(order.tableOrDestination)}</span></div>
    <div class="row bold"><span>HORA: ${formatDateTime(order.timestamp)}</span></div>
    <div class="row bold"><span>CLIENTE: ${escapeHtml(order.customerName || 'Cliente')}</span></div>
    <div class="dashed"></div>
    <table>
      <thead>
        <tr>
          <th class="col-qty">Cant</th>
          <th class="col-prod">Producto</th>
          <th class="col-notes">Notas</th>
        </tr>
      </thead>
      <tbody>${rowsHtml}</tbody>
    </table>
    ${
      specialAlerts || order.notes
        ? `<div class="obs-box">
            <div>*** OBSERVACIONES ESPECIALES ***</div>
            ${specialAlerts}
            ${order.notes ? `<div>GENERAL: ${escapeHtml(order.notes.toUpperCase())}</div>` : ''}
          </div>`
        : ''
    }
    <div class="dashed-thick"></div>
    <div class="center bold">LA PARRILLA 200 — COCINA</div>
  </div>
</body>
</html>`;
}

export function generateOrderReceiptHtml(
  order: OrderDoc,
  items: CartOrderItem[],
  config: BusinessConfigDoc,
  paperWidth: PaperWidth = 58
): string {
  const orderNum = String(order.orderNumber).padStart(4, '0');
  const methodLabel: Record<string, string> = {
    USD_EFECTIVO: 'EFECTIVO USD ($)',
    PAGO_MOVIL: 'PAGO MOVIL (Bs)',
    PUNTO_VENTA: 'PUNTO VENTA (Bs)',
    MIXTO: 'PAGO MIXTO',
    PENDIENTE: 'PENDIENTE'
  };

  const rowsHtml = items
    .map((item) => {
      const total = (item.priceUsd * item.quantity).toFixed(2);
      const mods = item.excludedIngredientNames?.length
        ? item.excludedIngredientNames.map((ex) => `<div class="mod-line">SIN ${escapeHtml(ex.toUpperCase())}</div>`).join('')
        : '';
      const note = item.specialInstructions
        ? `<div class="mod-line">* ${escapeHtml(item.specialInstructions)}</div>`
        : '';
      return `
        <tr>
          <td class="col-qty">${item.quantity}x</td>
          <td class="col-prod">${escapeHtml(item.dishName)}${mods}${note}</td>
          <td class="col-notes">$${total}</td>
        </tr>
      `;
    })
    .join('');

  return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <title>Ticket #${orderNum}</title>
  <style>${getTicketCss(paperWidth)}</style>
</head>
<body>
  <div class="ticket">
    <div class="center title">${escapeHtml(config.businessName || 'LA PARRILLA 200')}</div>
    ${config.rif ? `<div class="center bold">RIF: ${escapeHtml(config.rif)}</div>` : ''}
    ${config.phone ? `<div class="center bold">TELF: ${escapeHtml(config.phone)}</div>` : ''}
    ${config.address ? `<div class="center bold">${escapeHtml(config.address)}</div>` : ''}
    <div class="dashed-thick"></div>
    <div class="row bold"><span>TICKET: #${orderNum}</span><span>${escapeHtml(order.tableOrDestination)}</span></div>
    <div class="row bold"><span>CLIENTE: ${escapeHtml(order.customerName || 'Cliente')}</span></div>
    <div class="row bold"><span>FECHA: ${formatDateTime(order.timestamp)}</span></div>
    ${order.createdBy ? `<div class="row bold"><span>ATENDIO: ${escapeHtml(order.createdBy)}</span></div>` : ''}
    <table>
      <thead>
        <tr>
          <th class="col-qty">Cant</th>
          <th class="col-prod">Producto</th>
          <th class="col-notes">Total</th>
        </tr>
      </thead>
      <tbody>${rowsHtml}</tbody>
    </table>
    <div class="dashed"></div>
    <div class="row subtitle"><span>TOTAL USD:</span><span>$${order.totalUsd.toFixed(2)}</span></div>
    <div class="row bold"><span>TASA:</span><span>${order.exchangeRate.toFixed(2)} Bs/$</span></div>
    <div class="row subtitle"><span>TOTAL BS:</span><span>${order.totalBs.toFixed(2)} Bs</span></div>
    <div class="dashed"></div>
    <div class="center bold">DETALLE DE PAGO</div>
    <div class="row bold"><span>METODO:</span><span>${methodLabel[order.paymentMethod] || order.paymentMethod}</span></div>
    ${order.amountUsdPaid > 0 ? `<div class="row bold"><span>USD EFECTIVO:</span><span>$${order.amountUsdPaid.toFixed(2)}</span></div>` : ''}
    ${order.amountPagoMovilBs > 0 ? `<div class="row bold"><span>PAGO MOVIL:</span><span>${order.amountPagoMovilBs.toFixed(2)} Bs</span></div>` : ''}
    ${order.amountPuntoVentaBs > 0 ? `<div class="row bold"><span>PUNTO VENTA:</span><span>${order.amountPuntoVentaBs.toFixed(2)} Bs</span></div>` : ''}
    ${order.referenceCode ? `<div class="row bold"><span>REF / LOTE:</span><span>${escapeHtml(order.referenceCode)}</span></div>` : ''}
    <div class="row bold"><span>ESTADO:</span><span>${order.isPaid ? 'PAGADO - CONFORME' : 'POR COBRAR'}</span></div>
    <div class="dashed-thick"></div>
    <div class="center bold">${escapeHtml(config.ticketFooter || '¡Gracias por su compra!').replace(/\n/g, '<br/>')}</div>
    <div class="center bold">*** SAT 15T2U (${paperWidth}MM) ***</div>
  </div>
</body>
</html>`;
}

export function generateClosureReceiptHtml(
  closure: ClosureDoc,
  config: BusinessConfigDoc,
  paperWidth: PaperWidth = 58
): string {
  const num = String(closure.closureNumber).padStart(4, '0');
  const digitalBs = closure.totalPagoMovilBs + closure.totalPuntoVentaBs;
  return `<!doctype html>
<html lang="es">
<head>
  <meta charset="utf-8" />
  <title>Cierre #${num}</title>
  <style>${getTicketCss(paperWidth)}</style>
</head>
<body>
  <div class="ticket">
    <div class="center title">${escapeHtml(config.businessName || 'LA PARRILLA 200')}</div>
    <div class="center subtitle">CIERRE ${escapeHtml(closure.closureType)} #${num}</div>
    <div class="dashed-thick"></div>
    <div class="row bold"><span>EMISION:</span><span>${formatDateTime(closure.timestamp)}</span></div>
    <div class="row bold"><span>DESDE:</span><span>${formatDateTime(closure.startDate)}</span></div>
    <div class="row bold"><span>HASTA:</span><span>${formatDateTime(closure.endDate)}</span></div>
    <div class="row bold"><span>PEDIDOS:</span><span>${closure.orderCount}</span></div>
    ${closure.closedBy ? `<div class="row bold"><span>RESPONSABLE:</span><span>${escapeHtml(closure.closedBy)}</span></div>` : ''}
    <div class="dashed"></div>
    <div class="center bold">DESGLOSE DE CAJA</div>
    <div class="row bold"><span>USD EFECTIVO:</span><span>$${closure.totalUsdCash.toFixed(2)}</span></div>
    <div class="row bold"><span>PAGO MOVIL:</span><span>${closure.totalPagoMovilBs.toFixed(2)} Bs</span></div>
    <div class="row bold"><span>PUNTO VENTA:</span><span>${closure.totalPuntoVentaBs.toFixed(2)} Bs</span></div>
    <div class="dashed"></div>
    <div class="row bold"><span>TOTAL BS DIGITAL:</span><span>${digitalBs.toFixed(2)} Bs</span></div>
    <div class="row subtitle"><span>TOTAL GENERAL BS:</span><span>${closure.totalSalesBs.toFixed(2)} Bs</span></div>
    <div class="row subtitle"><span>TOTAL GENERAL USD:</span><span>$${closure.totalSalesUsd.toFixed(2)}</span></div>
    ${closure.notes ? `<div class="obs-box">OBS: ${escapeHtml(closure.notes)}</div>` : ''}
    <div class="dashed-thick"></div>
    <div class="center bold" style="margin-top: 18px;">__________________________<br/>FIRMA RESPONSABLE</div>
  </div>
</body>
</html>`;
}

export function printThermalHtml(html: string): void {
  const iframe = document.createElement('iframe');
  iframe.style.position = 'fixed';
  iframe.style.right = '0';
  iframe.style.bottom = '0';
  iframe.style.width = '0';
  iframe.style.height = '0';
  iframe.style.border = '0';
  document.body.appendChild(iframe);

  const doc = iframe.contentWindow?.document;
  if (!doc || !iframe.contentWindow) {
    document.body.removeChild(iframe);
    return;
  }
  doc.open();
  doc.write(html);
  doc.close();

  setTimeout(() => {
    try {
      iframe.contentWindow?.focus();
      iframe.contentWindow?.print();
    } finally {
      setTimeout(() => {
        if (document.body.contains(iframe)) {
          document.body.removeChild(iframe);
        }
      }, 1000);
    }
  }, 150);
}
