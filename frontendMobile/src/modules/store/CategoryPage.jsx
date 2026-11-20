import React from 'react';
import CatalogPage from './CatalogPage';
import ImagenProducto from './ImagenProducto';
import { imagenGeneral } from './productoUtils';

/**
 * Página de una categoría. Si el administrador creó "tipos" (ej.: Falda short, Falda larga) se
 * muestran primero como tarjetas con imagen; al elegir uno aparecen sus productos.
 * Si la categoría no tiene tipos, muestra directamente sus productos.
 */
const CategoryPage = ({ categoria, subcategorias, productos, subcategoriaId, onTipo, onVolverACategoria, onInicio, ...resto }) => {
  if (!categoria) {
    return <p className="vacio-texto">Esa categoría ya no existe.</p>;
  }

  const productosCat = productos.filter((p) => p.idCategoria === categoria.id_categoria || p.id_categoria === categoria.id_categoria);
  const tipos = subcategorias.filter((s) => s.id_categoria === categoria.id_categoria);

  const migasBase = (
    <>
      <button type="button" className="miga-link" onClick={onInicio}>Inicio</button>
      <span> / </span>
    </>
  );

  // 1) Pantalla de tipos
  if (tipos.length > 0 && !subcategoriaId) {
    return (
      <section className="page">
        <nav className="migas">{migasBase}<span>{categoria.nombre}</span></nav>
        <h1 className="page-title">{categoria.nombre}</h1>
        <p className="page-desc">Elige el tipo que buscas.</p>

        <div className="tipos-grid">
          {tipos.map((t) => {
            const delTipo = productosCat.filter((p) => p.id_subcategoria === t.id_subcategoria);
            const foto = t.imagen_url || (delTipo[0] ? imagenGeneral(delTipo[0]) : '');
            return (
              <button key={t.id_subcategoria} type="button" className="tipo-card" onClick={() => onTipo(t.id_subcategoria)}>
                <ImagenProducto src={foto} alt={t.nombre} className="tipo-img" />
                <span className="tipo-nombre">{t.nombre}</span>
                <span className="tipo-cant">{delTipo.length} {delTipo.length === 1 ? 'producto' : 'productos'}</span>
              </button>
            );
          })}
          <button type="button" className="tipo-card tipo-todos" onClick={() => onTipo('todos')}>
            <span className="tipo-todos-texto">Ver todos los productos de {categoria.nombre}</span>
            <span className="tipo-cant">{productosCat.length} {productosCat.length === 1 ? 'producto' : 'productos'}</span>
          </button>
        </div>
      </section>
    );
  }

  // 2) Productos de un tipo, de toda la categoría o de una categoría sin tipos
  const tipoActual = tipos.find((t) => t.id_subcategoria === subcategoriaId);
  const lista = tipoActual ? productosCat.filter((p) => p.id_subcategoria === tipoActual.id_subcategoria) : productosCat;

  const migas = (
    <>
      {migasBase}
      {tipos.length > 0 ? (
        <button type="button" className="miga-link" onClick={onVolverACategoria}>{categoria.nombre}</button>
      ) : (
        <span>{categoria.nombre}</span>
      )}
      {tipoActual && <><span> / </span><span>{tipoActual.nombre}</span></>}
    </>
  );

  return (
    <CatalogPage
      titulo={tipoActual ? `${categoria.nombre} · ${tipoActual.nombre}` : categoria.nombre}
      migas={migas}
      productos={lista}
      vacio="Todavía no hay productos en esta sección."
      {...resto}
    />
  );
};

export default CategoryPage;
