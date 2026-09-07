package fr.dedoublonneur.config;

import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Executeur mono-thread dedie au job d'analyse/traitement en tache de fond.
 * Sa capacite (1 thread, aucune file d'attente) fait office de verrou naturel
 * garantissant qu'un seul workflow (analyse ou traitement) tourne a la fois.
 */
@Configuration
public class AsyncConfig {

    public static final String ANALYSIS_EXECUTOR = "analysisTaskExecutor";

    @Bean(name = ANALYSIS_EXECUTOR)
    public ThreadPoolTaskExecutor analysisTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("photo-analysis-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
