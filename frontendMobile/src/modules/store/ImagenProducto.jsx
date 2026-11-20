import React, { useState, useEffect } from 'react';
import { urlImagen } from '../../api';

/** Foto de producto. Si no hay foto o no carga, muestra un recuadro elegante en vez de una imagen rota. */
const ImagenProducto = ({ src, respaldo = '', alt = '', className = '' }) => {
  const [fallo, setFallo] = useState(false);
  const [usandoRespaldo, setUsandoRespaldo] = useState(false);
  useEffect(() => { setFallo(false); setUsandoRespaldo(false); }, [src, respaldo]);

  // Si la foto del color no carga, se prueba con la foto general del producto antes de rendirse
  const actual = usandoRespaldo ? respaldo : (src || respaldo);
  const alFallar = () => {
    if (!usandoRespaldo && respaldo && actual !== respaldo) setUsandoRespaldo(true);
    else setFallo(true);
  };

  if (!actual || fallo) {
    return (
      <div className={`img-vacia ${className}`} role="img" aria-label={alt}>
        <span>Sin imagen</span>
      </div>
    );
  }
  return <img className={className} src={urlImagen(actual)} alt={alt} loading="lazy" onError={alFallar} />;
};

export default ImagenProducto;
