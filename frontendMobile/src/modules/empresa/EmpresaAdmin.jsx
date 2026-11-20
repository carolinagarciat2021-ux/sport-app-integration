import React, { useState, useEffect } from 'react';
import { obtenerEmpresa, guardarEmpresa } from '../../api';
import { empresaDesdeApi, empresaHaciaApi } from '../../config/EmpresaContext';

const CARBON = '#1f1d1b';

/**
 * Administrador: edita los datos de la empresa que ve toda la tienda (franja superior, pie de página
 * y botón de WhatsApp). Los cambios se guardan en la base de datos y se ven al instante.
 */
const EmpresaAdmin = ({ onGuardado }) => {
  const [datos, setDatos] = useState(null);
  const [guardando, setGuardando] = useState(false);
  const [mensaje, setMensaje] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        setDatos(empresaDesdeApi(await obtenerEmpresa()));
      } catch (err) {
        setMensaje({ tipo: 'error', texto: err.message || 'No se pudieron cargar los datos.' });
      }
    })();
  }, []);

  const cambiar = (campo) => (e) => setDatos({ ...datos, [campo]: e.target.value });

  const guardar = async (e) => {
    e.preventDefault();
    setMensaje(null);
    setGuardando(true);
    try {
      const guardado = await guardarEmpresa(empresaHaciaApi(datos));
      setDatos(empresaDesdeApi(guardado)); // muestra los datos tal como quedaron (ej.: WhatsApp normalizado)
      setMensaje({ tipo: 'ok', texto: 'Datos guardados. Ya se ven en la tienda.' });
      if (onGuardado) onGuardado();
    } catch (err) {
      setMensaje({ tipo: 'error', texto: err.message || 'No se pudieron guardar los datos.' });
    } finally {
      setGuardando(false);
    }
  };

  if (!datos) {
    return <p style={{ marginTop: '20px' }}>{mensaje ? `⚠️ ${mensaje.texto}` : 'Cargando...'}</p>;
  }

  const entrada = { padding: '10px', border: '1px solid #d9d2c3', width: '100%', boxSizing: 'border-box', fontSize: '1rem' };
  const grupo = { backgroundColor: '#fff', border: '1px solid #e6dfd2', padding: '18px', marginBottom: '18px' };
  const etiqueta = { display: 'block', fontSize: '0.85rem', color: '#555', marginBottom: '12px' };
  const ayuda = { display: 'block', fontSize: '0.75rem', color: '#8a8478', marginTop: '3px' };

  return (
    <form onSubmit={guardar} style={{ marginTop: '20px', maxWidth: '680px' }}>
      <h2 style={{ color: CARBON }}>Datos de la empresa</h2>
      <p style={{ color: '#666' }}>
        Estos datos aparecen en la franja superior, en el pie de página y en el botón de WhatsApp de la tienda.
      </p>

      <div style={grupo}>
        <h3 style={{ marginTop: 0 }}>La tienda</h3>
        <label style={etiqueta}>Nombre de la tienda
          <input value={datos.nombre} onChange={cambiar('nombre')} maxLength={80} required style={entrada} />
        </label>
        <label style={etiqueta}>Lema
          <input value={datos.lema} onChange={cambiar('lema')} maxLength={150} required style={entrada} />
        </label>
      </div>

      <div style={grupo}>
        <h3 style={{ marginTop: 0 }}>Contacto</h3>
        <label style={etiqueta}>WhatsApp
          <input value={datos.whatsapp} onChange={cambiar('whatsapp')} placeholder="573001234567" required style={entrada} />
          <span style={ayuda}>Con código de país y sin espacios. Colombia = 57. También puedes escribir +57 300 123 4567.</span>
        </label>
        <label style={etiqueta}>Mensaje que aparece al abrir el chat de WhatsApp
          <input value={datos.mensajeWhatsapp} onChange={cambiar('mensajeWhatsapp')} maxLength={200} style={entrada} />
        </label>
        <label style={etiqueta}>Correo
          <input type="email" value={datos.correo} onChange={cambiar('correo')} maxLength={150} required style={entrada} />
        </label>
        <label style={etiqueta}>Correo donde recibes el aviso de cada pedido nuevo
          <input type="email" value={datos.correoAvisos || ''} onChange={cambiar('correoAvisos')} maxLength={150} placeholder="tucorreo@gmail.com" style={entrada} />
          <span style={ayuda}>Cada vez que un cliente haga un pedido, te llega un correo con sus datos. Déjalo vacío para no recibirlos.</span>
        </label>
        <a
          href={`https://wa.me/${String(datos.whatsapp).replace(/\D/g, '')}?text=${encodeURIComponent(datos.mensajeWhatsapp || '')}`}
          target="_blank"
          rel="noopener noreferrer"
          style={{ fontSize: '0.85rem', color: CARBON }}
        >
          Probar el enlace de WhatsApp
        </a>
      </div>

      <div style={grupo}>
        <h3 style={{ marginTop: 0 }}>Ubicación y horarios</h3>
        <label style={etiqueta}>Dirección
          <input value={datos.direccion} onChange={cambiar('direccion')} maxLength={200} style={entrada} />
        </label>
        <label style={etiqueta}>Horario de atención al cliente
          <input value={datos.horario} onChange={cambiar('horario')} maxLength={200} style={entrada} />
        </label>
        <label style={etiqueta}>Texto de la tienda virtual
          <input value={datos.tiendaVirtual} onChange={cambiar('tiendaVirtual')} maxLength={200} style={entrada} />
        </label>
      </div>

      <div style={grupo}>
        <h3 style={{ marginTop: 0 }}>Redes sociales (opcional)</h3>
        <label style={etiqueta}>Instagram
          <input value={datos.instagram} onChange={cambiar('instagram')} placeholder="https://instagram.com/tutienda" style={entrada} />
        </label>
        <label style={etiqueta}>Facebook
          <input value={datos.facebook} onChange={cambiar('facebook')} placeholder="https://facebook.com/tutienda" style={entrada} />
          <span style={ayuda}>Debe empezar por https://. Déjalo vacío si no tienes.</span>
        </label>
      </div>

      <button
        type="submit"
        disabled={guardando}
        style={{ padding: '12px 26px', border: 'none', backgroundColor: guardando ? '#b5b0a6' : CARBON, color: '#fff', cursor: guardando ? 'wait' : 'pointer', letterSpacing: '0.06em', textTransform: 'uppercase', fontSize: '0.85rem' }}
      >
        {guardando ? 'Guardando...' : 'Guardar datos'}
      </button>

      {mensaje && (
        <p style={{ marginTop: '12px', color: mensaje.tipo === 'ok' ? '#2f6b3f' : '#b21f1f' }}>
          {mensaje.tipo === 'ok' ? '✓ ' : '⚠️ '}{mensaje.texto}
        </p>
      )}
    </form>
  );
};

export default EmpresaAdmin;
