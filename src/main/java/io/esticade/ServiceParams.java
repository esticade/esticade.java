package io.esticade;

import java.util.UUID;

class ServiceParams {
    final String serviceName;
    final String correlationBlock;

    public ServiceParams(String serviceName) {
        this.serviceName = serviceName;
        this.correlationBlock = UUID.randomUUID().toString();
    }
}
