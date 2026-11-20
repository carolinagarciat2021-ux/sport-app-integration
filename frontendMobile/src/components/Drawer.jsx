import React, { useEffect } from 'react';
import { IconClose } from './Icons';

/** Panel lateral (derecha) con fondo oscuro. Se cierra con Escape, con la X o tocando fuera. */
const Drawer = ({ abierto, titulo, onCerrar, children, pie }) => {
  useEffect(() => {
    if (!abierto) return undefined;
    const alTeclear = (e) => { if (e.key === 'Escape') onCerrar(); };
    document.addEventListener('keydown', alTeclear);
    const overflowAnterior = document.body.style.overflow;
    document.body.style.overflow = 'hidden'; // evita que la página de atrás se desplace
    return () => {
      document.removeEventListener('keydown', alTeclear);
      document.body.style.overflow = overflowAnterior;
    };
  }, [abierto, onCerrar]);

  if (!abierto) return null;

  return (
    <div className="dw-fondo" onMouseDown={(e) => { if (e.target === e.currentTarget) onCerrar(); }}>
      <aside className="dw" role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="dw-cab">
          <h2>{titulo}</h2>
          <button type="button" className="dw-x" onClick={onCerrar} aria-label="Cerrar"><IconClose /></button>
        </div>
        <div className="dw-cuerpo">{children}</div>
        {pie && <div className="dw-pie">{pie}</div>}
      </aside>
    </div>
  );
};

export default Drawer;
