package com.sportapp.tiendasportmobile.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CarritoDao {

    @Query("SELECT * FROM carrito_items WHERE idCliente = :idCliente ORDER BY id ASC")
    fun observarCarrito(idCliente: Int): Flow<List<CarritoItemEntity>>

    @Query("SELECT * FROM carrito_items WHERE idCliente = :idCliente")
    suspend fun itemsDe(idCliente: Int): List<CarritoItemEntity>

    @Query("SELECT * FROM carrito_items WHERE idProducto = :idProducto AND talla = :talla AND color = :color AND idCliente = :idCliente LIMIT 1")
    suspend fun buscarItem(idProducto: Int, talla: String, color: String, idCliente: Int): CarritoItemEntity?

    @Insert
    suspend fun insertar(item: CarritoItemEntity)

    @Update
    suspend fun actualizar(item: CarritoItemEntity)

    @Delete
    suspend fun eliminar(item: CarritoItemEntity)

    @Query("DELETE FROM carrito_items WHERE idCliente = :idCliente")
    suspend fun vaciarCarrito(idCliente: Int)
}
