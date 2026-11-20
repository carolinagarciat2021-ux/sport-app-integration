import React, { useState, useEffect, useCallback } from 'react';
import {
  listarSubcategorias, crearSubcategoria, actualizarSubcategoria,
  eliminarSubcategoria, asignarProductosSubcategoria,
} from '../../api';
import ImagenProducto from '../store/ImagenProducto';

const CARBON = '#1f1d1b';

/**
 * Administrador: crea los "tipos" de cada categoría (ej.: en "Faldas Deportivas": Falda short, Falda larga),
 * les pone una imagen y decide qué productos pertenecen a cada tipo.
 */
const SubcategoryManagement = ({ categorias, products, onCambio }) => {
  const [tipos, setTipos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [mensaje, setMensaje] = useState(null);
  const [nuevo, setNuevo] = useState({ nombre: '', id_categoria: '', imagen_url: '' });
  const [editando, setEditando] = useState(null);   // { id, nombre, imagen_url }
  const [abierto, setAbierto] = useState(null);     // id del tipo con panel de productos abierto
  const [seleccion, setSeleccion] = useState([]);   // ids de productos marcados

  const cargar = useCallback(async () => {
    try {
      setTipos((await listarSubcategorias()) || []);
    } catch (err) {
      setMensaje({ tipo: 'error', texto: err.message || 'No se pudieron cargar los tipos.' });
    } finally {
      setCargando(false);
    }
  }, []);

  useEffect(() => { cargar(); }, [cargar]);

  const avisar = (tipo, texto) => setMensaje({ tipo, texto });

  const crear = async () => {
    setMensaje(null);
    try {
      await crearSubcategoria({ nombre: nuevo.nombre, id_categoria: Number(nuevo.id_categoria), imagen_url: nuevo.imagen_url });
      setNuevo({ nombre: '', id_categoria: nuevo.id_categoria, imagen_url: '' });
      avisar('ok', 'Tipo creado.');
      await cargar();
      if (onCambio) onCambio();
    } catch (err) { avisar('error', err.message); }
  };

  const guardarEdicion = async () => {
    try {
      await actualizarSubcategoria(editando.id, { nombre: editando.nombre, imagen_url: editando.imagen_url });
      setEditando(null);
      avisar('ok', 'Tipo actualizado.');
      await cargar();
      if (onCambio) onCambio();
    } catch (err) { avisar('error', err.message); }
  };

  const borrar = async (t) => {
    if (!window.confirm(`¿Eliminar el tipo "${t.nombre}"? Los productos NO se borran, solo quedan sin tipo.`)) return;
    try {
      await eliminarSubcategoria(t.id_subcategoria);
      avisar('ok', 'Tipo eliminado.');
      if (abierto === t.id_subcategoria) setAbierto(null);
      await cargar();
      if (onCambio) onCambio();
    } catch (err) { avisar('error', err.message); }
  };

  const abrirProductos = (t) => {
    if (abierto === t.id_subcategoria) { setAbierto(null); return; }
    setAbierto(t.id_subcategoria);
    setSeleccion(products.filter((p) => p.id_subcategoria === t.id_subcategoria).map((p) => p.idProducto));
  };

  const alternar = (id) => setSeleccion(seleccion.includes(id) ? seleccion.filter((x) => x !== id) : [...seleccion, id]);

  const guardarProductos = async (t) => {
    try {
      await asignarProductosSubcategoria(t.id_subcategoria, seleccion);
      avisar('ok', 'Productos del tipo guardados.');
      setAbierto(null);
      if (onCambio) onCambio();
    } catch (err) { avisar('error', err.message); }
  };

  const entrada = { padding: '10px', border: '1px solid #d9d2c3', boxSizing: 'border-box', fontSize: '1rem', width: '100%' };
  const boton = { padding: '10px 16px', border: 'none', backgroundColor: CARBON, color: '#fff', cursor: 'pointer' };
  const botonLinea = { padding: '8px 12px', border: `1px solid ${CARBON}`, backgroundColor: '#fff', color: CARBON, cursor: 'pointer' };

  return (
    <div style={{ marginTop: '20px', maxWidth: '860px' }}>
      <h2 style={{ color: CARBON }}>Tipos de prenda</h2>
      <p style={{ color: '#666' }}>
        Crea tipos dentro de cada categoría (por ejemplo, en Faldas: “Falda short” y “Falda larga”). En la tienda, al elegir la categoría
        se muestran los tipos con su imagen y luego sus productos. Si una categoría no tiene tipos, se muestran directamente sus productos.
      </p>

      <div style={{ backgroundColor: '#fff', border: '1px solid #e6dfd2', padding: '18px', marginBottom: '20px' }}>
        <h3 style={{ marginTop: 0 }}>Nuevo tipo</h3>
        <div style={{ display: 'grid', gap: '10px', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))' }}>
          <label>Categoría
            <select value={nuevo.id_categoria} onChange={(e) => setNuevo({ ...nuevo, id_categoria: e.target.value })} style={entrada}>
              <option value="">Elige una categoría</option>
              {categorias.map((c) => <option key={c.id_categoria} value={c.id_categoria}>{c.nombre}</option>)}
            </select>
          </label>
          <label>Nombre del tipo
            <input value={nuevo.nombre} onChange={(e) => setNuevo({ ...nuevo, nombre: e.target.value })} placeholder="Ej.: Falda short" style={entrada} />
          </label>
          <label>Imagen (dirección web, opcional)
            <input value={nuevo.imagen_url} onChange={(e) => setNuevo({ ...nuevo, imagen_url: e.target.value })} placeholder="https://..." style={entrada} />
          </label>
        </div>
        <button type="button" onClick={crear} disabled={!nuevo.nombre.trim() || !nuevo.id_categoria} style={{ ...boton, marginTop: '12px', opacity: (!nuevo.nombre.trim() || !nuevo.id_categoria) ? 0.5 : 1 }}>
          Crear tipo
        </button>
      </div>

      {mensaje && (
        <p style={{ color: mensaje.tipo === 'ok' ? '#2f6b3f' : '#b21f1f' }}>{mensaje.tipo === 'ok' ? '✓ ' : '⚠️ '}{mensaje.texto}</p>
      )}
      {cargando && <p>Cargando...</p>}

      {categorias.map((cat) => {
        const deCategoria = tipos.filter((t) => t.id_categoria === cat.id_categoria);
        if (deCategoria.length === 0) return null;
        return (
          <div key={cat.id_categoria} style={{ marginBottom: '22px' }}>
            <h3 style={{ color: CARBON, borderBottom: '1px solid #e6dfd2', paddingBottom: '6px' }}>{cat.nombre}</h3>
            {deCategoria.map((t) => {
              const productosCat = products.filter((p) => p.idCategoria === cat.id_categoria);
              return (
                <div key={t.id_subcategoria} style={{ backgroundColor: '#fff', border: '1px solid #e6dfd2', padding: '12px', marginBottom: '10px' }}>
                  <div style={{ display: 'flex', gap: '12px', alignItems: 'center', flexWrap: 'wrap' }}>
                    <div style={{ width: 56, height: 56, flexShrink: 0 }}>
                      <ImagenProducto src={t.imagen_url} alt={t.nombre} className="miniatura-img" />
                    </div>
                    {editando && editando.id === t.id_subcategoria ? (
                      <div style={{ display: 'grid', gap: '8px', flex: 1, minWidth: '220px' }}>
                        <input value={editando.nombre} onChange={(e) => setEditando({ ...editando, nombre: e.target.value })} style={entrada} />
                        <input value={editando.imagen_url || ''} onChange={(e) => setEditando({ ...editando, imagen_url: e.target.value })} placeholder="Dirección de la imagen" style={entrada} />
                        <div style={{ display: 'flex', gap: '8px' }}>
                          <button type="button" style={boton} onClick={guardarEdicion}>Guardar</button>
                          <button type="button" style={botonLinea} onClick={() => setEditando(null)}>Cancelar</button>
                        </div>
                      </div>
                    ) : (
                      <>
                        <strong style={{ flex: 1, minWidth: '140px' }}>{t.nombre}</strong>
                        <button type="button" style={botonLinea} onClick={() => abrirProductos(t)}>
                          {abierto === t.id_subcategoria ? 'Cerrar productos' : 'Productos del tipo'}
                        </button>
                        <button type="button" style={botonLinea} onClick={() => setEditando({ id: t.id_subcategoria, nombre: t.nombre, imagen_url: t.imagen_url })}>Editar</button>
                        <button type="button" style={{ ...botonLinea, color: '#8c2f2f', borderColor: '#c9b8b8' }} onClick={() => borrar(t)}>Eliminar</button>
                      </>
                    )}
                  </div>

                  {abierto === t.id_subcategoria && (
                    <div style={{ marginTop: '12px', borderTop: '1px solid #eee', paddingTop: '10px' }}>
                      {productosCat.length === 0 ? (
                        <p style={{ color: '#888' }}>Esta categoría todavía no tiene productos.</p>
                      ) : (
                        <>
                          <p style={{ margin: '0 0 8px', color: '#555' }}>Marca los productos que son de este tipo:</p>
                          <div style={{ display: 'grid', gap: '6px', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>
                            {productosCat.map((p) => (
                              <label key={p.idProducto} style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                                <input type="checkbox" checked={seleccion.includes(p.idProducto)} onChange={() => alternar(p.idProducto)} />
                                <span>{p.nombre}</span>
                              </label>
                            ))}
                          </div>
                          <button type="button" style={{ ...boton, marginTop: '12px' }} onClick={() => guardarProductos(t)}>Guardar productos</button>
                        </>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        );
      })}
    </div>
  );
};

export default SubcategoryManagement;
