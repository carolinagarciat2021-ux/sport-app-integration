import React, { useState, useEffect, useCallback } from 'react';
import { listarPedidosStaff, actualizarPedido, eliminarPedido } from '../../api';
import ImagenProducto from '../store/ImagenProducto';
import { formatoCOP } from '../../utils/descuentos';

const CARBON = '#1f1d1b';
const ESTADOS = ['Pendiente', 'Confirmado', 'Enviado', 'Entregado', 'Cancelado'];
const COLOR_ESTADO = {
  Pendiente: { fondo: '#fff4e5', texto: '#b45309' },
  Confirmado: { fondo: '#e8f3ff', texto: '#1d5fa8' },
  Enviado: { fondo: '#efe9ff', texto: '#5b3fb0' },
  Entregado: { fondo: '#e8f5e9', texto: '#2f6b3f' },
  Cancelado: { fondo: '#fdecec', texto: '#b21f1f' },
};
const FILTROS_VACIOS = { q: '', desde: '', hasta: '', min: '', max: '', estado: '' };

const formatoFecha = (f) => (f ? new Date(f).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' }) : '');

/**
 * Pedidos para Administrador y Vendedor: muestra quién compró (nombre y correo), con filtros por
 * nombre/correo, fecha, valor y estado. El Administrador además puede anular o eliminar.
 */
const PedidosStaff = ({ esAdministrador }) => {
  const [formulario, setFormulario] = useState(FILTROS_VACIOS); // lo que se está escribiendo
  const [filtros, setFiltros] = useState(FILTROS_VACIOS);       // lo que ya se aplicó
  const [pagina, setPagina] = useState(0);
  const [datos, setDatos] = useState({ pedidos: [], total_paginas: 1, total_registros: 0 });
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [abierto, setAbierto] = useState(null);
  const [estadoNuevo, setEstadoNuevo] = useState({}); // { idPedido: 'Enviado' }
  const [datosEnvio, setDatosEnvio] = useState({});   // { idPedido: { transportadora, guia } }

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      setDatos(await listarPedidosStaff({ ...filtros, pagina }));
    } catch (err) {
      setError(err.message || 'No se pudieron cargar los pedidos.');
    } finally {
      setCargando(false);
    }
  }, [filtros, pagina]);

  useEffect(() => { cargar(); }, [cargar]);

  const buscar = (e) => {
    e.preventDefault();
    setPagina(0);
    setAbierto(null);
    setFiltros({ ...formulario, q: formulario.q.trim() });
  };

  const limpiar = () => {
    setFormulario(FILTROS_VACIOS);
    setFiltros(FILTROS_VACIOS);
    setPagina(0);
    setAbierto(null);
  };

  const cambiarEstado = async (pedido) => {
    const nuevo = estadoNuevo[pedido.id_pedido] || pedido.estado;
    if (nuevo === pedido.estado) return;
    try {
      const envio = datosEnvio[pedido.id_pedido] || {};
      await actualizarPedido({
        id_pedido: pedido.id_pedido,
        estado: nuevo,
        transportadora: nuevo === 'Enviado' ? (envio.transportadora || '') : undefined,
        numero_guia: nuevo === 'Enviado' ? (envio.guia || '') : undefined,
      });
      await cargar();
    } catch (err) {
      alert(`❌ No se pudo cambiar el estado: ${err.message}`);
    }
  };

  const anular = async (pedido) => {
    if (!window.confirm(`¿Anular el pedido #${pedido.id_pedido}? Se devolverá el stock y quedará como Cancelado.`)) return;
    try {
      await actualizarPedido({ id_pedido: pedido.id_pedido, estado: 'Cancelado' });
      await cargar();
    } catch (err) {
      alert(`❌ No se pudo anular: ${err.message}`);
    }
  };

  const eliminar = async (pedido) => {
    if (!window.confirm(`¿ELIMINAR definitivamente el pedido #${pedido.id_pedido}? No se puede deshacer. Si no estaba anulado, se devolverá el stock.`)) return;
    try {
      await eliminarPedido(pedido.id_pedido);
      setAbierto(null);
      await cargar();
    } catch (err) {
      alert(`❌ No se pudo eliminar: ${err.message}`);
    }
  };

  const campo = (nombre) => (e) => setFormulario({ ...formulario, [nombre]: e.target.value });
  const entrada = { padding: '9px', border: '1px solid #d9d2c3', width: '100%', boxSizing: 'border-box', fontSize: '0.95rem', background: '#fff' };
  const etiqueta = { fontSize: '0.78rem', color: '#555', display: 'block' };
  const hayFiltros = Object.values(filtros).some((v) => v !== '');

  return (
    <div style={{ marginTop: '20px' }}>
      <h2 style={{ color: CARBON }}>📦 Pedidos de clientes</h2>

      <form onSubmit={buscar} style={{ backgroundColor: '#fff', border: '1px solid #e6dfd2', padding: '16px', marginBottom: '16px' }}>
        <div style={{ display: 'grid', gap: '12px', gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))' }}>
          <label style={{ ...etiqueta, gridColumn: 'span 2' }}>Buscar por nombre, correo o identificación
            <input value={formulario.q} onChange={campo('q')} placeholder="Ej.: Lina, lina_tabares@gmail.com" style={entrada} />
          </label>
          <label style={etiqueta}>Fecha desde
            <input type="date" value={formulario.desde} onChange={campo('desde')} style={entrada} />
          </label>
          <label style={etiqueta}>Fecha hasta
            <input type="date" value={formulario.hasta} onChange={campo('hasta')} style={entrada} />
          </label>
          <label style={etiqueta}>Valor mínimo ($)
            <input type="number" min="0" value={formulario.min} onChange={campo('min')} style={entrada} />
          </label>
          <label style={etiqueta}>Valor máximo ($)
            <input type="number" min="0" value={formulario.max} onChange={campo('max')} style={entrada} />
          </label>
          <label style={etiqueta}>Estado
            <select value={formulario.estado} onChange={campo('estado')} style={entrada}>
              <option value="">Todos</option>
              {ESTADOS.map((e) => <option key={e} value={e}>{e}</option>)}
            </select>
          </label>
        </div>
        <div style={{ display: 'flex', gap: '10px', marginTop: '14px', flexWrap: 'wrap' }}>
          <button type="submit" style={{ padding: '10px 22px', border: 'none', background: CARBON, color: '#fff', cursor: 'pointer' }}>Buscar</button>
          <button type="button" onClick={limpiar} style={{ padding: '10px 22px', border: `1px solid ${CARBON}`, background: '#fff', color: CARBON, cursor: 'pointer' }}>Limpiar filtros</button>
        </div>
      </form>

      {error && <div style={{ background: '#ffe6e6', color: '#b21f1f', padding: '12px', marginBottom: '12px' }}>⚠️ {error}</div>}
      {cargando && <p>Cargando pedidos...</p>}

      {!cargando && !error && (
        <>
          <p style={{ color: '#6b665c', fontSize: '0.9rem' }}>
            {datos.total_registros} {datos.total_registros === 1 ? 'pedido' : 'pedidos'}{hayFiltros ? ' con esos filtros' : ''}
          </p>

          {datos.pedidos.length === 0 && (
            <p style={{ color: '#888', textAlign: 'center', padding: '30px 0' }}>No hay pedidos para mostrar.</p>
          )}

          {datos.pedidos.map((p) => {
            const color = COLOR_ESTADO[p.estado] || COLOR_ESTADO.Pendiente;
            const desplegado = abierto === p.id_pedido;
            const descuento = Number(p.descuento_valor) || 0;
            return (
              <div key={p.id_pedido} style={{ backgroundColor: '#fff', border: '1px solid #e6dfd2', marginBottom: '10px' }}>
                <button
                  type="button"
                  onClick={() => setAbierto(desplegado ? null : p.id_pedido)}
                  aria-expanded={desplegado}
                  style={{ width: '100%', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '12px', padding: '14px 16px', background: 'none', border: 'none', cursor: 'pointer', textAlign: 'left', flexWrap: 'wrap' }}
                >
                  <span style={{ flex: 1, minWidth: '230px' }}>
                    <strong>Pedido #{p.id_pedido}</strong>
                    <span style={{ color: '#7a7468', fontSize: '0.85rem' }}> · {formatoFecha(p.fecha)}</span>
                    <br />
                    <span style={{ fontSize: '0.95rem' }}>{p.nombre_cliente} {p.apellido_cliente}</span>
                    <span style={{ color: '#7a7468', fontSize: '0.85rem' }}> · {p.correo_cliente}</span>
                  </span>
                  <span style={{ background: color.fondo, color: color.texto, padding: '4px 12px', borderRadius: '12px', fontSize: '0.8rem', fontWeight: 600 }}>{p.estado}</span>
                  <strong style={{ minWidth: '90px', textAlign: 'right' }}>{formatoCOP(p.total)}</strong>
                  <span aria-hidden="true">{desplegado ? '▲' : '▼'}</span>
                </button>

                {desplegado && (
                  <div style={{ borderTop: '1px solid #eee', padding: '14px 16px' }}>
                    <div style={{ display: 'grid', gap: '4px', fontSize: '0.9rem', marginBottom: '12px' }}>
                      <span><b>Cliente:</b> {p.nombre_cliente} {p.apellido_cliente}</span>
                      <span><b>Correo:</b> <a href={`mailto:${p.correo_cliente}`}>{p.correo_cliente}</a></span>
                      {p.telefono_cliente && <span><b>Teléfono:</b> {p.telefono_cliente}</span>}
                      {p.direccion_entrega && <span><b>Dirección de entrega:</b> {p.direccion_entrega}</span>}
                      {p.transportadora && <span><b>Empresa de reparto:</b> {p.transportadora}</span>}
                      {p.numero_guia && <span><b>Número de guía:</b> {p.numero_guia}</span>}
                    </div>

                    <div style={{ overflowX: 'auto' }}>
                      <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: '520px', fontSize: '0.9rem' }}>
                        <thead>
                          <tr style={{ textAlign: 'left', borderBottom: '2px solid #eee' }}>
                            <th style={{ padding: '6px' }}>Producto</th>
                            <th style={{ padding: '6px' }}>Talla</th>
                            <th style={{ padding: '6px' }}>Color</th>
                            <th style={{ padding: '6px' }}>Cant.</th>
                            <th style={{ padding: '6px' }}>Precio</th>
                            <th style={{ padding: '6px' }}>Subtotal</th>
                          </tr>
                        </thead>
                        <tbody>
                          {p.productos.map((l, i) => (
                            <tr key={i} style={{ borderBottom: '1px solid #f1ece2' }}>
                              <td style={{ padding: '6px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                                <ImagenProducto src={l.imagen_url} alt="" className="lista-miniatura" />
                                {l.nombre_producto}
                              </td>
                              <td style={{ padding: '6px' }}>{l.talla}</td>
                              <td style={{ padding: '6px' }}>{l.color}</td>
                              <td style={{ padding: '6px' }}>{l.cantidad}</td>
                              <td style={{ padding: '6px' }}>{formatoCOP(l.precio_unitario)}</td>
                              <td style={{ padding: '6px' }}>{formatoCOP(l.subtotal)}</td>
                            </tr>
                          ))}
                          {p.productos.length === 0 && (
                            <tr><td colSpan={6} style={{ padding: '8px', color: '#888' }}>Este pedido no tiene productos registrados.</td></tr>
                          )}
                        </tbody>
                      </table>
                    </div>

                    {descuento > 0 && (
                      <p style={{ textAlign: 'right', margin: '10px 0 0', color: '#555' }}>
                        Subtotal {formatoCOP(p.subtotal)} · Descuento {Number(p.descuento_porcentaje)}% (−{formatoCOP(descuento)}) · <strong>Total {formatoCOP(p.total)}</strong>
                      </p>
                    )}

                    <div style={{ display: 'flex', gap: '10px', marginTop: '14px', flexWrap: 'wrap', alignItems: 'center' }}>
                      {p.estado !== 'Cancelado' && (
                        <>
                          <select
                            value={estadoNuevo[p.id_pedido] || p.estado}
                            onChange={(e) => setEstadoNuevo({ ...estadoNuevo, [p.id_pedido]: e.target.value })}
                            style={{ padding: '9px', border: '1px solid #d9d2c3' }}
                            aria-label="Nuevo estado del pedido"
                          >
                            {['Pendiente', 'Confirmado', 'Enviado', 'Entregado'].map((e) => <option key={e} value={e}>{e}</option>)}
                          </select>
                          {(estadoNuevo[p.id_pedido] || p.estado) === 'Enviado' && p.estado !== 'Enviado' && (
                            <>
                              <input
                                placeholder="Empresa de reparto (opcional)"
                                value={(datosEnvio[p.id_pedido] || {}).transportadora || ''}
                                onChange={(e) => setDatosEnvio({ ...datosEnvio, [p.id_pedido]: { ...(datosEnvio[p.id_pedido] || {}), transportadora: e.target.value } })}
                                maxLength={80}
                                style={{ padding: '9px', border: '1px solid #d9d2c3' }}
                              />
                              <input
                                placeholder="Número de guía (opcional)"
                                value={(datosEnvio[p.id_pedido] || {}).guia || ''}
                                onChange={(e) => setDatosEnvio({ ...datosEnvio, [p.id_pedido]: { ...(datosEnvio[p.id_pedido] || {}), guia: e.target.value } })}
                                maxLength={60}
                                style={{ padding: '9px', border: '1px solid #d9d2c3' }}
                              />
                            </>
                          )}
                          <button type="button" onClick={() => cambiarEstado(p)} style={{ padding: '9px 16px', border: 'none', background: CARBON, color: '#fff', cursor: 'pointer' }}>
                            Cambiar estado
                          </button>
                          <span style={{ fontSize: '0.78rem', color: '#8a8478' }}>Al cambiarlo, el cliente recibe un correo.</span>
                        </>
                      )}
                      {esAdministrador && p.estado !== 'Cancelado' && (
                        <button type="button" onClick={() => anular(p)} style={{ padding: '9px 16px', border: 'none', background: '#e65100', color: '#fff', cursor: 'pointer' }}>🚫 Anular pedido</button>
                      )}
                      {esAdministrador && (
                        <button type="button" onClick={() => eliminar(p)} style={{ padding: '9px 16px', border: 'none', background: '#b21f1f', color: '#fff', cursor: 'pointer' }}>🗑️ Eliminar pedido</button>
                      )}
                    </div>
                  </div>
                )}
              </div>
            );
          })}

          {datos.total_paginas > 1 && (
            <div style={{ display: 'flex', gap: '12px', alignItems: 'center', justifyContent: 'center', marginTop: '16px' }}>
              <button type="button" disabled={pagina === 0} onClick={() => { setPagina(pagina - 1); setAbierto(null); }} style={{ padding: '9px 14px' }}>◀ Anterior</button>
              <span>Página {pagina + 1} de {datos.total_paginas}</span>
              <button type="button" disabled={pagina + 1 >= datos.total_paginas} onClick={() => { setPagina(pagina + 1); setAbierto(null); }} style={{ padding: '9px 14px' }}>Siguiente ▶</button>
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default PedidosStaff;
