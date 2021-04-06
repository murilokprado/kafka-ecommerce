package br.com.alura.ecommerce.consumer;

import java.util.concurrent.Executors;

public class ServiceRunner<T> {

    private static final int THREADS = 1;
    private final ServiceProvider<T> provider;

    public ServiceRunner(ServiceFactory<T> factory) {
        this.provider = new ServiceProvider<>(factory);
    }

    public void start() {
        start(THREADS);
    }

    public void start(int threadCount) {
        var poll = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i <= threadCount; i++) {
            poll.submit(provider);
        }
    }
}
