import React, { useState, useEffect, useCallback, useRef } from 'react';
import Navbar from './components/Navbar';
import LoginCard from './modules/auth/LoginCard';
import RegisterCard from './modules/auth/RegisterCard';
import AdminDashboard from './modules/dashboard/AdminDashboard';
import ProductForm from './modules/products/ProductForm';
import ProductList from './modules/products/ProductList';
import CartView from './modules/cart/CartView';
import UserManagement from './modules/users/UserManagement';
import MisPedidos from './modules/orders/MisPedidos';
import PedidosStaff from './modules/orders/PedidosStaff';
import ActivityLog from './modules/activity/ActivityLog';
import DescuentosAdmin from './modules/descuentos/DescuentosAdmin';
import CategoryManagement from './modules/categories/CategoryManagement';
import SubcategoryManagement from './modules/subcategorias/SubcategoryManagement';
import EmpresaAdmin from './modules/empresa/EmpresaAdmin';
import { EmpresaContext, empresaDesdeApi } from './config/EmpresaContext';
import { EMPRESA_POR_DEFECTO } from './config/empresa';
import StoreHome from './modules/store/StoreHome';
import CatalogPage from './modules/store/CatalogPage';
import CategoryPage from './modules/store/CategoryPage';
import ProductDetail from './modules/store/ProductDetail';
import { coincideBusqueda } from './modules/store/productoUtils';
import Footer from './components/Footer';
import WhatsAppButton from './components/WhatsAppButton';
import CartDrawer from './components/CartDrawer';
import WishlistDrawer from './components/WishlistDrawer';
import SearchOverlay from './components/SearchOverlay';
import {
  listarProductos,
  listarCategorias,
  guardarProducto,
  actualizarProducto,
  eliminarProducto,
  registrarCliente,
  iniciarSesion,
  cerrarSesion,
  obtenerToken,
  registrarManejador401,
  enviarPedidoCompleto,
  listarDescuentos,
  listarSubcategorias,
  obtenerEmpresa,
  obtenerConfiguracion
} from './api';

// Mapeo de id_rol (tabla `roles`) a nombre de rol usado en la interfaz
const NOMBRE_ROL = { 1: 'Administrador', 2: 'Cliente', 3: 'Vendedor' };

// Claves de localStorage para que la sesión y el carrito de invitado sobrevivan al refrescar la página
const CLAVE_USUARIO = 'tiendaSport_usuario';
const CLAVE_CARRITO = 'tiendaSport_carrito';

function leerUsuarioGuardado() {
  try {
    const crudo = localStorage.getItem(CLAVE_USUARIO);
    return crudo ? JSON.parse(crudo) : null;
  } catch (e) {
    return null;
  }
}

// Cada usuario tiene su propio carrito guardado (tiendaSport_carrito_<id>);
// quien no ha iniciado sesión usa el carrito de invitado (tiendaSport_carrito).
function claveCarritoDe(usuario) {
  return usuario && usuario.id_clientes ? `${CLAVE_CARRITO}_${usuario.id_clientes}` : CLAVE_CARRITO;
}

function leerCarrito(clave) {
  try {
    const crudo = localStorage.getItem(clave);
    return crudo ? JSON.parse(crudo) : [];
  } catch (e) {
    return [];
  }
}

// Lista de deseos: una por usuario (o de invitado), guardada en este navegador
const claveFavoritosDe = (usuario) =>
  usuario && usuario.id_clientes ? `tiendaSport_favoritos_${usuario.id_clientes}` : 'tiendaSport_favoritos';

function leerLista(clave) {
  try {
    const crudo = localStorage.getItem(clave);
    const lista = crudo ? JSON.parse(crudo) : [];
    return Array.isArray(lista) ? lista : [];
  } catch (e) {
    return [];
  }
}

function guardarLista(clave, lista) {
  try {
    localStorage.setItem(clave, JSON.stringify(lista));
  } catch (e) {
    // si el navegador no deja guardar, la lista sigue funcionando mientras la página esté abierta
  }
}

// Une el carrito de invitado con el del usuario al iniciar sesión:
// si el mismo producto (talla y color) está en los dos, suma las cantidades.
function fusionarCarritos(base, extra) {
  const resultado = [...base];
  extra.forEach(item => {
    const i = resultado.findIndex(
      x => x.idProducto === item.idProducto && x.talla === item.talla && x.color === item.color
    );
    if (i >= 0) {
      const max = resultado[i].stockDisponible || 99;
      resultado[i] = { ...resultado[i], cantidad: Math.min(resultado[i].cantidad + item.cantidad, max) };
    } else {
      resultado.push(item);
    }
  });
  return resultado;
}

/**
 * Componente Principal: App
 * Administra el flujo de vistas, sesión de usuario y estado global de productos de Tienda Sport.
 * Todo el catálogo, el carrito y los pedidos quedan conectados al backend (MySQL) mediante api.js.
 *
 * El carrito es de "invitado": cualquiera puede agregar productos sin iniciar sesión,
 * y se guarda en localStorage para que no se pierda al refrescar. Solo al confirmar
 * el pedido (checkout) se exige tener sesión iniciada.
 */
function App() {
  // Sesión: se restaura de localStorage si ya existía un login previo (mientras el token siga vigente)
  const [user, setUser] = useState(() => (obtenerToken() ? leerUsuarioGuardado() : null));

  // Vista actual: 'tienda_publica', 'login', 'registro', 'inventario', 'crear', 'carrito', 'usuarios', 'pedidos', 'categorias'
  const [currentView, setCurrentView] = useState('tienda_publica');

  // Catálogo real, traído desde MySQL a través de la API
  const [products, setProducts] = useState([]);
  const [categorias, setCategorias] = useState([]);
  const [escalas, setEscalas] = useState([]);       // escala de descuentos mayoristas (pública)
  const [montoMinimo, setMontoMinimo] = useState(0);  // monto mínimo de compra (pública)
  const [cargandoCatalogo, setCargandoCatalogo] = useState(true);
  const [errorCatalogo, setErrorCatalogo] = useState('');
  const [subcategorias, setSubcategorias] = useState([]); // tipos dentro de cada categoría (públicos)

  // Datos de la empresa (WhatsApp, correo, dirección...): los edita el Administrador y se guardan en la base de datos
  const [empresa, setEmpresa] = useState(EMPRESA_POR_DEFECTO);
  const cargarEmpresa = useCallback(async () => {
    try {
      setEmpresa(empresaDesdeApi(await obtenerEmpresa()));
    } catch (e) {
      // si el servidor no responde, se usan los datos por defecto
    }
  }, []);
  useEffect(() => {
    cargarEmpresa();
  }, [cargarEmpresa]);
  useEffect(() => {
    document.title = empresa.nombre;
  }, [empresa.nombre]);

  // Navegación de la tienda: categoría, tipo, producto abierto y texto buscado
  const [tienda, setTienda] = useState({ categoriaId: null, subcategoriaId: null, productoId: null, q: '', origen: null });

  // Paneles laterales y buscador
  const [carritoAbierto, setCarritoAbierto] = useState(false);
  const [favoritosAbierto, setFavoritosAbierto] = useState(false);
  const [busquedaAbierta, setBusquedaAbierta] = useState(false);

  // Lista de deseos (productos guardados para comprar después)
  const [favoritos, setFavoritos] = useState(() => leerLista(claveFavoritosDe(obtenerToken() ? leerUsuarioGuardado() : null)));
  useEffect(() => {
    setFavoritos(leerLista(claveFavoritosDe(user)));
  }, [user]);

  // Carrito de invitado: arranca con lo que haya quedado guardado en el navegador
  // Clave del carrito que está activo ahora (el del usuario con sesión, o el de invitado)
  const claveCarritoActual = useRef(claveCarritoDe(user));
  const [cart, setCart] = useState(() => leerCarrito(claveCarritoActual.current));

  // Cada vez que cambia el carrito, lo guardamos en la clave del usuario activo
  // para que sobreviva a un refresh y a cerrar sesión
  useEffect(() => {
    localStorage.setItem(claveCarritoActual.current, JSON.stringify(cart));
  }, [cart]);

  // ------------------------------------------------------------------
  // Sesión: cerrar sesión (declarado antes para poder registrarlo como
  // manejador del evento 401 más abajo)
  // ------------------------------------------------------------------
  const handleLogout = useCallback(() => {
    cerrarSesion(); // borra el token
    // DEF-04: el carrito del usuario queda guardado con su id; al salir se vuelve al de invitado
    claveCarritoActual.current = CLAVE_CARRITO;
    setCart(leerCarrito(CLAVE_CARRITO));
    setUser(null);
    localStorage.removeItem(CLAVE_USUARIO);
    setCurrentView('tienda_publica');
  }, []);

  // Si el backend rechaza el token (sesión vencida, o el servidor se
  // reinició y la perdió de su memoria), cerramos sesión automáticamente
  // en la interfaz y avisamos, en vez de dejar un error confuso en pantalla.
  useEffect(() => {
    registrarManejador401(() => {
      if (user) {
        alert('Tu sesión ya no es válida (probablemente el servidor se reinició). Por favor inicia sesión de nuevo.');
        handleLogout();
      }
    });
  }, [user, handleLogout]);

  // ------------------------------------------------------------------
  // Carga de datos reales desde el backend
  // ------------------------------------------------------------------
  const cargarCatalogo = useCallback(async () => {
    setCargandoCatalogo(true);
    setErrorCatalogo('');
    try {
      const [productosDb, categoriasDb, subcategoriasDb] = await Promise.all([
        listarProductos(),
        listarCategorias(),
        listarSubcategorias().catch(() => []) // si el servidor aún no tiene los tipos, la tienda sigue funcionando
      ]);

      const mapaCategorias = {};
      (categoriasDb || []).forEach(cat => { mapaCategorias[cat.id_categoria] = cat.nombre; });

      // Normalizamos cada producto agregando alias en camelCase (compatibles con
      // los componentes ya existentes) sin perder los campos originales de la API.
      const normalizados = (productosDb || []).map(p => ({
        ...p,
        idProducto: p.id_producto,
        costoProducto: p.costo_producto,
        precioMayorista: p.precio_mayorista,
        imagenUrl: p.imagen_url,
        imagenesPorColor: p.imagenes_por_color || '',
        idCategoria: p.id_categoria,
        genero: p.genero || 'Unisex',
        categoria: mapaCategorias[p.id_categoria] || ''
      }));

      setCategorias(categoriasDb || []);
      setSubcategorias(subcategoriasDb || []);
      setProducts(normalizados);
    } catch (error) {
      console.error(error);
      setErrorCatalogo('No se pudo conectar con el servidor API (http://localhost:8080). Verifica que ServidorApi esté corriendo, que MySQL esté encendido y que hayas ejecutado los scripts de migración.');
    } finally {
      setCargandoCatalogo(false);
    }
  }, []);

  useEffect(() => {
    cargarCatalogo();
  }, [cargarCatalogo]);

  // Descuentos y monto mínimo: los define el Administrador; se leen siempre del servidor
  const cargarReglasDeCompra = useCallback(async () => {
    try {
      const [escalasDb, config] = await Promise.all([listarDescuentos(), obtenerConfiguracion()]);
      setEscalas(escalasDb || []);
      setMontoMinimo(Number(config?.monto_minimo_compra) || 0);
    } catch (e) {
      // Si falla, la tienda sigue funcionando sin mostrar descuentos
    }
  }, []);

  useEffect(() => {
    cargarReglasDeCompra();
  }, [cargarReglasDeCompra]);

  // ------------------------------------------------------------------
  // Sesión: login / registro (contra la tabla `clientes`)
  // ------------------------------------------------------------------
  const handleLogin = async (email, password, role) => {
    try {
      const cliente = await iniciarSesion({ correo: email, password, rol: role });
      const usuarioSesion = {
        id_clientes: cliente.id_clientes,
        name: cliente.nombre,
        email: cliente.correo,
        role: NOMBRE_ROL[cliente.id_rol] || role
      };
      setUser(usuarioSesion);
      localStorage.setItem(CLAVE_USUARIO, JSON.stringify(usuarioSesion));

      // Cargamos el carrito guardado de este usuario y le sumamos lo que haya agregado como invitado
      const claveUsuario = claveCarritoDe(usuarioSesion);
      const carritoFusionado = fusionarCarritos(leerCarrito(claveUsuario), cart);
      localStorage.setItem(CLAVE_CARRITO, '[]'); // el carrito de invitado queda vacío
      claveCarritoActual.current = claveUsuario;
      setCart(carritoFusionado);

      if (usuarioSesion.role === 'Administrador') {
        setCurrentView('inventario');
      } else if (usuarioSesion.role === 'Vendedor') {
        setCurrentView('pedidos');
      } else {
        setCurrentView('tienda_publica');
      }
      return { success: true };
    } catch (error) {
      return { success: false, error: error.message || 'Credenciales inválidas.' };
    }
  };

  const handleRegister = async (newUser) => {
    // 1. Registrar el cliente en MySQL (el backend valida la complejidad de la contraseña)
    await registrarCliente({
      nombre: newUser.nombre,
      apellido: newUser.apellido,
      identificacion: newUser.identificacion,
      telefono: newUser.telefono,
      direccion: newUser.direccion,
      correo: newUser.correo,
      password: newUser.password,
      id_rol: newUser.idRol
    });

    // 2. Iniciar sesión automáticamente con las credenciales recién creadas
    const resultado = await handleLogin(newUser.correo, newUser.password, 'Cliente');
    if (!resultado.success) {
      // El registro sí quedó guardado; solo informamos que debe iniciar sesión manualmente
      setCurrentView('login');
      throw new Error('Cliente registrado. Por favor inicia sesión.');
    }
  };

  // ------------------------------------------------------------------
  // CRUD de productos (Administrador / Vendedor) contra la tabla `producto`
  // ------------------------------------------------------------------
  const handleAddProduct = async (newProduct) => {
    await guardarProducto({
      nombre: newProduct.nombre,
      descripcion: newProduct.descripcion,
      talla: newProduct.talla,
      color: newProduct.color,
      genero: newProduct.genero,
      precio_mayorista: newProduct.precioMayorista,
      costo_producto: newProduct.costoProducto,
      stock: newProduct.stock,
      imagen_url: newProduct.imagenUrl,
      imagenes_por_color: newProduct.imagenesPorColor,
      id_categoria: newProduct.idCategoria
    });
    await cargarCatalogo();
    setCurrentView('inventario');
    // Si guardarProducto falla, lanza un error y este bloque no sigue;
    // ProductForm es quien captura ese error, avisa y CONSERVA lo que el usuario escribió.
  };

  const handleUpdateProduct = async (id, updatedProduct) => {
    const productoActual = products.find(p => p.idProducto === id || p.id_producto === id);
    try {
      await actualizarProducto({
        id_producto: id,
        nombre: updatedProduct.nombre,
        descripcion: updatedProduct.descripcion,
        talla: updatedProduct.talla,
        color: updatedProduct.color,
        genero: updatedProduct.genero || (productoActual ? productoActual.genero : 'Unisex'),
        precio_mayorista: updatedProduct.precioMayorista ?? updatedProduct.precio_mayorista,
        costo_producto: updatedProduct.costoProducto ?? updatedProduct.costo,
        stock: updatedProduct.stock,
        imagen_url: updatedProduct.imagenUrl ?? updatedProduct.imagen,
        imagenes_por_color: updatedProduct.imagenesPorColor ?? (productoActual ? productoActual.imagenesPorColor : ''),
        // ProductList no permite cambiar la categoría; conservamos la actual
        id_categoria: productoActual ? productoActual.idCategoria : 1
      });
      await cargarCatalogo();
    } catch (error) {
      alert(`No se pudo actualizar el producto: ${error.message}`);
    }
  };

  const handleDeleteProduct = async (id) => {
    try {
      await eliminarProducto(id);
      await cargarCatalogo();
    } catch (error) {
      alert(`No se pudo eliminar el producto: ${error.message}`);
    }
  };

  // ------------------------------------------------------------------
  // Carrito de invitado y creación de pedidos (tablas `pedido` y `detalle_pedido`)
  // ------------------------------------------------------------------
  const handleAddToCart = (item) => {
    setCart(prevCart => {
      const indiceExistente = prevCart.findIndex(
        i => i.idProducto === item.idProducto && i.talla === item.talla && i.color === item.color
      );
      if (indiceExistente >= 0) {
        const copia = [...prevCart];
        copia[indiceExistente] = {
          ...copia[indiceExistente],
          cantidad: copia[indiceExistente].cantidad + item.cantidad
        };
        return copia;
      }
      return [...prevCart, item];
    });
  };

  const handleUpdateCartQuantity = (index, cantidad) => {
    setCart(prevCart => {
      const copia = [...prevCart];
      const max = copia[index].stockDisponible || 99;
      copia[index] = { ...copia[index], cantidad: Math.max(1, Math.min(cantidad, max)) };
      return copia;
    });
  };

  const handleRemoveFromCart = (index) => {
    setCart(prevCart => prevCart.filter((_, i) => i !== index));
  };

  const handleCheckout = async () => {
    if (!user) {
      // El componente CartView es quien redirige a Login/Registro; esto es un respaldo.
      throw new Error('Debes iniciar sesión o registrarte para confirmar el pedido.');
    }
    const items = cart.map(item => ({
      id_producto: item.idProducto,
      talla: item.talla,
      color: item.color,
      cantidad: item.cantidad
      // Sin precio: el servidor lo lee de la base de datos y calcula el total
    }));
    // El backend toma el id_cliente del token de sesión, no hace falta enviarlo
    const pedido = await enviarPedidoCompleto(items);
    setCart([]);
    // Refrescamos el catálogo para reflejar el stock actualizado tras la compra
    await cargarCatalogo();
    return pedido;
  };

  const cartCount = cart.reduce((acc, item) => acc + item.cantidad, 0);

  // ------------------------------------------------------------------
  // Navegación de la tienda y lista de deseos
  // ------------------------------------------------------------------
  const irInicio = () => {
    setTienda({ categoriaId: null, subcategoriaId: null, productoId: null, q: '', origen: null });
    setCurrentView('tienda_publica');
  };
  const irCatalogo = () => {
    setTienda(t => ({ ...t, categoriaId: null, subcategoriaId: null }));
    setCurrentView('catalogo');
  };
  const irCategoria = (id) => {
    setTienda(t => ({ ...t, categoriaId: id, subcategoriaId: null }));
    setCurrentView('categoria');
  };
  const irTipo = (valor) => setTienda(t => ({ ...t, subcategoriaId: valor }));
  const irBusqueda = (q) => {
    setTienda(t => ({ ...t, q }));
    setCurrentView('busqueda');
  };
  const irProducto = (id) => {
    setTienda(t => ({
      ...t,
      productoId: id,
      // recordamos desde dónde se abrió para el botón "Volver" (si ya estábamos en un producto, se conserva el origen)
      origen: currentView === 'producto' ? t.origen : { view: currentView, categoriaId: t.categoriaId, subcategoriaId: t.subcategoriaId, q: t.q }
    }));
    setCurrentView('producto');
  };
  const volverDeProducto = () => {
    const o = tienda.origen;
    if (o && o.view !== 'producto') {
      setTienda(t => ({ ...t, categoriaId: o.categoriaId, subcategoriaId: o.subcategoriaId, q: o.q }));
      setCurrentView(o.view);
    } else {
      irInicio();
    }
  };
  const alternarFavorito = (id) => {
    setFavoritos(prev => {
      const siguiente = prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id];
      guardarLista(claveFavoritosDe(user), siguiente);
      return siguiente;
    });
  };
  const agregarYAbrirCarrito = (item) => {
    handleAddToCart(item);
    setCarritoAbierto(true);
  };
  const finalizarCompra = () => {
    setCarritoAbierto(false);
    setCurrentView('carrito');
  };

  const productosFavoritos = products.filter(p => favoritos.includes(p.idProducto));
  const productoActual = products.find(p => p.idProducto === tienda.productoId);
  const categoriaActual = categorias.find(c => c.id_categoria === tienda.categoriaId);
  const resultadosBusqueda = tienda.q ? products.filter(p => coincideBusqueda(p, tienda.q)) : [];

  // Al cambiar de página dentro de la tienda, volvemos arriba
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [currentView, tienda.categoriaId, tienda.subcategoriaId, tienda.productoId, tienda.q]);

  // Propiedades que comparten las páginas con tarjetas de producto
  const propsTarjetas = {
    favoritos,
    onToggleFavorito: alternarFavorito,
    onAbrir: irProducto,
    escalas
  };

  return (
    <EmpresaContext.Provider value={empresa}>
    <div className="app-container" style={{ minHeight: '100vh', backgroundColor: '#faf8f5' }}>
      {/* Franja de promociones + encabezado fijo con buscador, favoritos, cuenta, carrito y categorías */}
      <Navbar
        user={user}
        onLogout={handleLogout}
        setView={setCurrentView}
        categorias={categorias}
        escalas={escalas}
        cartCount={cartCount}
        wishCount={favoritos.length}
        onInicio={irInicio}
        onCategoria={irCategoria}
        onAbrirBusqueda={() => setBusquedaAbierta(true)}
        onAbrirCarrito={() => setCarritoAbierto(true)}
        onAbrirFavoritos={() => setFavoritosAbierto(true)}
      />

      {/* RENDERIZADO CONDICIONAL DE VISTAS */}
      <main className="main-content" style={{ maxWidth: '1280px', margin: '0 auto', padding: '20px' }}>

        {errorCatalogo && (
          <div style={{ backgroundColor: '#ffe6e6', color: '#b21f1f', padding: '12px 16px', borderRadius: '6px', marginBottom: '15px' }}>
            ⚠️ {errorCatalogo}
          </div>
        )}

        {/* Vista: Inicio de Sesión */}
        {currentView === 'login' && (
          <LoginCard
            onLogin={handleLogin}
            onCancel={() => setCurrentView('tienda_publica')}
            onGoToRegister={() => setCurrentView('registro')}
          />
        )}

        {/* Vista: Registro de Cliente */}
        {currentView === 'registro' && (
          <RegisterCard
            onRegister={handleRegister}
            onCancel={() => setCurrentView('tienda_publica')}
            onGoToLogin={() => setCurrentView('login')}
          />
        )}

        {/* Vistas de la tienda (accesibles para invitados) */}
        {['tienda_publica', 'catalogo', 'categoria', 'busqueda', 'producto'].includes(currentView) && cargandoCatalogo && (
          <p style={{ textAlign: 'center', marginTop: '40px' }}>Cargando catálogo...</p>
        )}

        {!cargandoCatalogo && currentView === 'tienda_publica' && (
          <StoreHome productos={products} onVerCatalogo={irCatalogo} {...propsTarjetas} />
        )}

        {!cargandoCatalogo && currentView === 'catalogo' && (
          <CatalogPage
            titulo="Todo el catálogo"
            productos={products}
            vacio="Pronto agregaremos productos a la tienda."
            {...propsTarjetas}
          />
        )}

        {!cargandoCatalogo && currentView === 'categoria' && (
          <CategoryPage
            categoria={categoriaActual}
            subcategorias={subcategorias}
            productos={products}
            subcategoriaId={tienda.subcategoriaId}
            onTipo={irTipo}
            onVolverACategoria={() => irCategoria(tienda.categoriaId)}
            onInicio={irInicio}
            {...propsTarjetas}
          />
        )}

        {!cargandoCatalogo && currentView === 'busqueda' && (
          <CatalogPage
            titulo={`Resultados para “${tienda.q}”`}
            productos={resultadosBusqueda}
            vacio="No encontramos productos con esa búsqueda. Prueba con otra palabra."
            {...propsTarjetas}
          />
        )}

        {!cargandoCatalogo && currentView === 'producto' && (
          productoActual ? (
            <ProductDetail
              key={productoActual.idProducto}
              producto={productoActual}
              categorias={categorias}
              productos={products}
              onAgregarAlCarrito={agregarYAbrirCarrito}
              onVolver={volverDeProducto}
              {...propsTarjetas}
            />
          ) : (
            <p style={{ textAlign: 'center', marginTop: '40px' }}>Ese producto ya no está disponible.</p>
          )
        )}

        {/* Vista: Carrito de Compras (de invitado; pide login solo al confirmar) */}
        {currentView === 'carrito' && (
          <CartView
            cart={cart}
            user={user}
            escalas={escalas}
            montoMinimo={montoMinimo}
            onUpdateQuantity={handleUpdateCartQuantity}
            onRemoveItem={handleRemoveFromCart}
            onCheckout={handleCheckout}
            onGoToStore={() => setCurrentView('tienda_publica')}
            onGoToLogin={() => setCurrentView('login')}
            onGoToRegister={() => setCurrentView('registro')}
          />
        )}

        {/* Vista: Control de Inventario y Valoración (solo Administrador) */}
        {currentView === 'inventario' && user?.role === 'Administrador' && (
          <>
            {user?.role === 'Administrador' && <AdminDashboard products={products} />}
            <ProductList
              products={products}
              onUpdateProduct={handleUpdateProduct}
              onDeleteProduct={handleDeleteProduct}
            />
          </>
        )}

        {/* Vista: Registro de Nuevo Producto (solo Administrador) */}
        {currentView === 'crear' && user?.role === 'Administrador' && (
          <ProductForm onAddProduct={handleAddProduct} categorias={categorias} />
        )}

        {/* Vista: Gestión de Usuarios (solo Administrador) */}
        {currentView === 'usuarios' && user?.role === 'Administrador' && (
          <UserManagement />
        )}

        {/* Vista: Gestión de Categorías (solo Administrador) */}
        {currentView === 'categorias' && user?.role === 'Administrador' && (
          <CategoryManagement onCategoriasChange={cargarCatalogo} />
        )}

        {/* Vista: Pedidos — Administrador/Vendedor ven todos, Cliente ve solo los suyos */}
        {currentView === 'pedidos' && user && (
          user.role === 'Cliente'
            ? <MisPedidos onIrATienda={irInicio} />
            : <PedidosStaff esAdministrador={user.role === 'Administrador'} />
        )}

        {/* Vista: Descuentos y compra mínima — solo Administrador */}
        {currentView === 'descuentos' && user?.role === 'Administrador' && (
          <DescuentosAdmin onGuardado={cargarReglasDeCompra} />
        )}

        {/* Vista: Tipos de prenda — solo Administrador */}
        {currentView === 'tipos' && user?.role === 'Administrador' && (
          <SubcategoryManagement categorias={categorias} products={products} onCambio={cargarCatalogo} />
        )}

        {/* Vista: Datos de la empresa — solo Administrador */}
        {currentView === 'empresa' && user?.role === 'Administrador' && (
          <EmpresaAdmin onGuardado={cargarEmpresa} />
        )}

        {/* Vista: Historial de actividad — solo Administrador */}
        {currentView === 'actividad' && user?.role === 'Administrador' && <ActivityLog />}

      </main>

      <Footer categorias={categorias} escalas={escalas} user={user} onCategoria={irCategoria} setView={setCurrentView} />
      <WhatsAppButton />

      {/* Paneles laterales y buscador */}
      <CartDrawer
        abierto={carritoAbierto}
        onCerrar={() => setCarritoAbierto(false)}
        cart={cart}
        escalas={escalas}
        onUpdateQuantity={handleUpdateCartQuantity}
        onRemoveItem={handleRemoveFromCart}
        onFinalizar={finalizarCompra}
      />
      <WishlistDrawer
        abierto={favoritosAbierto}
        onCerrar={() => setFavoritosAbierto(false)}
        productos={productosFavoritos}
        onAbrir={irProducto}
        onQuitar={alternarFavorito}
      />
      <SearchOverlay
        abierto={busquedaAbierta}
        onCerrar={() => setBusquedaAbierta(false)}
        productos={products}
        onAbrirProducto={irProducto}
        onBuscar={irBusqueda}
      />
    </div>
    </EmpresaContext.Provider>
  );
}

export default App;
