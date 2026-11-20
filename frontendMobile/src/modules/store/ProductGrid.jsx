import React, { useMemo } from 'react';
import ProductCard from './ProductCard';
import { calcularDescuento } from '../../utils/descuentos';
import { idDe } from './productoUtils';

const ProductGrid = ({ productos, favoritos, onToggleFavorito, onAbrir, escalas, vacio = 'No hay productos para mostrar.' }) => {
  const primerEscalon = useMemo(() => calcularDescuento(escalas, 0).escalas[0] || null, [escalas]);

  if (!productos || productos.length === 0) {
    return <p className="vacio-texto">{vacio}</p>;
  }
  return (
    <div className="pgrid">
      {productos.map((p) => (
        <ProductCard
          key={idDe(p)}
          producto={p}
          esFavorito={favoritos.includes(idDe(p))}
          onToggleFavorito={onToggleFavorito}
          onAbrir={onAbrir}
          primerEscalon={primerEscalon}
        />
      ))}
    </div>
  );
};

export default ProductGrid;
