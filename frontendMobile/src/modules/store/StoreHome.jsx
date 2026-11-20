import React, { useMemo } from 'react';
import ProductGrid from './ProductGrid';
import { calcularDescuento, formatoCOP } from '../../utils/descuentos';
import { useEmpresa } from '../../config/EmpresaContext';
import { idDe } from './productoUtils';

/** Página de inicio: presentación breve + productos (foto, nombre y precio). */
const StoreHome = ({ productos, escalas, favoritos, onToggleFavorito, onAbrir, onVerCatalogo }) => {
  const EMPRESA = useEmpresa();
  const escalones = useMemo(() => calcularDescuento(escalas, 0).escalas, [escalas]);

  // Los más recientes primero (id más alto), solo los que tienen stock arriba
  const destacados = useMemo(() => {
    const ordenados = [...productos].sort((a, b) => Number(idDe(b)) - Number(idDe(a)));
    const conStock = ordenados.filter((p) => Number(p.stock) > 0);
    const sinStock = ordenados.filter((p) => !(Number(p.stock) > 0));
    return [...conStock, ...sinStock].slice(0, 8);
  }, [productos]);

  return (
    <div>
      <section className="hero">
        <p className="hero-kicker">{EMPRESA.nombre}</p>
        <h1 className="hero-title">{EMPRESA.lema}</h1>
        {escalones.length > 0 && (
          <p className="hero-sub">
            Compra al por mayor y ahorra:{' '}
            {escalones.map((e) => `${e.pct}% desde ${formatoCOP(e.monto)}`).join(' · ')}
          </p>
        )}
        <button type="button" className="btn-dorado" onClick={onVerCatalogo}>Ver catálogo</button>
      </section>

      <section className="page">
        <h2 className="seccion-titulo">Novedades</h2>
        <ProductGrid
          productos={destacados}
          favoritos={favoritos}
          onToggleFavorito={onToggleFavorito}
          onAbrir={onAbrir}
          escalas={escalas}
          vacio="Pronto agregaremos productos a la tienda."
        />
        {productos.length > destacados.length && (
          <div className="mas-wrap">
            <button type="button" className="btn-oscuro" onClick={onVerCatalogo}>Ver todo el catálogo</button>
          </div>
        )}
      </section>
    </div>
  );
};

export default StoreHome;
