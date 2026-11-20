package com.sportapp.tiendasport.config;

import com.sportapp.tiendasport.model.Cliente;
import com.sportapp.tiendasport.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, cifra con BCrypt cualquier contraseña que todavía esté en texto
 * plano (cuentas del backend antiguo que nunca volvieron a iniciar sesión).
 * Es idempotente: si todas ya son hash BCrypt, no hace nada. Nunca escribe
 * contraseñas en el log.
 */
@Component
@RequiredArgsConstructor
public class PasswordMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordMigrationRunner.class);

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        int migradas = 0;
        for (Cliente c : clienteRepository.findAll()) {
            String p = c.getPassword();
            if (p != null && !p.isEmpty() && !(p.startsWith("$2a$") || p.startsWith("$2b$") || p.startsWith("$2y$"))) {
                c.setPassword(passwordEncoder.encode(p));
                clienteRepository.save(c);
                migradas++;
            }
        }
        if (migradas > 0) {
            log.info("Contraseñas en texto plano cifradas con BCrypt: {}", migradas);
        }
    }
}
