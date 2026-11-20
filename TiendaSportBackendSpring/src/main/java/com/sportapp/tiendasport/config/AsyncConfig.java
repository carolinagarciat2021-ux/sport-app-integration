package com.sportapp.tiendasport.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Permite tareas en segundo plano (envío de correos) para que un correo lento no demore la compra. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
