package com.sportapp.tiendasportmobile.data.repository

import android.content.Context
import com.sportapp.tiendasportmobile.data.local.AppDatabase
import com.sportapp.tiendasportmobile.data.local.CarritoItemEntity
import com.sportapp.tiendasportmobile.data.model.*
import com.sportapp.tiendasportmobile.data.network.RetrofitClient
import com.sportapp.tiendasportmobile.util.SessionManager
import kotlinx.coroutines.flow.Flow
import com.sportapp.tiendasportmobile.data.network.PedidoResponseItem

/**
 * Repositorio: es la única puerta de entrada para que las pantallas (Activities)
 * pidan datos, ya sea que vengan de la red (backend) o de la base de datos local
 * (carrito en SQLite). Así ninguna pantalla llama a Retrofit o a Room directamente,
 * siguiendo el mismo espíritu de separación de capas que ya usa el proyecto web
 * (api.js separado de los componentes de React).
 */
class TiendaRepository(context: Context) {

    private val api = RetrofitClient.api
    private val carritoDao = AppDatabase.obtener(context).carritoDao()
    val session = SessionManager(context)

    // ----------- Autenticación -----------
    suspend fun iniciarSesion(correo: String, password: String, rol: String): Result<LoginResponse> {
        val respuesta = api.iniciarSesion(LoginRequest(correo, password, rol))
        return if (respuesta.isSuccessful && respuesta.body() != null) {
            val body = respuesta.body()!!
            session.guardarSesion(body.token, body.id_clientes, body.nombre, body.correo, body.id_rol)
            fusionarCarritoInvitado(body.id_clientes)
            Result.success(body)
        } else {
            Result.failure(Exception(extraerError(respuesta.errorBody()?.string())))
        }
    }

    suspend fun registrarCliente(cliente: Cliente): Result<Unit> {
        val respuesta = api.registrarCliente(cliente)
        return if (respuesta.isSuccessful) Result.success(Unit)
        else Result.failure(Exception(extraerError(respuesta.errorBody()?.string())))
    }

    // ----------- Catálogo -----------
    suspend fun listarProductos(): Result<List<Producto>> {
        val respuesta = api.listarProductos()
        return if (respuesta.isSuccessful) Result.success(respuesta.body() ?: emptyList())
        else Result.failure(Exception(extraerError(respuesta.errorBody()?.string())))
    }

    suspend fun crearProducto(producto: Producto): Result<String> {
        val r = api.crearProducto(session.tokenConBearer(), producto)
        return if (r.isSuccessful) Result.success(r.body()?.mensaje ?: "Producto creado")
        else Result.failure(Exception(extraerError(r.errorBody()?.string())))
    }

    suspend fun actualizarProducto(id: Int, producto: Producto): Result<String> {
        val r = api.actualizarProducto(session.tokenConBearer(), id, producto)
        return if (r.isSuccessful) Result.success(r.body()?.mensaje ?: "Producto actualizado")
        else Result.failure(Exception(extraerError(r.errorBody()?.string())))
    }

    suspend fun eliminarProducto(id: Int): Result<String> {
        val r = api.eliminarProducto(session.tokenConBearer(), id)
        return if (r.isSuccessful) Result.success(r.body()?.mensaje ?: "Producto eliminado")
        else Result.failure(Exception(extraerError(r.errorBody()?.string())))
    }
    suspend fun listarCategorias(): Result<List<Categoria>> {
        val respuesta = api.listarCategorias()
        return if (respuesta.isSuccessful) Result.success(respuesta.body() ?: emptyList())
        else Result.failure(Exception(extraerError(respuesta.errorBody()?.string())))
    }

    suspend fun crearCategoria(categoria: Categoria): Result<String> {
        val r = api.crearCategoria(session.tokenConBearer(), categoria)
        return if (r.isSuccessful) Result.success(r.body()?.mensaje ?: "Categoría creada")
        else Result.failure(Exception(extraerError(r.errorBody()?.string())))
    }

    suspend fun actualizarCategoria(id: Int, categoria: Categoria): Result<String> {
        val r = api.actualizarCategoria(session.tokenConBearer(), id, categoria)
        return if (r.isSuccessful) Result.success(r.body()?.mensaje ?: "Categoría actualizada")
        else Result.failure(Exception(extraerError(r.errorBody()?.string())))
    }

    suspend fun eliminarCategoria(id: Int): Result<String> {
        val r = api.eliminarCategoria(session.tokenConBearer(), id)
        return if (r.isSuccessful) Result.success(r.body()?.mensaje ?: "Categoría eliminada")
        else Result.failure(Exception(extraerError(r.errorBody()?.string())))
    }

    suspend fun listarUsuarios(): Result<List<UsuarioItem>> {
        val r = api.listarUsuarios(session.tokenConBearer())
        return if (r.isSuccessful) Result.success(r.body() ?: emptyList())
        else Result.failure(Exception(extraerError(r.errorBody()?.string())))
    }

    suspend fun actualizarUsuario(id: Int, datos: ActualizarUsuarioRequest): Result<String> {
        val r = api.actualizarUsuario(session.tokenConBearer(), id, datos)
        return if (r.isSuccessful) Result.success(r.body()?.mensaje ?: "Usuario actualizado")
        else Result.failure(Exception(extraerError(r.errorBody()?.string())))
    }

    suspend fun eliminarUsuario(id: Int): Result<String> {
        val r = api.eliminarUsuario(session.tokenConBearer(), id)
        return if (r.isSuccessful) Result.success(r.body()?.mensaje ?: "Usuario eliminado")
        else Result.failure(Exception(extraerError(r.errorBody()?.string())))
    }

    // ----------- Carrito local (SQLite vía Room) -----------
    // El carrito es por usuario: 0 = invitado (sin sesión), si no el id del cliente con sesión.
    // Al cerrar sesión los productos quedan guardados y reaparecen al volver a entrar.
    private fun idCarritoActual(): Int = if (session.haySesionActiva()) session.obtenerIdClientes() else 0

    fun observarCarrito(): Flow<List<CarritoItemEntity>> = carritoDao.observarCarrito(idCarritoActual())

    suspend fun agregarAlCarrito(item: CarritoItemEntity) {
        val idCliente = idCarritoActual()
        val existente = carritoDao.buscarItem(item.idProducto, item.talla, item.color, idCliente)
        if (existente != null) {
            existente.cantidad += item.cantidad
            carritoDao.actualizar(existente)
        } else {
            carritoDao.insertar(item.copy(idCliente = idCliente))
        }
    }

    /** Al iniciar sesión, lo que se agregó como invitado pasa al carrito del usuario (sumando repetidos). */
    private suspend fun fusionarCarritoInvitado(idCliente: Int) {
        for (invitado in carritoDao.itemsDe(0)) {
            val propio = carritoDao.buscarItem(invitado.idProducto, invitado.talla, invitado.color, idCliente)
            if (propio != null) {
                propio.cantidad = minOf(propio.cantidad + invitado.cantidad, propio.stockDisponible)
                carritoDao.actualizar(propio)
                carritoDao.eliminar(invitado)
            } else {
                carritoDao.actualizar(invitado.copy(idCliente = idCliente))
            }
        }
    }

    suspend fun actualizarCantidad(item: CarritoItemEntity, nuevaCantidad: Int) {
        carritoDao.actualizar(item.copy(cantidad = nuevaCantidad.coerceIn(1, item.stockDisponible)))
    }

    suspend fun quitarDelCarrito(item: CarritoItemEntity) = carritoDao.eliminar(item)

    suspend fun vaciarCarrito() = carritoDao.vaciarCarrito(idCarritoActual())

    // ----------- Pedido -----------
    suspend fun confirmarPedido(items: List<CarritoItemEntity>): Result<Int> {
        val montoTotal = items.sumOf { it.precioMayorista * it.cantidad }

        val montoMinimo = obtenerConfiguracion().getOrNull()?.monto_minimo_compra ?: 200000.0
        if (montoTotal < montoMinimo) {
            return Result.failure(Exception(
                "El monto mínimo de compra es de \$${montoMinimo.toInt()}. Tu carrito suma \$${montoTotal.toInt()}."
            ))
        }

        // El servidor lee los precios de la base de datos y calcula el total: aquí solo se envía
        // qué producto, talla, color y cantidad. Todo se crea en una sola transacción.
        val lineas = items.map { ItemCompraRequest(it.idProducto, it.talla, it.color, it.cantidad) }
        val respuesta = api.crearPedidoCompleto(session.tokenConBearer(), lineas)
        if (!respuesta.isSuccessful || respuesta.body() == null) {
            return Result.failure(Exception(extraerError(respuesta.errorBody()?.string())))
        }
        vaciarCarrito()
        return Result.success(respuesta.body()!!.id_pedido)
    }

    suspend fun listarPedidos(): Result<List<PedidoResponseItem>> {
        val respuesta = api.listarPedidos(session.tokenConBearer())
        return if (respuesta.isSuccessful) Result.success(respuesta.body() ?: emptyList())
        else Result.failure(Exception(extraerError(respuesta.errorBody()?.string())))
    }

    suspend fun listarDetallePedido(idPedido: Int): Result<List<DetallePedidoItem>> {
        val respuesta = api.listarDetallePedido(session.tokenConBearer(), idPedido)
        return if (respuesta.isSuccessful) Result.success(respuesta.body() ?: emptyList())
        else Result.failure(Exception(extraerError(respuesta.errorBody()?.string())))
    }

    private fun extraerError(cuerpoError: String?): String {
        if (cuerpoError.isNullOrBlank()) return "Error de conexión con el servidor"
        return try {
            val regex = "\"error\"\\s*:\\s*\"([^\"]*)\"".toRegex()
            regex.find(cuerpoError)?.groupValues?.get(1) ?: cuerpoError
        } catch (e: Exception) {
            cuerpoError
        }
    }
    suspend fun obtenerConfiguracion(): Result<ConfiguracionTienda> {
        val r = api.obtenerConfiguracion()
        return if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.failure(Exception(extraerError(r.errorBody()?.string())))
    }

    suspend fun actualizarConfiguracion(nuevoMonto: Double): Result<String> {
        val r = api.actualizarConfiguracion(session.tokenConBearer(), ConfiguracionTienda(1, nuevoMonto))
        return if (r.isSuccessful) Result.success(r.body()?.mensaje ?: "Configuración actualizada")
        else Result.failure(Exception(extraerError(r.errorBody()?.string())))
    }
}
