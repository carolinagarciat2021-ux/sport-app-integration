// Funciones auxiliares para leer un producto sin importar si viene de la API
// (id_producto, precio_mayorista...) o ya normalizado (idProducto, precioMayorista...).

export const idDe = (p) => p.idProducto ?? p.id_producto;
export const nombreDe = (p) => p.nombre || p.descripcion || 'Producto';
export const precioDe = (p) => Number(p.precioMayorista ?? p.precio_mayorista ?? 0);
export const stockDe = (p) => Number(p.stock) || 0;

const lista = (texto, porDefecto) => {
  const l = texto ? String(texto).split(',').map((x) => x.trim()).filter(Boolean) : [];
  return l.length ? l : [porDefecto];
};

export const coloresDe = (p) => lista(p.color, 'Único');
export const tallasDe = (p) => lista(p.talla, 'Única');

// "Negro=url1;Azul=url2"  ->  { Negro: 'url1', Azul: 'url2' }
export const mapaImagenes = (p) => {
  const mapa = {};
  const crudo = p.imagenesPorColor ?? p.imagenes_por_color ?? '';
  String(crudo).split(';').forEach((par) => {
    const i = par.indexOf('=');
    if (i > 0) {
      const color = par.slice(0, i).trim();
      const url = par.slice(i + 1).trim();
      // Los enlaces "blob:" eran temporales (de cuando la foto se elegía sin subirla) y ya no existen: se ignoran
      if (color && url && !url.startsWith('blob:')) mapa[color] = url;
    }
  });
  return mapa;
};

export const imagenGeneral = (p) => {
  const url = p.imagenUrl || p.imagen_url || p.imagen || '';
  return url.startsWith('blob:') ? '' : url;
};

// Imagen del producto para un color (si ese color no tiene foto propia, usa la general)
export const imagenDe = (p, color) => mapaImagenes(p)[color] || imagenGeneral(p) || '';

// Todas las fotos distintas del producto
export const galeriaDe = (p) => {
  const todas = [imagenGeneral(p), ...Object.values(mapaImagenes(p))].filter(Boolean);
  return Array.from(new Set(todas));
};

// Para buscar sin importar tildes ni mayúsculas
export const normalizar = (t) =>
  String(t || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim();

export const coincideBusqueda = (p, consulta) => {
  const q = normalizar(consulta);
  if (!q) return false;
  const texto = normalizar([nombreDe(p), p.descripcion, p.categoria, p.genero, p.color].join(' '));
  return q.split(/\s+/).every((palabra) => texto.includes(palabra));
};
