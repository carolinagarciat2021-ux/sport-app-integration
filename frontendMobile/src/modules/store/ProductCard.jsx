import React from 'react';
import { IconHeart } from '../../components/Icons';
import { formatoCOP } from '../../utils/descuentos';
import ImagenProducto from './ImagenProducto';
import { idDe, nombreDe, precioDe, stockDe, coloresDe, imagenDe, imagenGeneral } from './productoUtils';

/** Tarjeta minimalista: foto, nombre y precio (como en las tiendas de moda). El detalle se abre al hacer clic. */
const ProductCard = ({ producto, esFavorito, onToggleFavorito, onAbrir, primerEscalon }) => {
  const id = idDe(producto);
  const nombre = nombreDe(producto);
  const precio = precioDe(producto);
  const agotado = stockDe(producto) <= 0;

  return (
    <article className="pc">
      <div className="pc-media">
        <button type="button" className="pc-img-btn" onClick={() => onAbrir(id)} aria-label={`Ver ${nombre}`}>
          <ImagenProducto src={imagenDe(producto, coloresDe(producto)[0])} respaldo={imagenGeneral(producto)} alt={nombre} className="pc-img" />
        </button>
        {agotado && <span className="pc-tag">Agotado</span>}
        <button
          type="button"
          className={`pc-fav ${esFavorito ? 'activo' : ''}`}
          onClick={() => onToggleFavorito(id)}
          aria-label={esFavorito ? 'Quitar de la lista de deseos' : 'Agregar a la lista de deseos'}
          aria-pressed={esFavorito}
        >
          <IconHeart lleno={esFavorito} />
        </button>
      </div>
      <button type="button" className="pc-info" onClick={() => onAbrir(id)}>
        <span className="pc-name">{nombre}</span>
        <span className="pc-price">{formatoCOP(precio)}</span>
        {primerEscalon && !agotado && (
          <span className="pc-promo">
            {formatoCOP(precio * (1 - primerEscalon.pct / 100))} c/u comprando {formatoCOP(primerEscalon.monto)} o más
          </span>
        )}
      </button>
    </article>
  );
};

export default ProductCard;
