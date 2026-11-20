import React, { useState, useMemo } from 'react';
import { IconHeart } from '../../components/Icons';
import { calcularDescuento, formatoCOP } from '../../utils/descuentos';
import ImagenProducto from './ImagenProducto';
import ProductGrid from './ProductGrid';
import { idDe, nombreDe, precioDe, stockDe, coloresDe, tallasDe, imagenDe, imagenGeneral, galeriaDe } from './productoUtils';

/**
 * Detalle completo de un producto: foto grande, nombre, descripción, precio, tallas, colores,
 * cantidad disponible y botón de agregar al carrito.
 */
const ProductDetail = ({
  producto, categorias, productos, escalas, favoritos, onToggleFavorito,
  onAbrir, onAgregarAlCarrito, onVolver, textoVolver = 'Volver al catálogo',
}) => {
  const id = idDe(producto);
  const nombre = nombreDe(producto);
  const precio = precioDe(producto);
  const stock = stockDe(producto);
  const agotado = stock <= 0;
  const colores = coloresDe(producto);
  const tallas = tallasDe(producto);

  const [talla, setTalla] = useState(tallas[0]);
  const [color, setColor] = useState(colores[0]);
  const [cantidad, setCantidad] = useState(1);
  const [fotoElegida, setFotoElegida] = useState(null); // foto tocada en las miniaturas
  const [aviso, setAviso] = useState('');

  const escalones = useMemo(() => calcularDescuento(escalas, 0).escalas, [escalas]);
  const galeria = galeriaDe(producto);
  const fotoPrincipal = fotoElegida || imagenDe(producto, color);
  const esFavorito = favoritos.includes(id);

  const categoria = (categorias || []).find((c) => c.id_categoria === (producto.idCategoria ?? producto.id_categoria));

  const relacionados = useMemo(
    () => productos
      .filter((p) => idDe(p) !== id && (p.idCategoria ?? p.id_categoria) === (producto.idCategoria ?? producto.id_categoria))
      .slice(0, 4),
    [productos, producto, id]
  );

  const cambiarCantidad = (valor) => {
    const n = Math.max(1, Math.min(Number(valor) || 1, stock || 1));
    setCantidad(n);
  };

  const agregar = () => {
    if (agotado) return;
    if (cantidad > stock) {
      setAviso(`Solo hay ${stock} unidades disponibles.`);
      return;
    }
    setAviso('');
    onAgregarAlCarrito({
      idProducto: id,
      nombre,
      precioMayorista: precio,
      imagenUrl: imagenDe(producto, color),
      talla,
      color,
      cantidad,
      stockDisponible: stock,
    });
  };

  return (
    <section className="page detalle">
      <nav className="migas">
        <button type="button" className="miga-link" onClick={onVolver}>← {textoVolver}</button>
        {categoria && <span> · {categoria.nombre}</span>}
      </nav>

      <div className="detalle-grid">
        <div className="detalle-fotos">
          <div className="detalle-foto-principal">
            <ImagenProducto src={fotoPrincipal} respaldo={imagenGeneral(producto)} alt={nombre} className="detalle-img" />
          </div>
          {galeria.length > 1 && (
            <div className="miniaturas">
              {galeria.map((url) => (
                <button key={url} type="button" className={`miniatura ${fotoPrincipal === url ? 'activo' : ''}`} onClick={() => setFotoElegida(url)} aria-label="Ver foto">
                  <ImagenProducto src={url} alt="" className="miniatura-img" />
                </button>
              ))}
            </div>
          )}
        </div>

        <div className="detalle-info">
          <h1 className="detalle-nombre">{nombre}</h1>
          <p className="detalle-ref">Referencia #{id}{producto.genero ? ` · ${producto.genero}` : ''}</p>
          <p className="detalle-precio">{formatoCOP(precio)} <span className="detalle-precio-nota">precio mayorista por unidad</span></p>

          {escalones.length > 0 && (
            <div className="detalle-escala">
              <strong>Descuentos por el valor de tu compra</strong>
              <ul>
                {escalones.map((e) => (
                  <li key={e.monto}>
                    Desde {formatoCOP(e.monto)} ({e.pct}% dto.): <b>{formatoCOP(precio * (1 - e.pct / 100))} c/u</b>
                  </li>
                ))}
              </ul>
              <small>El descuento se aplica sobre el total de la compra y lo verás en el carrito antes de confirmar.</small>
            </div>
          )}

          {producto.descripcion && <p className="detalle-desc">{producto.descripcion}</p>}

          <div className="opcion">
            <span className="opcion-titulo">Talla</span>
            <div className="opcion-lista">
              {tallas.map((t) => (
                <button key={t} type="button" className={`opcion-btn ${talla === t ? 'activo' : ''}`} onClick={() => setTalla(t)}>{t}</button>
              ))}
            </div>
          </div>

          <div className="opcion">
            <span className="opcion-titulo">Color: <b>{color}</b></span>
            <div className="opcion-lista">
              {colores.map((c) => (
                <button key={c} type="button" className={`opcion-btn ${color === c ? 'activo' : ''}`} onClick={() => { setColor(c); setFotoElegida(null); }}>{c}</button>
              ))}
            </div>
          </div>

          <div className="opcion">
            <span className="opcion-titulo">Cantidad</span>
            <div className="cantidad-fila">
              <div className="stepper">
                <button type="button" onClick={() => cambiarCantidad(cantidad - 1)} disabled={agotado} aria-label="Menos">−</button>
                <input type="number" min="1" max={stock || 1} value={cantidad} onChange={(e) => cambiarCantidad(e.target.value)} disabled={agotado} aria-label="Cantidad" />
                <button type="button" onClick={() => cambiarCantidad(cantidad + 1)} disabled={agotado} aria-label="Más">+</button>
              </div>
              <span className={`stock ${agotado ? 'agotado' : ''}`}>
                {agotado ? 'Agotado' : `${stock} ${stock === 1 ? 'unidad disponible' : 'unidades disponibles'}`}
              </span>
            </div>
          </div>

          {aviso && <p className="aviso-error">{aviso}</p>}

          <div className="detalle-acciones">
            <button type="button" className="btn-oscuro grande" onClick={agregar} disabled={agotado}>
              {agotado ? 'Producto agotado' : 'Agregar al carrito'}
            </button>
            <button
              type="button"
              className={`btn-fav ${esFavorito ? 'activo' : ''}`}
              onClick={() => onToggleFavorito(id)}
              aria-pressed={esFavorito}
              aria-label={esFavorito ? 'Quitar de la lista de deseos' : 'Guardar en la lista de deseos'}
            >
              <IconHeart lleno={esFavorito} />
            </button>
          </div>
        </div>
      </div>

      {relacionados.length > 0 && (
        <div className="relacionados">
          <h2 className="seccion-titulo">También te puede gustar</h2>
          <ProductGrid productos={relacionados} favoritos={favoritos} onToggleFavorito={onToggleFavorito} onAbrir={onAbrir} escalas={escalas} />
        </div>
      )}
    </section>
  );
};

export default ProductDetail;
