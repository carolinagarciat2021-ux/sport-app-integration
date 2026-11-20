import { createContext, useContext } from 'react';
import { EMPRESA_POR_DEFECTO } from './empresa';

// Todos los componentes leen los datos de la empresa con useEmpresa(); App.jsx los carga del servidor.
export const EmpresaContext = createContext(EMPRESA_POR_DEFECTO);
export const useEmpresa = () => useContext(EmpresaContext);

// Convierte la respuesta de la API (nombres_con_guion_bajo) al formato que usa la interfaz
export const empresaDesdeApi = (d) => ({
  nombre: d.nombre || EMPRESA_POR_DEFECTO.nombre,
  lema: d.lema || EMPRESA_POR_DEFECTO.lema,
  whatsapp: d.whatsapp || EMPRESA_POR_DEFECTO.whatsapp,
  mensajeWhatsapp: d.mensaje_whatsapp || EMPRESA_POR_DEFECTO.mensajeWhatsapp,
  correo: d.correo || EMPRESA_POR_DEFECTO.correo,
  direccion: d.direccion || '',
  horario: d.horario || '',
  tiendaVirtual: d.tienda_virtual || '',
  instagram: d.instagram || '',
  facebook: d.facebook || '',
  envioGratisDesde: Number(d.envio_gratis_desde) || 0,
  correoAvisos: d.correo_avisos || '',
});

// Y al revés, para guardar
export const empresaHaciaApi = (e) => ({
  nombre: e.nombre,
  lema: e.lema,
  whatsapp: e.whatsapp,
  mensaje_whatsapp: e.mensajeWhatsapp,
  correo: e.correo,
  direccion: e.direccion,
  horario: e.horario,
  tienda_virtual: e.tiendaVirtual,
  instagram: e.instagram,
  facebook: e.facebook,
  correo_avisos: e.correoAvisos,
  // El envío gratis ya no se envía desde aquí: se edita en "Descuentos y compra mínima"
});
