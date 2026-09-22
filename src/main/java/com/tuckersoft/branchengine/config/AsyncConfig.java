package com.tuckersoft.branchengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * El pool donde corre el listener de notificaciones.
 *
 * El prefijo del hilo es lo que permite comprobar en el log que el envio NO ocurre
 * en el hilo de la peticion: tiene que salir branch-worker-N y no http-nio-8080-exec-N.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "branchExecutor")
    public Executor branchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("branch-worker-");
        executor.initialize();
        return executor;
    }
}
