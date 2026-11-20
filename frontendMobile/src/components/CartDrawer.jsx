import React from 'react';
import Drawer from './Drawer';
import ImagenProducto from '../modules/store/ImagenProducto';
import { calcularDescuento, formatoCOP } from '../utils/descuentos';

/** Carrito lateral: resumen rápido. El pago se confirma en la pantalla "Finalizar compra". */
const CartDrawer = ({ abierto, onCerrar, cart, escalas, onUpdateQuantity, onRemoveItem, onFinalizar }) => {
  const subtotal = cart.reduce((acc, i) => acc + i.precioMayorista * i.cantidad, 0);
  const calc = calcularDescuento(escalas, subtotal);
  const cantidadTotal = cart.reduce((acc, i) => acc + i.cantidad, 0);

  const pie = cart.length > 0 && (
    <>
      <div className="dw-resumen">
        {calc.valor > 0 ? (
          <>
            <div><span>Subtotal</span><s>{formatoCOP(subtotal)}</s></div>
            <div className="ahorro"><span>Descuento {calc.porcentaje}%</span><b>−{formatoCOP(calc.valor)}</b></div>
            <div className="total"><span>Total</span><b>{formatoCOP(calc.total)}</b></div>
          </>
        ) : (
          <div className="total"><span>Subtotal</span><b>{formatoCOP(subtotal)}</b></div>
        )}
      </div>
      <button type="button" className="btn-oscuro grande ancho" onClick={onFinalizar}>Finalizar compra</button>
      <button type="button" className="btn-linea ancho" onClick={onCerrar}>Seguir comprando</button>
    </>
  );

  return (
    <Drawer abierto={abierto} titulo={`Carrito${cantidadTotal ? ` (${cantidadTotal})` : ''}`} onCerrar={onCerrar} pie={pie}>
      {cart.length === 0 ? (
        <div className="dw-vacio">
          <p className="dw-vacio-t">Tu carrito está vacío</p>
          <p>Cuando agregues productos los verás aquí.</p>
          <button type="button" className="btn-linea" onClick={onCerrar}>Volver a la tienda</button>
        </div>
      ) : (
        <>
          {calc.escalas.length > 0 && (
            <div className="dw-progreso">
              <p>
                {calc.siguiente
                  ? <>Te faltan <b>{formatoCOP(calc.faltante)}</b> para tu <b>{calc.siguiente.pct}%</b> de descuento</>
                  : <>¡Tienes el máximo descuento: <b>{calc.porcentaje}%</b>!</>}
              </p>
              <div className="barra"><div style={{ width: `${calc.siguiente ? Math.min(100, (subtotal / calc.siguiente.monto) * 100) : 100}%` }} /></div>
            </div>
          )}
          <ul className="dw-lista">
            {cart.map((item, idx) => (
              <li key={`${item.idProducto}-${item.talla}-${item.color}`} className="dw-item">
                <ImagenProducto src={item.imagenUrl} alt={item.nombre} className="dw-item-img" />
                <div className="dw-item-info">
                  <p className="dw-item-nombre">{item.nombre}</p>
                  <p className="dw-item-det">Talla {item.talla} · {item.color}</p>
                  <div className="dw-item-fila">
                    <div className="stepper pequeno">
                      <button type="button" onClick={() => onUpdateQuantity(idx, item.cantidad - 1)} aria-label="Menos">−</button>
                      <span>{item.cantidad}</span>
                      <button type="button" onClick={() => onUpdateQuantity(idx, item.cantidad + 1)} aria-label="Más">+</button>
                    </div>
                    <b>{formatoCOP(item.precioMayorista * item.cantidad)}</b>
                  </div>
                  <button type="button" className="dw-quitar" onClick={() => onRemoveItem(idx)}>Quitar</button>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}
    </Drawer>
  );
};

export default CartDrawer;
