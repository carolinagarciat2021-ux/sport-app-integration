import React, { useState } from 'react';
import { calcularDescuento, formatoCOP } from '../../utils/descuentos';
import ImagenProducto from '../store/ImagenProducto';

const METODOS_PAGO = [
  { valor: 'contraentrega', etiqueta: '💵 Pago contra entrega (efectivo)' },
  { valor: 'transferencia', etiqueta: '🏦 Transferencia bancaria' }
];

const CARBON = '#1f1d1b';
const ORO = '#b8975a';

/**
 * Componente: CartView
 * Carrito de compras con descuentos mayoristas.
 * - Barra de progreso hacia el siguiente escalón de descuento.
 * - Cuando hay descuento: subtotal tachado, línea de ahorro y total final resaltado.
 * - El descuento que se muestra aquí es informativo; el servidor lo recalcula al confirmar
 *   y la pantalla de éxito muestra los valores oficiales del pedido.
 */
const CartView = ({ cart = [], user, escalas = [], montoMinimo = 0, onUpdateQuantity, onRemoveItem, onCheckout, onGoToStore, onGoToLogin, onGoToRegister }) => {
  const [procesando, setProcesando] = useState(false);
  const [metodoPago, setMetodoPago] = useState('');
  const [pedidoConfirmado, setPedidoConfirmado] = useState(null); // respuesta del servidor + método de pago

  const subtotal = cart.reduce((acc, item) => acc + (item.precioMayorista * item.cantidad), 0);
  const calc = calcularDescuento(escalas, subtotal);
  const faltaMinimo = montoMinimo > 0 && subtotal < montoMinimo;

  const handleConfirmar = async () => {
    if (cart.length === 0 || !metodoPago || faltaMinimo) return;

    setProcesando(true);
    try {
      const pedido = await onCheckout();
      // Guardamos el resultado ANTES de que el carrito se vacíe, para mostrar la pantalla de éxito.
      setPedidoConfirmado({ ...pedido, metodoPago });
    } catch (error) {
      alert(`❌ No se pudo completar el pedido: ${error.message}`);
    } finally {
      setProcesando(false);
    }
  };

  const handleVolverAComprar = () => {
    setPedidoConfirmado(null);
    setMetodoPago('');
    onGoToStore();
  };

  const boton = (fondo, texto = '#fff') => ({
    padding: '12px 26px', backgroundColor: fondo, color: texto, border: 'none', borderRadius: '2px',
    fontWeight: 600, letterSpacing: '0.06em', textTransform: 'uppercase', fontSize: '0.8rem', cursor: 'pointer'
  });

  // ---------- Pantalla de ÉXITO tras confirmar el pedido ----------
  if (pedidoConfirmado) {
    const etiquetaMetodo = METODOS_PAGO.find(m => m.valor === pedidoConfirmado.metodoPago)?.etiqueta || '';
    const ahorro = Number(pedidoConfirmado.descuento_valor) || 0;
    return (
      <div style={{ textAlign: 'center', padding: '50px 20px', backgroundColor: '#fff', border: `1px solid ${ORO}`, marginTop: '20px' }}>
        <div style={{ fontSize: '2.4rem', color: ORO }}>✓</div>
        <h2 style={{ color: CARBON, marginBottom: '4px' }}>Pedido confirmado</h2>
        <p style={{ color: '#555', fontSize: '1.05rem' }}>
          Tu pedido <strong>#{pedidoConfirmado.id_pedido}</strong> quedó registrado correctamente.
        </p>
        {ahorro > 0 && (
          <p style={{ color: '#2f6b3f', margin: '4px 0' }}>
            Subtotal <s>{formatoCOP(pedidoConfirmado.subtotal)}</s> · Ahorraste <strong>{formatoCOP(ahorro)}</strong> ({Number(pedidoConfirmado.descuento_porcentaje)}% dto.)
          </p>
        )}
        <p style={{ color: '#555' }}>
          Total pagado: <strong>{formatoCOP(pedidoConfirmado.total)}</strong> · Método de pago: <strong>{etiquetaMetodo}</strong>
        </p>
        <p style={{ color: '#888', fontSize: '0.9rem', maxWidth: '480px', margin: '10px auto' }}>
          Si elegiste transferencia, coordina el pago con la tienda; si elegiste contra entrega, pagas cuando te llegue el pedido.
        </p>
        <button onClick={handleVolverAComprar} style={{ ...boton(CARBON), marginTop: '15px' }}>Seguir comprando</button>
      </div>
    );
  }

  // ---------- Carrito vacío ----------
  if (cart.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '60px 20px', backgroundColor: '#fff', border: '1px solid #e6dfd2', marginTop: '20px' }}>
        <h2 style={{ color: CARBON }}>Tu carrito está vacío</h2>
        <p style={{ color: '#666' }}>Agrega productos desde el catálogo para poder hacer un pedido.</p>
        <button onClick={onGoToStore} style={{ ...boton(CARBON), marginTop: '15px' }}>Ir al catálogo</button>
      </div>
    );
  }

  // Barra de progreso hacia el siguiente escalón
  const progreso = calc.siguiente ? Math.min(100, (subtotal / calc.siguiente.monto) * 100) : 100;

  return (
    <div style={{ marginTop: '20px' }}>
      <h2 style={{ color: CARBON }}>Carrito de compras</h2>

      {/* ---- Barra de progreso de descuento ---- */}
      {calc.escalas.length > 0 && (
        <div style={{ backgroundColor: '#fbf7ef', border: '1px solid #e8dcc3', padding: '14px 18px', marginBottom: '16px' }}>
          <div style={{ color: '#5c4a24', marginBottom: '8px' }}>
            {calc.siguiente ? (
              <>Te faltan <strong>{formatoCOP(calc.faltante)}</strong> para activar tu <strong>{calc.siguiente.pct}%</strong> de descuento mayorista</>
            ) : (
              <>¡Tienes el máximo descuento mayorista: <strong>{calc.porcentaje}%</strong>!</>
            )}
            {calc.actual && calc.siguiente && (
              <span style={{ color: '#2f6b3f', marginLeft: '8px' }}>(ahora tienes {calc.porcentaje}% activo)</span>
            )}
          </div>
          <div style={{ height: '8px', backgroundColor: '#eadfc6', borderRadius: '4px', overflow: 'hidden' }}>
            <div style={{ width: `${progreso}%`, height: '100%', backgroundColor: ORO, transition: 'width 0.3s' }} />
          </div>
        </div>
      )}

      {faltaMinimo && (
        <div style={{ backgroundColor: '#fff4e5', border: '1px solid #f0c987', color: '#8a5a00', padding: '12px 16px', marginBottom: '16px' }}>
          La compra mínima es de <strong>{formatoCOP(montoMinimo)}</strong>. Te faltan <strong>{formatoCOP(montoMinimo - subtotal)}</strong> para poder confirmar.
        </div>
      )}

      {!user && (
        <div style={{ backgroundColor: '#fff', border: '1px solid #e6dfd2', padding: '14px 18px', marginBottom: '16px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
          <span style={{ color: '#555' }}>
            Estás comprando como invitado. Tu carrito se conserva, pero para confirmar necesitas iniciar sesión o crear una cuenta.
          </span>
          <div style={{ display: 'flex', gap: '8px' }}>
            <button onClick={onGoToLogin} style={boton(CARBON)}>Iniciar sesión</button>
            <button onClick={onGoToRegister} style={{ ...boton('#fff', CARBON), border: `1px solid ${CARBON}` }}>Registrarme</button>
          </div>
        </div>
      )}

      <div style={{ backgroundColor: '#fff', border: '1px solid #e6dfd2', overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: '560px' }}>
          <thead>
            <tr style={{ background: '#faf8f5', borderBottom: '1px solid #e6dfd2', textAlign: 'left' }}>
              <th style={{ padding: '10px' }}>Producto</th>
              <th style={{ padding: '10px' }}>Talla</th>
              <th style={{ padding: '10px' }}>Color</th>
              <th style={{ padding: '10px' }}>Cantidad</th>
              <th style={{ padding: '10px' }}>Subtotal</th>
              <th style={{ padding: '10px' }}></th>
            </tr>
          </thead>
          <tbody>
            {cart.map((item, idx) => (
              <tr key={`${item.idProducto}-${item.talla}-${item.color}-${idx}`} style={{ borderBottom: '1px solid #f1ece2' }}>
                <td style={{ padding: '10px', display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <ImagenProducto src={item.imagenUrl} alt={item.nombre} className="cart-miniatura" />
                  <strong>{item.nombre}</strong>
                </td>
                <td style={{ padding: '10px' }}>{item.talla}</td>
                <td style={{ padding: '10px' }}>{item.color}</td>
                <td style={{ padding: '10px' }}>
                  <input
                    type="number"
                    min="1"
                    max={item.stockDisponible || 99}
                    value={item.cantidad}
                    onChange={(e) => onUpdateQuantity(idx, Number(e.target.value))}
                    style={{ width: '60px', padding: '6px' }}
                  />
                </td>
                <td style={{ padding: '10px', fontWeight: 600 }}>
                  {formatoCOP(item.precioMayorista * item.cantidad)}
                </td>
                <td style={{ padding: '10px' }}>
                  <button
                    onClick={() => onRemoveItem(idx)}
                    style={{ background: 'none', border: '1px solid #c9b8b8', color: '#8c2f2f', padding: '6px 10px', cursor: 'pointer' }}
                  >
                    Quitar
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Método de pago: obligatorio antes de confirmar */}
      {user && (
        <div style={{ backgroundColor: '#fff', border: '1px solid #e6dfd2', padding: '16px 18px', marginTop: '16px' }}>
          <h3 style={{ margin: '0 0 10px 0', color: CARBON, fontSize: '1.05rem' }}>Método de pago</h3>
          {METODOS_PAGO.map((m) => (
            <label key={m.valor} style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '6px 0', cursor: 'pointer' }}>
              <input type="radio" name="metodoPago" value={m.valor} checked={metodoPago === m.valor} onChange={(e) => setMetodoPago(e.target.value)} />
              {m.etiqueta}
            </label>
          ))}
        </div>
      )}

      {/* ---- Resumen: subtotal / ahorro / total ---- */}
      <div style={{ marginTop: '20px', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: '6px' }}>
        {calc.valor > 0 ? (
          <>
            <span style={{ color: '#777' }}>Subtotal: <s>{formatoCOP(subtotal)}</s></span>
            <span style={{ color: '#2f6b3f', fontWeight: 600 }}>
              Descuento mayorista ({calc.porcentaje}%): −{formatoCOP(calc.valor)}
            </span>
            <span style={{ fontSize: '1.5rem', fontWeight: 700, color: CARBON }}>
              Total: {formatoCOP(calc.total)}
            </span>
          </>
        ) : (
          <span style={{ fontSize: '1.4rem', fontWeight: 700, color: CARBON }}>Total: {formatoCOP(subtotal)}</span>
        )}
        <span style={{ fontSize: '0.75rem', color: '#8a8a8a', maxWidth: '420px', textAlign: 'right' }}>
          El descuento se calcula sobre el valor total de la compra. El valor final lo confirma el sistema al registrar tu pedido.
        </span>

        {user ? (
          <button
            onClick={handleConfirmar}
            disabled={procesando || !metodoPago || faltaMinimo}
            title={faltaMinimo ? 'Aún no alcanzas la compra mínima' : (!metodoPago ? 'Elige un método de pago primero' : '')}
            style={{
              ...boton((procesando || !metodoPago || faltaMinimo) ? '#b5b0a6' : CARBON),
              cursor: (procesando || !metodoPago || faltaMinimo) ? 'not-allowed' : 'pointer',
              marginTop: '8px'
            }}
          >
            {procesando ? 'Procesando...' : 'Confirmar pedido'}
          </button>
        ) : (
          <button onClick={onGoToLogin} style={{ ...boton(ORO, CARBON), marginTop: '8px' }}>Inicia sesión para comprar</button>
        )}
      </div>
    </div>
  );
};

export default CartView;
