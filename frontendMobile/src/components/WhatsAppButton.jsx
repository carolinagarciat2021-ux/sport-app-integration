import React from 'react';
import { IconWhatsApp } from './Icons';
import { useEmpresa } from '../config/EmpresaContext';

/** Botón flotante abajo a la derecha para escribir por WhatsApp. */
const WhatsAppButton = () => {
  const EMPRESA = useEmpresa();
  return (
  <a
    className="wa"
    href={`https://wa.me/${EMPRESA.whatsapp}?text=${encodeURIComponent(EMPRESA.mensajeWhatsapp)}`}
    target="_blank"
    rel="noopener noreferrer"
    aria-label="Escríbenos por WhatsApp"
    title="¿Dudas? Escríbenos por WhatsApp (te respondemos en horario de oficina)"
  >
    <IconWhatsApp />
  </a>
  );
};

export default WhatsAppButton;
