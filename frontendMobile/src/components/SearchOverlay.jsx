import React, { useState, useEffect, useRef, useMemo } from 'react';
import { IconSearch, IconClose } from './Icons';
import ImagenProducto from '../modules/store/ImagenProducto';
import { formatoCOP } from '../utils/descuentos';
import { idDe, nombreDe, precioDe, coloresDe, imagenDe, imagenGeneral, coincideBusqueda } from '../modules/store/productoUtils';

/** Buscador a pantalla completa: escribe una palabra clave y salen sugerencias; Enter muestra todos los resultados. */
const SearchOverlay = ({ abierto, onCerrar, productos, onAbrirProducto, onBuscar }) => {
  const [texto, setTexto] = useState('');
  const inputRef = useRef(null);

  useEffect(() => {
    if (!abierto) return undefined;
    setTexto('');
    const t = setTimeout(() => { if (inputRef.current) inputRef.current.focus(); }, 50);
    const alTeclear = (e) => { if (e.key === 'Escape') onCerrar(); };
    document.addEventListener('keydown', alTeclear);
    return () => { clearTimeout(t); document.removeEventListener('keydown', alTeclear); };
  }, [abierto, onCerrar]);

  const sugerencias = useMemo(
    () => (texto.trim().length >= 2 ? productos.filter((p) => coincideBusqueda(p, texto)).slice(0, 6) : []),
    [productos, texto]
  );

  if (!abierto) return null;

  const enviar = (e) => {
    e.preventDefault();
    if (texto.trim()) { onBuscar(texto.trim()); onCerrar(); }
  };

  return (
    <div className="bu-fondo" onMouseDown={(e) => { if (e.target === e.currentTarget) onCerrar(); }}>
      <div className="bu" role="dialog" aria-modal="true" aria-label="Buscar productos">
        <form className="bu-form" onSubmit={enviar}>
          <IconSearch />
          <input
            ref={inputRef}
            type="search"
            placeholder="Busca por nombre, color, categoría…"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            aria-label="Buscar"
          />
          <button type="button" className="bu-cerrar" onClick={onCerrar} aria-label="Cerrar buscador"><IconClose /></button>
        </form>

        {texto.trim().length >= 2 && (
          sugerencias.length > 0 ? (
            <ul className="bu-lista">
              {sugerencias.map((p) => (
                <li key={idDe(p)}>
                  <button type="button" className="bu-item" onClick={() => { onAbrirProducto(idDe(p)); onCerrar(); }}>
                    <ImagenProducto src={imagenDe(p, coloresDe(p)[0])} respaldo={imagenGeneral(p)} alt="" className="bu-img" />
                    <span className="bu-nombre">{nombreDe(p)}</span>
                    <span className="bu-precio">{formatoCOP(precioDe(p))}</span>
                  </button>
                </li>
              ))}
              <li>
                <button type="button" className="bu-todos" onClick={enviar}>Ver todos los resultados</button>
              </li>
            </ul>
          ) : (
            <p className="bu-vacio">No encontramos productos con “{texto.trim()}”. Prueba con otra palabra.</p>
          )
        )}
      </div>
    </div>
  );
};

export default SearchOverlay;
