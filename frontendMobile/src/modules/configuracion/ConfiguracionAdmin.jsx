

import React, { useState, useEffect } from 'react';
import { obtenerConfiguracion, actualizarConfiguracion } from '../../api';

function ConfiguracionAdmin() {
  const [montoActual, setMontoActual] = useState(null);
  const [nuevoMonto, setNuevoMonto] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [mensaje, setMensaje] = useState('');

  useEffect(() => {
    cargar();
  }, []);

  const cargar = async () => {
    try {
      const config = await obtenerConfiguracion();
      setMontoActual(config.monto_minimo_compra);
      setNuevoMonto(config.monto_minimo_compra);
    } catch (error) {
      setMensaje('No se pudo cargar la configuración actual.');
    }
  };

  const guardar = async () => {
    setGuardando(true);
    setMensaje('');
    try {
      await actualizarConfiguracion(Number(nuevoMonto));
      setMensaje('Monto mínimo actualizado correctamente.');
      cargar();
    } catch (error) {
      setMensaje('Error al guardar: ' + error.message);
    } finally {
      setGuardando(false);
    }
  };

  return (
    <div style={{ border: '1px solid #ddd', borderRadius: '8px', padding: '16px', marginTop: '16px', maxWidth: '400px' }}>
      <h4 style={{ margin: '0 0 8px 0', color: '#1f1d1b' }}>Monto mínimo de compra mayorista</h4>
      <p style={{ fontSize: '13px', color: '#555', margin: '0 0 10px 0' }}>
        Ningún cliente podrá confirmar un pedido por debajo de este valor, ni desde la página ni desde la app Android.
      </p>
      {montoActual !== null && (
        <p style={{ fontSize: '14px', margin: '0 0 10px 0' }}>
          Monto actual: <strong>${Number(montoActual).toLocaleString('es-CO')}</strong>
        </p>
      )}
      <input
        type="number"
        value={nuevoMonto}
        onChange={(e) => setNuevoMonto(e.target.value)}
        style={{ width: '100%', padding: '8px', marginBottom: '10px' }}
      />
      <button
        onClick={guardar}
        disabled={guardando}
        style={{ width: '100%', padding: '8px', backgroundColor: '#2E7D32', color: 'white', border: 'none', borderRadius: '4px' }}
      >
        {guardando ? 'Guardando...' : 'Guardar'}
      </button>
      {mensaje && <p style={{ fontSize: '13px', marginTop: '8px' }}>{mensaje}</p>}
    </div>
  );
}

export default ConfiguracionAdmin;
