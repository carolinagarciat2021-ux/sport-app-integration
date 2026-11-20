// Utilidades de descuento mayorista (solo para MOSTRAR en pantalla).
// El valor oficial lo calcula el servidor al confirmar el pedido.

export const formatoCOP = (n) => `$${Math.round(Number(n) || 0).toLocaleString('es-CO')}`;

/**
 * escalas: [{ monto_minimo, porcentaje }] (cualquier orden).
 * Devuelve el descuento que corresponde al subtotal, el siguiente escalón y cuánto falta para alcanzarlo.
 */
export const calcularDescuento = (escalas, subtotal) => {
  const ordenadas = (escalas || [])
    .map((e) => ({ monto: Number(e.monto_minimo), pct: Number(e.porcentaje) }))
    .filter((e) => e.monto > 0 && e.pct > 0)
    .sort((a, b) => a.monto - b.monto);

  let actual = null;
  ordenadas.forEach((e) => { if (subtotal >= e.monto) actual = e; });
  const siguiente = ordenadas.find((e) => e.monto > subtotal) || null;

  const porcentaje = actual ? actual.pct : 0;
  const valor = Math.round((subtotal * porcentaje) / 100);
  return {
    escalas: ordenadas,
    actual,
    siguiente,
    porcentaje,
    valor,
    total: subtotal - valor,
    faltante: siguiente ? siguiente.monto - subtotal : 0,
  };
};
