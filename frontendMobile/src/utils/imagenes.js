// Reduce una foto del computador antes de subirla: máximo 1000 px por lado y JPG.
// Así pesa poco (casi siempre menos de 300 KB), carga rápido en el celular y cabe en el servidor.
export const reducirImagen = (archivo, maxLado = 1000, calidad = 0.85) =>
  new Promise((resolve, reject) => {
    if (!archivo || !String(archivo.type).startsWith('image/')) {
      reject(new Error('El archivo elegido no es una imagen.'));
      return;
    }
    const url = URL.createObjectURL(archivo);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      const escala = Math.min(1, maxLado / Math.max(img.width, img.height));
      const ancho = Math.max(1, Math.round(img.width * escala));
      const alto = Math.max(1, Math.round(img.height * escala));
      const lienzo = document.createElement('canvas');
      lienzo.width = ancho;
      lienzo.height = alto;
      const ctx = lienzo.getContext('2d');
      ctx.fillStyle = '#ffffff'; // las PNG con fondo transparente quedan sobre blanco
      ctx.fillRect(0, 0, ancho, alto);
      ctx.drawImage(img, 0, 0, ancho, alto);
      lienzo.toBlob(
        (blob) => (blob
          ? resolve(new File([blob], 'foto.jpg', { type: 'image/jpeg' }))
          : reject(new Error('No se pudo procesar la imagen.'))),
        'image/jpeg',
        calidad
      );
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('No se pudo leer la imagen. Prueba con un archivo JPG, PNG o WEBP.'));
    };
    img.src = url;
  });
