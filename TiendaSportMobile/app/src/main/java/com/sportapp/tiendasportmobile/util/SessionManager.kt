package com.sportapp.tiendasportmobile.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Guarda el token de sesión y los datos del usuario logueado en el dispositivo,
 * CIFRADOS con EncryptedSharedPreferences (llave en el Android Keystore), para que
 * la sesión sobreviva a cerrar la app sin dejar el token legible en el teléfono.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = crearPrefsCifradas(context.applicationContext)

    init {
        // Borra la sesión antigua guardada sin cifrar (el usuario deberá iniciar sesión una vez).
        context.applicationContext
            .getSharedPreferences(NOMBRE_ANTIGUO, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }

    fun guardarSesion(token: String, idClientes: Int, nombre: String, correo: String, idRol: Int) {
        prefs.edit()
            .putString("token", token)
            .putInt("id_clientes", idClientes)
            .putString("nombre", nombre)
            .putString("correo", correo)
            .putInt("id_rol", idRol)
            .apply()
    }

    fun obtenerToken(): String? = prefs.getString("token", null)
    fun obtenerNombre(): String? = prefs.getString("nombre", null)
    fun obtenerIdClientes(): Int = prefs.getInt("id_clientes", 0)
    fun obtenerIdRol(): Int = prefs.getInt("id_rol", 2)
    fun haySesionActiva(): Boolean = !obtenerToken().isNullOrEmpty()

    /** El header que exige el backend en cada endpoint protegido. */
    fun tokenConBearer(): String = "Bearer ${obtenerToken() ?: ""}"

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val NOMBRE_ANTIGUO = "tienda_sport_sesion"
        private const val NOMBRE_CIFRADO = "tienda_sport_sesion_cifrada"

        private fun crear(context: Context): SharedPreferences {
            val llave = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                NOMBRE_CIFRADO,
                llave,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        /** Si el Keystore quedó dañado (pasa en algunos teléfonos), se borra el archivo y se recrea; nunca se cae a texto plano. */
        private fun crearPrefsCifradas(context: Context): SharedPreferences =
            try {
                crear(context)
            } catch (e: Exception) {
                context.deleteSharedPreferences(NOMBRE_CIFRADO)
                crear(context)
            }
    }
}
