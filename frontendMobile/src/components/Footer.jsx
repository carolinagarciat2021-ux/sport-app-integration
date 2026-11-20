import React from 'react';
import { calcularDescuento, formatoCOP } from '../utils/descuentos';
import { useEmpresa } from '../config/EmpresaContext';

const Footer = ({ categorias = [], escalas = [], user, onCategoria, setView }) => {
  const EMPRESA = useEmpresa();
  const escalones = calcularDescuento(escalas, 0).escalas;
  const anio = new Date().getFullYear();

  return (
    <footer className="ft">
      <div className="ft-grid">
        <div>
          <p className="ft-marca">{EMPRESA.nombre}</p>
          <p className="ft-texto">{EMPRESA.lema}</p>
          {(EMPRESA.instagram || EMPRESA.facebook) && (
            <p className="ft-texto">
              {EMPRESA.instagram && <a href={EMPRESA.instagram} target="_blank" rel="noopener noreferrer">Instagram</a>}
              {EMPRESA.instagram && EMPRESA.facebook && ' · '}
              {EMPRESA.facebook && <a href={EMPRESA.facebook} target="_blank" rel="noopener noreferrer">Facebook</a>}
            </p>
          )}
        </div>

        <div>
          <p className="ft-titulo">Contacto</p>
          <ul>
            <li>
              <a href={`https://wa.me/${EMPRESA.whatsapp}?text=${encodeURIComponent(EMPRESA.mensajeWhatsapp)}`} target="_blank" rel="noopener noreferrer">
                WhatsApp
              </a>
            </li>
            {EMPRESA.correo && <li>{EMPRESA.correo}</li>}
            {EMPRESA.direccion && <li>{EMPRESA.direccion}</li>}
            {EMPRESA.horario && <li>{EMPRESA.horario}</li>}
            {EMPRESA.tiendaVirtual && <li>{EMPRESA.tiendaVirtual}</li>}
          </ul>
        </div>

        <div>
          <p className="ft-titulo">Comprar</p>
          <ul>
            {categorias.map((c) => (
              <li key={c.id_categoria}>
                <button type="button" className="ft-link" onClick={() => onCategoria(c.id_categoria)}>{c.nombre}</button>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <p className="ft-titulo">Tu compra</p>
          <ul>
            {escalones.map((e) => (
              <li key={e.monto}>Desde {formatoCOP(e.monto)}: {e.pct}% de descuento</li>
            ))}
            {EMPRESA.envioGratisDesde > 0 && <li>Envío gratis desde {formatoCOP(EMPRESA.envioGratisDesde)}</li>}
            <li>Pago contra entrega o transferencia</li>
            {user && (
              <li><button type="button" className="ft-link" onClick={() => setView('pedidos')}>Mis pedidos</button></li>
            )}
          </ul>
        </div>
      </div>
      <p className="ft-copy">© {anio} {EMPRESA.nombre}. Todos los derechos reservados.</p>
    </footer>
  );
};

export default Footer;
