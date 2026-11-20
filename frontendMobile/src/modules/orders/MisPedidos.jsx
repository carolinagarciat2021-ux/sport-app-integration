import React, { useState, useEffect } from 'react';
import { listarMisPedidos } from '../../api';
import ImagenProducto from '../store/ImagenProducto';
import LineaTiempoPedido from './LineaTiempoPedido';
import { formatoCOP } from '../../utils/descuentos';

const CARBON = '#1f1d1b';
const formatoFecha = (f) => (f ? new Date(f).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' }) : '');

/** "Mis pedidos" del cliente: cada pedido con su línea de tiempo y sus productos. No tiene botón de borrar. */
const MisPedidos = ({ onIrATienda }) => {
  const [pedidos, setPedidos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [abierto, setAbierto] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        setPedidos((await listarMisPedidos()) || []);
      } catch (err) {
        setError(err.message || 'No se pudieron cargar tus pedidos.');
      } finally {
        setCargando(false);
      }
    })();
  }, []);

  return (
    <div style={{ marginTop: '20px', maxWidth: '900px' }}>
      <h2 style={{ color: CARBON }}>Mis pedidos</h2>

      {cargando && <p>Cargando tus pedidos...</p>}
      {error && <p style={{ color: '#b21f1f' }}>⚠️ {error}</p>}

      {!cargando && !error && pedidos.length === 0 && (
        <div style={{ textAlign: 'center', padding: '40px 10px', background: '#fff', border: '1px solid #e6dfd2' }}>
          <p style={{ color: '#666' }}>Todavía no has hecho pedidos.</p>
          <button type="button" className="btn-oscuro" onClick={onIrATienda}>Ir a la tienda</button>
        </div>
      )}

      {pedidos.map((p) => {
        const descuento = Number(p.descuento_valor) || 0;
        const desplegado = abierto === p.id_pedido;
        return (
          <div key={p.id_pedido} style={{ background: '#fff', border: '1px solid #e6dfd2', marginBottom: '16px', padding: '16px 18px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: '8px', marginBottom: '14px' }}>
              <span>
                <strong>Pedido #{p.id_pedido}</strong>
                <span style={{ color: '#7a7468', fontSize: '0.85rem' }}> · {formatoFecha(p.fecha)}</span>
              </span>
              <strong>{formatoCOP(p.total)}</strong>
            </div>

            <LineaTiempoPedido
              estado={p.estado}
              fecha={p.fecha}
              historial={p.historial || []}
              transportadora={p.transportadora}
              numeroGuia={p.numero_guia}
            />

            <button
              type="button"
              onClick={() => setAbierto(desplegado ? null : p.id_pedido)}
              aria-expanded={desplegado}
              style={{ background: 'none', border: 'none', color: CARBON, textDecoration: 'underline', cursor: 'pointer', padding: '10px 0 0', fontSize: '0.88rem' }}
            >
              {desplegado ? 'Ocultar productos' : 'Ver productos'}
            </button>

            {desplegado && (
              <div style={{ marginTop: '10px' }}>
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: '420px', fontSize: '0.9rem' }}>
                    <tbody>
                      {(p.productos || []).map((l, i) => (
                        <tr key={i} style={{ borderBottom: '1px solid #f1ece2' }}>
                          <td style={{ padding: '6px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <ImagenProducto src={l.imagen_url} alt="" className="lista-miniatura" />
                            <span>{l.nombre_producto}<br /><small style={{ color: '#7a7468' }}>Talla {l.talla} · {l.color}</small></span>
                          </td>
                          <td style={{ padding: '6px' }}>x{l.cantidad}</td>
                          <td style={{ padding: '6px', textAlign: 'right' }}>{formatoCOP(l.subtotal)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {descuento > 0 && (
                  <p style={{ textAlign: 'right', margin: '8px 0 0', color: '#555' }}>
                    Subtotal {formatoCOP(p.subtotal)} · Descuento {Number(p.descuento_porcentaje)}% (−{formatoCOP(descuento)}) · <strong>Total {formatoCOP(p.total)}</strong>
                  </p>
                )}
                {p.direccion_entrega && (
                  <p style={{ margin: '8px 0 0', fontSize: '0.88rem', color: '#555' }}><b>Dirección de entrega:</b> {p.direccion_entrega}</p>
                )}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};

export default MisPedidos;
