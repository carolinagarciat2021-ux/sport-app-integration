import React from 'react';

const PASOS = [
  { estado: 'Pendiente', titulo: 'Pedido recibido' },
  { estado: 'Confirmado', titulo: 'Confirmado' },
  { estado: 'Enviado', titulo: 'Entregado a la empresa de reparto' },
  { estado: 'Entregado', titulo: 'Entregado en tu domicilio' },
];

const formatoFecha = (f) => (f ? new Date(f).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' }) : '');

/**
 * Línea de tiempo del pedido: Pedido recibido → Confirmado → Entregado a la empresa de reparto → Entregado.
 * Los pasos ya cumplidos se pintan en dorado y muestran su fecha.
 */
const LineaTiempoPedido = ({ estado, fecha, historial = [], transportadora, numeroGuia }) => {
  const cancelado = estado === 'Cancelado';
  const actual = PASOS.findIndex((p) => p.estado === estado);

  const fechaDe = (paso, i) => {
    const hito = historial.find((h) => h.estado === paso.estado);
    if (hito) return hito.fecha;
    // Los pedidos nuevos nacen "Confirmado": el primer paso (recibido) se muestra con la fecha de la compra
    if (i === 0 && actual >= 0) return fecha;
    return null;
  };

  return (
    <div>
      {cancelado && (
        <p className="lt-anulado">Este pedido fue anulado.</p>
      )}
      <ol className={`lt ${cancelado ? 'lt-apagada' : ''}`} aria-label="Estado del pedido">
        {PASOS.map((paso, i) => {
          const hecho = !cancelado && i <= actual;
          const esActual = !cancelado && i === actual;
          const f = hecho ? fechaDe(paso, i) : null;
          return (
            <li key={paso.estado} className={`lt-paso ${hecho ? 'hecho' : ''} ${esActual ? 'actual' : ''}`}>
              <span className="lt-punto" aria-hidden="true">{hecho ? '✓' : i + 1}</span>
              <span className="lt-titulo">{paso.titulo}</span>
              {f && <span className="lt-fecha">{formatoFecha(f)}</span>}
              {paso.estado === 'Enviado' && hecho && (transportadora || numeroGuia) && (
                <span className="lt-extra">
                  {transportadora && <>Empresa: {transportadora}<br /></>}
                  {numeroGuia && <>Guía: <b>{numeroGuia}</b></>}
                </span>
              )}
            </li>
          );
        })}
      </ol>
    </div>
  );
};

export default LineaTiempoPedido;
