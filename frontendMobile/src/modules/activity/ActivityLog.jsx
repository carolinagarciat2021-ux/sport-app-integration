import React, { useState, useEffect, useCallback } from 'react';
import { listarActividad } from '../../api';

const ACCIONES = [
  ['', 'Todas las acciones'],
  ['LOGIN', 'Inicios de sesión'],
  ['LOGIN_FALLIDO', 'Inicios de sesión fallidos'],
  ['PEDIDO_CREADO', 'Compras'],
  ['PEDIDO_ESTADO', 'Cambios de estado'],
  ['PEDIDO_ANULADO', 'Pedidos anulados'],
  ['PEDIDO_ELIMINADO', 'Pedidos eliminados'],
  ['PRODUCTO_CREADO', 'Productos creados'],
  ['PRODUCTO_EDITADO', 'Productos editados'],
  ['PRODUCTO_ELIMINADO', 'Productos eliminados'],
  ['CLIENTE_REGISTRADO', 'Registros de cuenta'],
  ['CLIENTE_EDITADO', 'Usuarios editados'],
  ['CLIENTE_ELIMINADO', 'Usuarios eliminados'],
];

/**
 * Historial de actividad (solo Administrador): quién hizo qué y cuándo.
 * Es de solo lectura; los registros no se pueden editar ni borrar.
 */
const ActivityLog = () => {
  const [registros, setRegistros] = useState([]);
  const [pagina, setPagina] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(1);
  const [accion, setAccion] = useState('');
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  const cargar = useCallback(async () => {
    setCargando(true);
    setError('');
    try {
      const r = await listarActividad(pagina, accion);
      setRegistros(r.registros || []);
      setTotalPaginas(Math.max(r.total_paginas || 1, 1));
    } catch (err) {
      setError(err.message || 'No se pudo cargar el historial.');
    } finally {
      setCargando(false);
    }
  }, [pagina, accion]);

  useEffect(() => { cargar(); }, [cargar]);

  const formatoFecha = (f) => (f ? new Date(f).toLocaleString('es-CO') : '');

  return (
    <div style={{ marginTop: '20px' }}>
      <h2 style={{ color: '#1f1d1b' }}>🕘 Historial de actividad</h2>
      <p style={{ color: '#666' }}>Registro de lo que hacen administradores, vendedores y clientes. No se puede editar ni borrar.</p>

      <select
        value={accion}
        onChange={(e) => { setPagina(0); setAccion(e.target.value); }}
        style={{ padding: '10px', borderRadius: '6px', border: '1px solid #ccc', margin: '10px 0', maxWidth: '100%' }}
      >
        {ACCIONES.map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
      </select>

      {error && <div style={{ backgroundColor: '#ffe6e6', color: '#b21f1f', padding: '12px', borderRadius: '6px' }}>⚠️ {error}</div>}
      {cargando && <p>Cargando historial...</p>}

      {!cargando && !error && (
        <div style={{ overflowX: 'auto' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', backgroundColor: '#fff' }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #eee', textAlign: 'left' }}>
                <th style={{ padding: '8px' }}>Fecha</th>
                <th style={{ padding: '8px' }}>Usuario</th>
                <th style={{ padding: '8px' }}>Acción</th>
                <th style={{ padding: '8px' }}>Detalle</th>
              </tr>
            </thead>
            <tbody>
              {registros.map((r) => (
                <tr key={r.id_registro} style={{ borderBottom: '1px solid #f0f0f0', verticalAlign: 'top' }}>
                  <td style={{ padding: '8px', whiteSpace: 'nowrap' }}>{formatoFecha(r.fecha)}</td>
                  <td style={{ padding: '8px' }}>{r.correo_usuario || '—'}{r.rol_usuario ? ` (${r.rol_usuario})` : ''}</td>
                  <td style={{ padding: '8px', fontWeight: 'bold' }}>{r.accion}{r.id_entidad ? ` #${r.id_entidad}` : ''}</td>
                  <td style={{ padding: '8px', minWidth: '260px' }}>{r.detalle}</td>
                </tr>
              ))}
              {registros.length === 0 && (
                <tr><td colSpan={4} style={{ padding: '12px', color: '#888' }}>Todavía no hay actividad registrada.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      <div style={{ display: 'flex', gap: '12px', alignItems: 'center', marginTop: '14px' }}>
        <button disabled={pagina === 0} onClick={() => setPagina(pagina - 1)} style={{ padding: '10px 14px' }}>◀ Anterior</button>
        <span>Página {pagina + 1} de {totalPaginas}</span>
        <button disabled={pagina + 1 >= totalPaginas} onClick={() => setPagina(pagina + 1)} style={{ padding: '10px 14px' }}>Siguiente ▶</button>
      </div>
    </div>
  );
};

export default ActivityLog;
