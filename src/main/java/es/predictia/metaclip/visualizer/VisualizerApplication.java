package es.predictia.metaclip.visualizer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
public class VisualizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(VisualizerApplication.class, args);
	}
	
	@Bean
	public ExecutorService executor(){
		return Executors.newFixedThreadPool(1);
	}
	
	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setThreadNamePrefix("metaclip-exec-");
		executor.initialize();
		return executor;
	}
}
