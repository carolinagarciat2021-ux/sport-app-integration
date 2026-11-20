import React, { useState, useMemo, useEffect } from 'react';
import ProductGrid from './ProductGrid';
import { precioDe, nombreDe } from './productoUtils';

const POR_PAGINA = 12;

/** Lista de productos con filtro por género, orden y "mostrar más". Sirve para catálogo, categoría, tipo y búsqueda. */
const CatalogPage = ({ titulo, descripcion, migas, productos, favoritos, onToggleFavorito, onAbrir, escalas, vacio }) => {
  const [genero, setGenero] = useState('Todos');
  const [orden, setOrden] = useState('recientes');
  const [visibles, setVisibles] = useState(POR_PAGINA);

  useEffect(() => {
    setGenero('Todos');
    setOrden('recientes');
    setVisibles(POR_PAGINA);
  }, [titulo]);

  const generos = useMemo(() => {
    const unicos = Array.from(new Set((productos || []).map((p) => (p.genero || '').trim()).filter(Boolean)));
    return ['Todos', ...unicos];
  }, [productos]);

  const filtrados = useMemo(() => {
    let lista = (productos || []).filter(
      (p) => genero === 'Todos' || (p.genero || '').trim().toLowerCase() === genero.toLowerCase()
    );
    if (orden === 'precio_asc') lista = [...lista].sort((a, b) => precioDe(a) - precioDe(b));
    if (orden === 'precio_desc') lista = [...lista].sort((a, b) => precioDe(b) - precioDe(a));
    if (orden === 'nombre') lista = [...lista].sort((a, b) => nombreDe(a).localeCompare(nombreDe(b), 'es'));
    return lista;
  }, [productos, genero, orden]);

  return (
    <section className="page">
      {migas && <nav className="migas" aria-label="Ruta de navegación">{migas}</nav>}
      <h1 className="page-title">{titulo}</h1>
      {descripcion && <p className="page-desc">{descripcion}</p>}

      <div className="toolbar">
        <div className="chips" role="group" aria-label="Filtrar por género">
          {generos.length > 2 && generos.map((g) => (
            <button key={g} type="button" className={`chip ${genero === g ? 'activo' : ''}`} onClick={() => { setGenero(g); setVisibles(POR_PAGINA); }}>
              {g}
            </button>
          ))}
        </div>
        <label className="orden">
          <span>Ordenar por</span>
          <select value={orden} onChange={(e) => setOrden(e.target.value)}>
            <option value="recientes">Más recientes</option>
            <option value="precio_asc">Precio: menor a mayor</option>
            <option value="precio_desc">Precio: mayor a menor</option>
            <option value="nombre">Nombre (A–Z)</option>
          </select>
        </label>
      </div>

      <p className="conteo">{filtrados.length} {filtrados.length === 1 ? 'producto' : 'productos'}</p>

      <ProductGrid
        productos={filtrados.slice(0, visibles)}
        favoritos={favoritos}
        onToggleFavorito={onToggleFavorito}
        onAbrir={onAbrir}
        escalas={escalas}
        vacio={vacio}
      />

      {filtrados.length > visibles && (
        <div className="mas-wrap">
          <button type="button" className="btn-oscuro" onClick={() => setVisibles(visibles + POR_PAGINA)}>
            Mostrar más
          </button>
        </div>
      )}
    </section>
  );
};

export default CatalogPage;
