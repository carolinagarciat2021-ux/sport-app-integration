import React, { useState, useEffect } from 'react';
import { listarDescuentos, guardarDescuentos, obtenerEmpresa, guardarEnvioGratis } from '../../api';
import ConfiguracionAdmin from '../configuracion/ConfiguracionAdmin';
import { formatoCOP } from '../../utils/descuentos';

const CARBON = '#1f1d1b';
const ORO = '#b8975a';

/**
 * Pantalla del Administrador: define a partir de qué compra se da cada descuento mayorista
 * y el monto mínimo de compra. Todo se aplica en la web y en la app, y lo calcula el servidor.
 */
const DescuentosAdmin = ({ onGuardado }) => {
  const [filas, setFilas] = useState([]); // [{ monto: '200000', pct: '5' }]
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [mensaje, setMensaje] = useState(null); // { tipo: 'ok' | 'error', texto }
  const [envio, setEnvio] = useState('');
  const [guardandoEnvio, setGuardandoEnvio] = useState(false);
  const [mensajeEnvio, setMensajeEnvio] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        const escalas = await listarDescuentos();
        setFilas((escalas || []).map((e) => ({ monto: String(Number(e.monto_minimo)), pct: String(Number(e.porcentaje)) })));
      } catch (err) {
        setMensaje({ tipo: 'error', texto: err.message || 'No se pudieron cargar los descuentos.' });
      } finally {
        setCargando(false);
      }
    })();
  }, []);

  useEffect(() => {
    (async () => {
      try {
        const empresa = await obtenerEmpresa();
        setEnvio(String(Number(empresa.envio_gratis_desde) || 0));
      } catch (err) {
        setMensajeEnvio({ tipo: 'error', texto: err.message || 'No se pudo cargar el envío gratis.' });
      }
    })();
  }, []);

  const guardarEnvio = async () => {
    setMensajeEnvio(null);
    setGuardandoEnvio(true);
    try {
      await guardarEnvioGratis(envio);
      setMensajeEnvio({ tipo: 'ok', texto: 'Envío gratis guardado. Ya se ve en la franja superior y en el pie de página.' });
      if (onGuardado) onGuardado();
    } catch (err) {
      setMensajeEnvio({ tipo: 'error', texto: err.message || 'No se pudo guardar el envío gratis.' });
    } finally {
      setGuardandoEnvio(false);
    }
  };

  const cambiar = (i, campo, valor) => setFilas(filas.map((f, idx) => (idx === i ? { ...f, [campo]: valor } : f)));
  const quitar = (i) => setFilas(filas.filter((_, idx) => idx !== i));
  const agregar = () => setFilas([...filas, { monto: '', pct: '' }]);

  const guardar = async () => {
    setMensaje(null);
    setGuardando(true);
    try {
      await guardarDescuentos(filas.map((f) => ({ monto_minimo: Number(f.monto), porcentaje: Number(f.pct) })));
      setMensaje({ tipo: 'ok', texto: 'Descuentos guardados. Ya se ven en la tienda y se aplican en las nuevas compras.' });
      if (onGuardado) onGuardado();
    } catch (err) {
      setMensaje({ tipo: 'error', texto: err.message || 'No se pudieron guardar los descuentos.' });
    } finally {
      setGuardando(false);
    }
  };

  const entrada = { padding: '10px', border: '1px solid #d9d2c3', width: '100%', boxSizing: 'border-box', fontSize: '1rem' };

  return (
    <div style={{ marginTop: '20px', maxWidth: '640px' }}>
      <h2 style={{ color: CARBON }}>Descuentos y compra mínima</h2>

      <div style={{ backgroundColor: '#fff', border: '1px solid #e6dfd2', padding: '20px', marginBottom: '20px' }}>
        <h3 style={{ marginTop: 0, color: CARBON }}>Descuentos mayoristas</h3>
        <p style={{ color: '#666', fontSize: '0.9rem', marginTop: 0 }}>
          Cuando el valor de la compra alcanza un monto, el cliente recibe ese porcentaje de descuento sobre el total.
          A mayor monto, mayor porcentaje. Máximo 10 escalones.
        </p>

        {cargando && <p>Cargando...</p>}

        {!cargando && filas.length === 0 && <p style={{ color: '#888' }}>No hay descuentos configurados.</p>}

        {filas.map((f, i) => (
          <div key={i} style={{ display: 'grid', gridTemplateColumns: '1fr 110px auto', gap: '10px', alignItems: 'end', marginBottom: '12px' }}>
            <label style={{ fontSize: '0.8rem', color: '#555' }}>
              Compra desde ($)
              <input type="number" min="1" value={f.monto} onChange={(e) => cambiar(i, 'monto', e.target.value)} style={entrada} />
            </label>
            <label style={{ fontSize: '0.8rem', color: '#555' }}>
              Descuento (%)
              <input type="number" min="1" max="90" value={f.pct} onChange={(e) => cambiar(i, 'pct', e.target.value)} style={entrada} />
            </label>
            <button onClick={() => quitar(i)} title="Quitar escalón" style={{ padding: '10px 12px', border: '1px solid #c9b8b8', background: 'none', color: '#8c2f2f', cursor: 'pointer' }}>✕</button>
          </div>
        ))}

        {filas.some((f) => Number(f.monto) > 0 && Number(f.pct) > 0) && (
          <p style={{ fontSize: '0.85rem', color: '#5c4a24', backgroundColor: '#fbf7ef', padding: '10px 12px', border: '1px solid #e8dcc3' }}>
            Vista previa:{' '}
            {[...filas].filter((f) => Number(f.monto) > 0 && Number(f.pct) > 0)
              .sort((a, b) => Number(a.monto) - Number(b.monto))
              .map((f) => `desde ${formatoCOP(f.monto)} → ${Number(f.pct)}%`).join('  ·  ')}
          </p>
        )}

        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginTop: '8px' }}>
          <button onClick={agregar} disabled={filas.length >= 10} style={{ padding: '10px 16px', border: `1px solid ${CARBON}`, background: '#fff', color: CARBON, cursor: 'pointer' }}>
            + Agregar escalón
          </button>
          <button onClick={guardar} disabled={guardando} style={{ padding: '10px 20px', border: 'none', backgroundColor: guardando ? '#b5b0a6' : CARBON, color: '#fff', cursor: 'pointer', letterSpacing: '0.05em' }}>
            {guardando ? 'Guardando...' : 'Guardar descuentos'}
          </button>
        </div>

        {mensaje && (
          <p style={{ marginTop: '12px', color: mensaje.tipo === 'ok' ? '#2f6b3f' : '#b21f1f' }}>
            {mensaje.tipo === 'ok' ? '✓ ' : '⚠️ '}{mensaje.texto}
          </p>
        )}
      </div>

      <div style={{ backgroundColor: '#fff', border: '1px solid #e6dfd2', padding: '20px', marginBottom: '20px' }}>
        <h3 style={{ marginTop: 0, color: CARBON }}>Envío gratis</h3>
        <label style={{ fontSize: '0.8rem', color: '#555', display: 'block', maxWidth: '320px' }}>
          Envío gratis en compras superiores a ($)
          <input type="number" min="0" value={envio} onChange={(e) => setEnvio(e.target.value)} style={entrada} />
        </label>
        <p style={{ fontSize: '0.8rem', color: '#8a8478', margin: '6px 0 12px' }}>
          {Number(envio) > 0
            ? `Se anunciará: “Envío gratis en compras superiores a ${formatoCOP(envio)}”.`
            : 'Con 0 no se anuncia envío gratis.'}
        </p>
        <button type="button" onClick={guardarEnvio} disabled={guardandoEnvio} style={{ padding: '10px 20px', border: 'none', backgroundColor: guardandoEnvio ? '#b5b0a6' : CARBON, color: '#fff', cursor: 'pointer' }}>
          {guardandoEnvio ? 'Guardando...' : 'Guardar envío gratis'}
        </button>
        {mensajeEnvio && (
          <p style={{ marginTop: '12px', color: mensajeEnvio.tipo === 'ok' ? '#2f6b3f' : '#b21f1f' }}>
            {mensajeEnvio.tipo === 'ok' ? '✓ ' : '⚠️ '}{mensajeEnvio.texto}
          </p>
        )}
      </div>

      <div style={{ backgroundColor: '#fff', border: '1px solid #e6dfd2', padding: '4px 20px 20px', borderTop: `3px solid ${ORO}` }}>
        <ConfiguracionAdmin />
      </div>
    </div>
  );
};

export default DescuentosAdmin;
