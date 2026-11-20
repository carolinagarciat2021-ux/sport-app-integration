import React, { useMemo, useState } from 'react';
import { calcularDescuento, formatoCOP } from '../utils/descuentos';
import { useEmpresa } from '../config/EmpresaContext';

/**
 * Franja negra superior con texto en movimiento de derecha a izquierda: descuentos (los define el
 * administrador), envío gratis y el lema. Se detiene al pasar el mouse y tiene un botón de pausa.
 */
const AnnouncementBar = ({ escalas }) => {
  const EMPRESA = useEmpresa();
  const [pausada, setPausada] = useState(false);

  const mensajes = useMemo(() => {
    const lista = calcularDescuento(escalas, 0).escalas.map(
      (e) => `Compras desde ${formatoCOP(e.monto)}: ${e.pct}% de descuento`
    );
    if (EMPRESA.envioGratisDesde > 0) {
      lista.push(`Envío gratis en compras superiores a ${formatoCOP(EMPRESA.envioGratisDesde)}`);
    }
    lista.push(EMPRESA.lema);
    return lista;
  }, [escalas, EMPRESA]);

  return (
    <div className={`ab ${pausada ? 'pausada' : ''}`} role="region" aria-label="Promociones">
      <div className="ab-ventana">
        <div className="ab-track">
          {[0, 1].map((copia) => (
            <div className="ab-grupo" key={copia} aria-hidden={copia === 1}>
              {mensajes.map((m, i) => (
                <span className="ab-item" key={i}>{m}</span>
              ))}
            </div>
          ))}
        </div>
      </div>
      <button
        type="button"
        className="ab-pausa"
        onClick={() => setPausada(!pausada)}
        aria-label={pausada ? 'Reanudar el texto en movimiento' : 'Pausar el texto en movimiento'}
        title={pausada ? 'Reanudar' : 'Pausar'}
      >
        {pausada ? '▶' : '❚❚'}
      </button>
    </div>
  );
};

export default AnnouncementBar;
