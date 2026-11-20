import React, { useState, useRef } from 'react';
import { subirImagen } from '../../api';
import { reducirImagen } from '../../utils/imagenes';
import ImagenProducto from '../store/ImagenProducto';

/**
 * Botón "Subir foto desde mi computador": elige la foto, la reduce, la sube al servidor
 * (queda guardada en la base de datos) y devuelve su dirección con onSubida(url).
 */
const SubirImagen = ({ url, onSubida, etiqueta = 'Subir foto desde mi computador' }) => {
  const [estado, setEstado] = useState('reposo'); // reposo | subiendo | listo | error
  const [error, setError] = useState('');
  const entrada = useRef(null);

  const alElegir = async (e) => {
    const archivo = e.target.files && e.target.files[0];
    e.target.value = ''; // permite volver a elegir el mismo archivo
    if (!archivo) return;
    setEstado('subiendo');
    setError('');
    try {
      const reducida = await reducirImagen(archivo);
      const nuevaUrl = await subirImagen(reducida);
      onSubida(nuevaUrl);
      setEstado('listo');
    } catch (err) {
      setError(err.message || 'No se pudo subir la foto.');
      setEstado('error');
    }
  };

  return (
    <div className="subir-img">
      <input
        ref={entrada}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        onChange={alElegir}
        style={{ display: 'none' }}
      />
      <button
        type="button"
        onClick={() => entrada.current && entrada.current.click()}
        disabled={estado === 'subiendo'}
        style={{ padding: '8px 14px', border: '1px solid #1f1d1b', background: '#fff', color: '#1f1d1b', cursor: estado === 'subiendo' ? 'wait' : 'pointer' }}
      >
        {estado === 'subiendo' ? 'Subiendo foto...' : etiqueta}
      </button>
      {url && <ImagenProducto src={url} alt="Vista previa" className="subir-prev" />}
      {estado === 'listo' && <span style={{ color: '#2f6b3f', fontSize: '0.85rem' }}>✓ Foto subida y guardada</span>}
      {estado === 'error' && <span style={{ color: '#b21f1f', fontSize: '0.85rem' }}>⚠️ {error}</span>}
    </div>
  );
};

export default SubirImagen;
