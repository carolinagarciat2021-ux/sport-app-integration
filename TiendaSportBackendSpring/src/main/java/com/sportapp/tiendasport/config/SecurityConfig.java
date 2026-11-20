package com.sportapp.tiendasport.config;

import com.sportapp.tiendasport.security.CustomUserDetailsService;
import com.sportapp.tiendasport.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // habilita @PreAuthorize("hasRole('ADMINISTRADOR')") en los controladores
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;

    // En Railway/Render, define esta variable de entorno con la URL real de tu
    // frontend publicado, por ejemplo: https://tiendasport.vercel.app
    // Puedes poner varias separadas por coma. En tu computador, no hace falta
    // tocar nada: ya incluye localhost:3000 por defecto.
    @Value("${ALLOWED_ORIGINS:http://localhost:3000}")
    private String allowedOrigins;

    // BCrypt: cada vez que se guarda una contraseña se cifra con un "salt" distinto,
    // así que dos usuarios con la misma contraseña NUNCA tienen el mismo hash guardado.
    // Esto reemplaza el texto plano que usaba el backend anterior.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // no hace falta CSRF: la API no usa cookies de sesión, usa JWT
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ---- Público: cualquiera puede entrar (invitados incluidos) ----
                .requestMatchers(HttpMethod.POST, "/api/login", "/api/clientes").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categorias", "/api/producto", "/api/configuracion", "/api/descuentos", "/api/subcategorias", "/api/empresa").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/imagenes/**").permitAll()

                // ---- Solo Administrador ----
                .requestMatchers(HttpMethod.POST, "/api/categorias", "/api/subcategorias", "/api/imagenes").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/subcategorias/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/subcategorias/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/categorias/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/categorias/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/producto/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/clientes/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/api/clientes").hasRole("ADMINISTRADOR")
                // Solo el Administrador puede eliminar pedidos (PedidoService vuelve a comprobarlo).
                // Clientes y Vendedores NO pueden. Los detalles solo se borran junto con su pedido.
                .requestMatchers(HttpMethod.DELETE, "/api/pedidos/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.DELETE, "/api/detalle_pedido/**").denyAll()
                // Historial de actividad: solo lectura y solo Administrador (no hay endpoints para editar/borrar)
                .requestMatchers(HttpMethod.GET, "/api/actividad/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/actividad/**").denyAll()

                .requestMatchers(HttpMethod.POST, "/api/producto").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/producto/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/configuracion").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/descuentos").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.PUT, "/api/empresa", "/api/empresa/**").hasRole("ADMINISTRADOR")

                // ---- Administrador o Vendedor ----
                // --- .requestMatchers(HttpMethod.POST, "/api/producto").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                // --- .requestMatchers(HttpMethod.PUT, "/api/producto/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                // Solo ellos cambian el estado de un pedido (la anulación la limita además PedidoService)
                .requestMatchers(HttpMethod.PUT, "/api/pedidos/**").hasAnyRole("ADMINISTRADOR", "VENDEDOR")

                // ---- Cualquier usuario autenticado (Cliente, Vendedor o Administrador) ----
                .requestMatchers(HttpMethod.GET, "/api/pedidos/todos").hasAnyRole("ADMINISTRADOR", "VENDEDOR")
                .requestMatchers("/api/pedidos/**", "/api/detalle_pedido/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/clientes/**").authenticated()

                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
