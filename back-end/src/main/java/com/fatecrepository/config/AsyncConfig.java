package com.fatecrepository.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Execução assíncrona da notificação de submissão.
 *
 * <p>O pool é pequeno e a tarefa é best-effort: não vale fila infinita nem
 * crescer sem limite para um alerta que pode ser perdido sem prejuízo de dados.
 *
 * <p>O bean se chama {@code notificacaoExecutor} de propósito. Um bean nomeado
 * {@code applicationTaskExecutor} faria o {@code TaskExecutionAutoConfiguration} do
 * Boot recuar, e aí qualquer {@code @Async} sem qualifier do projeto passaria a
 * dividir este pool de 1-2 threads — um raio de explosão muito maior que a
 * notificação. Por isso o nome próprio e o qualifier correspondente em
 * {@code NotificacaoProjetoService}.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "notificacaoExecutor")
    public Executor notificacaoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("notificacao-");
        // Estouro de fila não derruba quem submeteu: a notificação é descartável.
        // Mas o descarte é logado — sem isso, "fila cheia" seria indistinguível de
        // "integração desligada" ou "não houve submissão".
        executor.setRejectedExecutionHandler((runnable, pool) -> log.warn(
            "Fila de notificação cheia: uma notificação de submissão foi descartada "
                + "(threads ativas={}, tamanho da fila={}, max={})",
            pool.getActiveCount(), pool.getQueue().size(), pool.getMaximumPoolSize()));
        executor.initialize();
        return executor;
    }
}
