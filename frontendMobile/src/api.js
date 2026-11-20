// frontend/src/api.js
// Capa de comunicación entre el frontend (React) y el backend (ServidorApi.java / MySQL).

// En desarrollo local (npm start) usa tu backend en localhost. En producción
// (una vez despliegues en Railway/Render), Create React App reemplaza esto
// automáticamente por el valor de REACT_APP_API_URL definido en el archivo
// .env.production — no necesitas tocar este archivo para publicar la app,
// solo llenar .env.production con la URL real de tu backend.

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8081/api';

// Las fotos subidas desde el computador se guardan como "/api/imagenes/<id>" (ruta relativa).
// Esta función les antepone la dirección del servidor para poder mostrarlas.
const ORIGEN_API = API_BASE.replace(/\/api\/?$/, '');
export const urlImagen = (url) => (url && url.startsWith('/api/') ? `${ORIGEN_API}${url}` : url);

// ============ MANEJO DEL TOKEN DE SESIÓN ============
// El token lo devuelve /api/login y hay que reenviarlo en cada petición
// protegida (header Authorization). Se guarda en localStorage para que la
// sesión sobreviva si el usuario refresca la página.
const CLAVE_TOKEN = 'tiendaSport_token';

export const guardarToken = (token) => {
  if (token) localStorage.setItem(CLAVE_TOKEN, token);
};

export const obtenerToken = () => localStorage.getItem(CLAVE_TOKEN);

export const borrarToken = () => localStorage.removeItem(CLAVE_TOKEN);

// Permite que App.jsx se entere cuando el backend rechaza el token (sesión
// vencida o el servidor se reinició y perdió la sesión en memoria), para
// cerrar sesión automáticamente en vez de dejar un error confuso en pantalla.
let manejador401 = null;
export const registrarManejador401 = (callback) => { manejador401 = callback; };

/**
 * Función genérica para enviar peticiones HTTP a la API.
 * Soporta GET, POST, PUT y DELETE, y adjunta el token de sesión
 * automáticamente si el usuario ya inició sesión.
 */
const apiRequest = async (endpoint, metodo = 'GET', datos = null) => {
  try {
    const headers = { 'Content-Type': 'application/json' };
    const token = obtenerToken();
    if (token) headers['Authorization'] = `Bearer ${token}`;

    const opciones = { method: metodo, headers };
    if (datos !== null) {
      opciones.body = JSON.stringify(datos);
    }

    const respuesta = await fetch(`${API_BASE}/${endpoint}`, opciones);

    // Intentamos parsear el JSON aunque la respuesta no sea 2xx,
    // porque el backend siempre responde con un cuerpo JSON (incluso en errores).
    let cuerpo = null;
    const texto = await respuesta.text();
    if (texto) {
      try { cuerpo = JSON.parse(texto); } catch (e) { cuerpo = texto; }
    }

    if (respuesta.status === 401 && token) {
      // El token que teníamos guardado ya no es válido para el backend
      // (sesión vencida, o el servidor se reinició y la perdió). Lo borramos
      // y avisamos a App.jsx para que cierre la sesión en la interfaz también,
      // en vez de dejar al usuario viendo "Debes iniciar sesión" sin explicación.
      borrarToken();
      if (manejador401) manejador401();
    }

    if (!respuesta.ok) {
      const mensajeError = (cuerpo && cuerpo.error) ? cuerpo.error : `Error ${respuesta.status} en el servidor`;
      throw new Error(mensajeError);
    }

    return cuerpo;
  } catch (error) {
    console.error(`Error al conectar con /api/${endpoint}:`, error);
    throw error;
  }
};

// ============ CLIENTES ============
// Subir una foto de producto (solo Administrador). Devuelve su dirección, ej.: "/api/imagenes/12"
export const subirImagen = async (archivo) => {
  const token = obtenerToken();
  const formulario = new FormData();
  formulario.append('archivo', archivo);
  const respuesta = await fetch(`${API_BASE}/imagenes`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formulario,
  });
  const texto = await respuesta.text();
  let cuerpo = null;
  try { cuerpo = JSON.parse(texto); } catch (e) { cuerpo = null; }
  if (!respuesta.ok) {
    throw new Error((cuerpo && cuerpo.error) ? cuerpo.error : `No se pudo subir la foto (error ${respuesta.status}).`);
  }
  return cuerpo.url;
};

// Pedidos para Administrador y Vendedor: con quién compró (nombre, correo) y sus productos, con filtros y páginas
export const listarPedidosStaff = ({ q = '', desde = '', hasta = '', min = '', max = '', estado = '', pagina = 0, tamano = 20 } = {}) => {
  const params = new URLSearchParams();
  if (q) params.set('q', q);
  if (desde) params.set('desde', desde);
  if (hasta) params.set('hasta', hasta);
  if (min !== '' && min !== null) params.set('min', min);
  if (max !== '' && max !== null) params.set('max', max);
  if (estado) params.set('estado', estado);
  params.set('pagina', pagina);
  params.set('tamano', tamano);
  return apiRequest(`pedidos/todos?${params.toString()}`, 'GET');
};

// Mis pedidos (cliente): con productos y línea de tiempo en una sola respuesta
export const listarMisPedidos = () => apiRequest('pedidos/mis-pedidos', 'GET');

// Historial de actividad (solo Administrador, solo lectura)
export const listarActividad = (pagina = 0, accion = '') =>
  apiRequest(`actividad?pagina=${pagina}&tamano=25${accion ? `&accion=${encodeURIComponent(accion)}` : ''}`, 'GET');
export const listarClientes = () => apiRequest('clientes', 'GET'); // solo Administrador
export const registrarCliente = (datosCliente) => apiRequest('clientes', 'POST', datosCliente);
export const actualizarCliente = (datosCliente) => apiRequest(`clientes/${datosCliente.id_clientes}`, 'PUT', datosCliente);
export const eliminarCliente = (id) => apiRequest(`clientes/${id}`, 'DELETE');

// ============ LOGIN ============
// Espera { correo, password, rol } y devuelve los datos del cliente autenticado + token.
export const iniciarSesion = async (credenciales) => {
  const resultado = await apiRequest('login', 'POST', credenciales);
  if (resultado && resultado.token) {
    guardarToken(resultado.token);
  }
  return resultado;
};

export const cerrarSesion = () => {
  borrarToken();
};

// ============ CONFIGURACIÓN (monto mínimo de compra) ============
// Escala de descuentos mayoristas: GET público, PUT solo Administrador
export const listarDescuentos = () => apiRequest('descuentos', 'GET');
export const guardarDescuentos = (escalas) => apiRequest('descuentos', 'PUT', { escalas });
// Datos de la empresa: GET público, PUT solo Administrador
export const obtenerEmpresa = () => apiRequest('empresa', 'GET');
export const guardarEmpresa = (datos) => apiRequest('empresa', 'PUT', datos);
export const guardarEnvioGratis = (valor) => apiRequest('empresa/envio-gratis', 'PUT', { envio_gratis_desde: Number(valor) || 0 });
export const obtenerConfiguracion = () => apiRequest('configuracion', 'GET'); // pública
export const actualizarConfiguracion = (montoMinimoCompra) =>
  apiRequest('configuracion', 'PUT', { id: 1, monto_minimo_compra: montoMinimoCompra }); // solo Administrador


// ============ CATEGORÍAS (tipos de prenda) ============
export const listarCategorias = () => apiRequest('categorias', 'GET');
export const guardarCategoria = (datosCategoria) => apiRequest('categorias', 'POST', datosCategoria); // Admin
export const actualizarCategoria = (datosCategoria) => apiRequest(`categorias/${datosCategoria.id_categoria}`, 'PUT', datosCategoria); // Admin
export const eliminarCategoria = (id) => apiRequest(`categorias/${id}`, 'DELETE'); // Admin

// ============ PRODUCTOS ============
// Tipos de prenda dentro de cada categoría: GET público; el resto solo Administrador
export const listarSubcategorias = () => apiRequest('subcategorias', 'GET');
export const crearSubcategoria = (datos) => apiRequest('subcategorias', 'POST', datos);
export const actualizarSubcategoria = (id, datos) => apiRequest(`subcategorias/${id}`, 'PUT', datos);
export const eliminarSubcategoria = (id) => apiRequest(`subcategorias/${id}`, 'DELETE');
export const asignarProductosSubcategoria = (id, ids) => apiRequest(`subcategorias/${id}/productos`, 'PUT', { ids });

export const listarProductos = () => apiRequest('producto', 'GET'); // pública (invitados incluidos)
export const guardarProducto = (datosProducto) => apiRequest('producto', 'POST', datosProducto); // Admin/Vendedor
export const actualizarProducto = (datosProducto) => apiRequest(`producto/${datosProducto.id_producto}`, 'PUT', datosProducto); // Admin/Vendedor
export const eliminarProducto = (id) => apiRequest(`producto/${id}`, 'DELETE'); // Admin

// ============ PEDIDOS ============
export const listarPedidos = () => apiRequest('pedidos', 'GET'); // requiere sesión
// datosPedido: { estado }. El id_cliente lo toma el backend del token, no del body.
export const crearPedido = (datosPedido) => apiRequest('pedidos', 'POST', datosPedido);
export const actualizarPedido = (datosPedido) => apiRequest(`pedidos/${datosPedido.id_pedido}`, 'PUT', datosPedido); // Admin/Vendedor
// Solo Administrador. Anular = actualizarPedido con estado 'Cancelado'; eliminar = borrado definitivo (devuelve el stock).
export const eliminarPedido = (id) => apiRequest(`pedidos/${id}`, 'DELETE');

// ============ DETALLE DE PEDIDO (líneas del carrito) ============
// datosDetalle: { id_pedido, id_producto, cantidad, precio_unitario }
export const crearDetallePedido = (datosDetalle) => apiRequest('detalle_pedido', 'POST', datosDetalle); // requiere sesión
export const listarDetallePedido = (idPedido) => apiRequest(`detalle_pedido?id_pedido=${idPedido}`, 'GET');

/**
 * Confirma la compra con UNA sola llamada: POST pedidos/completo.
 * Solo se envía qué producto, talla, color y cantidad; el servidor lee los
 * precios de la base de datos, calcula el total, valida stock y monto mínimo,
 * y crea pedido + detalles en una transacción.
 * items: [{ id_producto, cantidad, talla, color, ... }] (cualquier precio se ignora)
 * Devuelve el pedido confirmado con subtotal, descuento aplicado y total final.
 */
export const enviarPedidoCompleto = async (items) => {
  const lineas = items.map(({ id_producto, talla, color, cantidad }) => ({
    id_producto, talla, color, cantidad,
  }));
  // Devuelve { id_pedido, subtotal, descuento_porcentaje, descuento_valor, total, estado }
  return apiRequest('pedidos/completo', 'POST', lineas);
};

/**
 * Trae todos los pedidos junto con su detalle (líneas de productos comprados).
 * Si el usuario es Administrador/Vendedor, el backend devuelve TODOS los pedidos;
 * si es Cliente, el backend ya filtra y solo devuelve los suyos.
 */
export const listarPedidosConDetalle = async () => {
  const pedidos = await listarPedidos();
  const pedidosConDetalle = await Promise.all(
    (pedidos || []).map(async (pedido) => {
      const detalle = await listarDetallePedido(pedido.id_pedido);
      return { ...pedido, detalle: detalle || [] };
    })
  );
  return pedidosConDetalle;
};
