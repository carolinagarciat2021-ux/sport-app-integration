import React, { useState, useEffect, useRef } from 'react';
import AnnouncementBar from './AnnouncementBar';
import { IconSearch, IconHeart, IconUser, IconBag, IconMenu, IconClose } from './Icons';
import { useEmpresa } from '../config/EmpresaContext';

/**
 * Encabezado de la tienda: franja de promociones, logo, buscador, favoritos, cuenta y carrito,
 * y debajo el menú de categorías. El encabezado se queda fijo al desplazar la página.
 */
const Navbar = ({
  user, onLogout, setView, categorias = [], escalas = [],
  cartCount = 0, wishCount = 0,
  onInicio, onCategoria, onAbrirBusqueda, onAbrirCarrito, onAbrirFavoritos,
}) => {
  const EMPRESA = useEmpresa();
  const [cuentaAbierta, setCuentaAbierta] = useState(false);
  const [menuMovil, setMenuMovil] = useState(false);
  const cuentaRef = useRef(null);

  // Cierra el menú de cuenta al tocar fuera o con Escape
  useEffect(() => {
    const cerrar = (e) => {
      if (e.type === 'keydown' && e.key !== 'Escape') return;
      if (e.type === 'mousedown' && cuentaRef.current && cuentaRef.current.contains(e.target)) return;
      setCuentaAbierta(false);
    };
    document.addEventListener('mousedown', cerrar);
    document.addEventListener('keydown', cerrar);
    return () => {
      document.removeEventListener('mousedown', cerrar);
      document.removeEventListener('keydown', cerrar);
    };
  }, []);

  const esAdmin = user?.role === 'Administrador';

  const ir = (vista) => { setView(vista); setCuentaAbierta(false); setMenuMovil(false); };
  const irCategoria = (id) => { onCategoria(id); setMenuMovil(false); };

  return (
    <>
      <AnnouncementBar escalas={escalas} />
      <header className="hd">
        <div className="hd-fila">
          <button type="button" className="hd-menu-btn" onClick={() => setMenuMovil(!menuMovil)} aria-label="Abrir o cerrar el menú" aria-expanded={menuMovil}>
            {menuMovil ? <IconClose /> : <IconMenu />}
          </button>

          <button type="button" className="hd-logo" onClick={() => { onInicio(); setMenuMovil(false); }} aria-label="Ir al inicio">
            <span className="hd-logo-nombre">{EMPRESA.nombre}</span>
          </button>

          <div className="hd-iconos">
            <button type="button" className="hd-icono" onClick={onAbrirBusqueda} aria-label="Buscar productos"><IconSearch /></button>

            <button type="button" className="hd-icono" onClick={onAbrirFavoritos} aria-label="Lista de deseos">
              <IconHeart />
              {wishCount > 0 && <span className="hd-badge">{wishCount}</span>}
            </button>

            <div className="hd-cuenta" ref={cuentaRef}>
              <button type="button" className="hd-icono" onClick={() => setCuentaAbierta(!cuentaAbierta)} aria-label="Mi cuenta" aria-expanded={cuentaAbierta}>
                <IconUser />
              </button>
              {cuentaAbierta && (
                <div className="hd-cuenta-menu" role="menu">
                  {user ? (
                    <>
                      <div className="hd-cuenta-saludo">
                        <strong>{user.name}</strong>
                        <span>{user.role}</span>
                      </div>
                      <button type="button" onClick={() => ir('pedidos')}>{user.role === 'Cliente' ? 'Mis pedidos' : 'Pedidos'}</button>
                      {esAdmin && (
                        <>
                          <div className="hd-cuenta-titulo">Administración</div>
                          <button type="button" onClick={() => ir('inventario')}>Inventario</button>
                          <button type="button" onClick={() => ir('crear')}>Registrar producto</button>
                          <button type="button" onClick={() => ir('categorias')}>Categorías</button>
                          <button type="button" onClick={() => ir('tipos')}>Tipos de prenda</button>
                          <button type="button" onClick={() => ir('usuarios')}>Usuarios</button>
                          <button type="button" onClick={() => ir('descuentos')}>Descuentos y compra mínima</button>
                          <button type="button" onClick={() => ir('empresa')}>Datos de la empresa</button>
                          <button type="button" onClick={() => ir('actividad')}>Historial de actividad</button>
                        </>
                      )}
                      <button type="button" className="hd-salir" onClick={() => { setCuentaAbierta(false); onLogout(); }}>Cerrar sesión</button>
                    </>
                  ) : (
                    <>
                      <button type="button" onClick={() => ir('login')}>Ingresar</button>
                      <button type="button" onClick={() => ir('registro')}>Crear cuenta</button>
                    </>
                  )}
                </div>
              )}
            </div>

            <button type="button" className="hd-icono" onClick={onAbrirCarrito} aria-label={`Carrito, ${cartCount} productos`}>
              <IconBag />
              {cartCount > 0 && <span className="hd-badge">{cartCount}</span>}
            </button>
          </div>
        </div>

        <nav className={`hd-nav ${menuMovil ? 'abierto' : ''}`} aria-label="Categorías">
          {categorias.map((c) => (
            <button key={c.id_categoria} type="button" className="hd-nav-item" onClick={() => irCategoria(c.id_categoria)}>
              {c.nombre}
            </button>
          ))}
        </nav>
      </header>
    </>
  );
};

export default Navbar;
