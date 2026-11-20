# Tienda Sport — Backend (Spring Boot)

Backend migrado de Java plano a **Spring Boot + Spring Security + JWT**, pensado
para desplegarse en la nube (Railway o Render) con HTTPS real.

## Variables de entorno necesarias en producción

| Variable | Para qué sirve | Ejemplo |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL JDBC de MySQL (Aiven) | `jdbc:mysql://HOST:PUERTO/BD?sslMode=REQUIRED` |
| `SPRING_DATASOURCE_USERNAME` | Usuario de MySQL | `avnadmin` |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña de MySQL | *(la de Aiven)* |
| `JWT_SECRET` | **OBLIGATORIA, sin valor por defecto**: si falta o tiene menos de 32 caracteres la app no arranca | *(clave aleatoria larga)* |
| `JWT_EXPIRATION_MS` | Duración del token en milisegundos (opcional) | `14400000` (4 horas) |
| `ALLOWED_ORIGINS` | URL(s) de tu frontend publicado, separadas por coma | `https://tiendasport.vercel.app` |
| `PORT` | Railway/Render la asignan automáticamente — no la definas tú | *(automática)* |

En tu computador, para desarrollo local solo necesitas definir `JWT_SECRET`
(ver `src/main/resources/application.properties.example`); lo demás tiene valores
por defecto para la base local y `http://localhost:3000`.

## Base de datos

Los cambios de tablas ya no los hace Hibernate (`ddl-auto=validate`). Se aplican a mano
con los scripts de la carpeta `db/`, en este orden: `00_respaldo.sql`, `01_verificar.sql`,
`V1__seguridad_pedidos.sql`, `V2__registro_actividad.sql`, `V3__descuentos_mayoristas.sql`.

## Descuentos mayoristas

Escala en la tabla `descuento_escala` (la edita el Administrador desde la web: `GET /api/descuentos` público, `PUT /api/descuentos` solo Administrador). Al confirmar la compra el servidor aplica el escalón más alto que alcance el subtotal y guarda `subtotal`, `descuento_porcentaje`, `descuento_valor` y `total` en el pedido. El monto mínimo de compra (`PUT /api/configuracion`) se valida sobre el subtotal.

## Historial de actividad

Tabla `registro_actividad`: guarda quién hizo qué (login, compras, cambios y eliminación de pedidos, productos y usuarios).
Solo el Administrador lo consulta (`GET /api/actividad`); no hay endpoints para editarlo ni borrarlo. Nunca guarda contraseñas ni tokens.

## Reglas de pedidos

- La compra se confirma con `POST /api/pedidos/completo` (el servidor calcula precios y total).
- Un pedido confirmado no se modifica. Solo el Administrador puede anularlo (`Cancelado`) o eliminarlo (`DELETE /api/pedidos/{id}`); ambas devuelven el stock.
- Solo Administrador y Vendedor cambian el estado; solo el Administrador anula (`Cancelado`) y eso devuelve el stock.

## Cómo correr el proyecto localmente

```bash
mvn spring-boot:run
```

## Cómo construir el .jar manualmente

```bash
mvn clean package
java -jar target/tiendasport-backend.jar
```

## Despliegue

Ver la guía paso a paso que te dio Claude para Railway/Render — este proyecto
ya incluye el `Dockerfile` necesario para ambos.
