import React from 'react';
import Drawer from './Drawer';
import { IconHeart } from './Icons';
import ImagenProducto from '../modules/store/ImagenProducto';
import { formatoCOP } from '../utils/descuentos';
import { idDe, nombreDe, precioDe, coloresDe, imagenDe, imagenGeneral } from '../modules/store/productoUtils';

/** Lista de deseos: lo que la persona quiere comprar más adelante. Se guarda en este navegador. */
const WishlistDrawer = ({ abierto, onCerrar, productos, onAbrir, onQuitar }) => (
  <Drawer abierto={abierto} titulo="Lista de deseos" onCerrar={onCerrar}>
    {productos.length === 0 ? (
      <div className="dw-vacio">
        <span className="dw-vacio-icono"><IconHeart /></span>
        <p className="dw-vacio-t">La lista de deseos está vacía</p>
        <p>Toca el corazón de un producto para guardarlo aquí y comprarlo después.</p>
        <button type="button" className="btn-linea" onClick={onCerrar}>Ver la tienda</button>
      </div>
    ) : (
      <ul className="dw-lista">
        {productos.map((p) => (
          <li key={idDe(p)} className="dw-item">
            <button type="button" className="dw-item-imgbtn" onClick={() => { onAbrir(idDe(p)); onCerrar(); }} aria-label={`Ver ${nombreDe(p)}`}>
              <ImagenProducto src={imagenDe(p, coloresDe(p)[0])} respaldo={imagenGeneral(p)} alt={nombreDe(p)} className="dw-item-img" />
            </button>
            <div className="dw-item-info">
              <p className="dw-item-nombre">{nombreDe(p)}</p>
              <p className="dw-item-det">{formatoCOP(precioDe(p))}</p>
              <div className="dw-item-fila">
                <button type="button" className="btn-linea pequeno" onClick={() => { onAbrir(idDe(p)); onCerrar(); }}>Ver producto</button>
                <button type="button" className="dw-quitar" onClick={() => onQuitar(idDe(p))}>Quitar</button>
              </div>
            </div>
          </li>
        ))}
      </ul>
    )}
  </Drawer>
);

export default WishlistDrawer;
