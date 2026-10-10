package com.defence.logistics.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP adapter for the four existing domain microservices.
 * This application intentionally owns no domain database or JPA entities.
 */
@Service
public class MicroserviceClient {
    private static final Logger log = LoggerFactory.getLogger(MicroserviceClient.class);

    private final RestClient resources;
    private final RestClient logistics;
    private final RestClient inventory;
    private final RestClient maintenance;

    public MicroserviceClient(
            @Value("${defence.services.resource.url:http://localhost:8081}") String resourceUrl,
            @Value("${defence.services.logistics.url:http://localhost:8082}") String logisticsUrl,
            @Value("${defence.services.inventory.url:http://localhost:8083}") String inventoryUrl,
            @Value("${defence.services.maintenance.url:http://localhost:8084}") String maintenanceUrl) {
        this.resources = RestClient.create(resourceUrl);
        this.logistics = RestClient.create(logisticsUrl);
        this.inventory = RestClient.create(inventoryUrl);
        this.maintenance = RestClient.create(maintenanceUrl);
    }

    public List<Map<String, Object>> getList(ServiceName service, String path) {
        try {
            Object response = client(service).get().uri(path).retrieve().body(Object.class);
            if (response instanceof List<?> values) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (Object value : values) {
                    if (value instanceof Map<?, ?> map) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        map.forEach((key, val) -> row.put(String.valueOf(key), val));
                        rows.add(row);
                    }
                }
                return rows;
            }
            if (response instanceof Map<?, ?> map && map.get("content") instanceof List<?> values) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (Object value : values) {
                    if (value instanceof Map<?, ?> row) {
                        Map<String, Object> converted = new LinkedHashMap<>();
                        row.forEach((key, val) -> converted.put(String.valueOf(key), val));
                        rows.add(converted);
                    }
                }
                return rows;
            }
            return Collections.emptyList();
        } catch (RestClientException ex) {
            log.warn("Could not read {} from {}: {}", path, service, ex.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<String, Object> getOne(ServiceName service, String path) {
        try {
            Map<String, Object> result = client(service).get().uri(path).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return result == null ? new LinkedHashMap<>() : result;
        } catch (RestClientException ex) {
            log.warn("Could not read {} from {}: {}", path, service, ex.getMessage());
            return new LinkedHashMap<>();
        }
    }

    public boolean post(ServiceName service, String path, Map<String, ?> payload) {
        try {
            client(service).post().uri(path).contentType(MediaType.APPLICATION_JSON)
                    .body(payload).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.warn("POST {} to {} failed: {}", path, service, ex.getMessage());
            return false;
        }
    }

    public boolean put(ServiceName service, String path, Map<String, ?> payload) {
        try {
            client(service).put().uri(path).contentType(MediaType.APPLICATION_JSON)
                    .body(payload).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.warn("PUT {} to {} failed: {}", path, service, ex.getMessage());
            return false;
        }
    }

    public boolean delete(ServiceName service, String path) {
        try {
            client(service).delete().uri(path).retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.warn("DELETE {} from {} failed: {}", path, service, ex.getMessage());
            return false;
        }
    }

    private RestClient client(ServiceName service) {
        return switch (service) {
            case RESOURCE -> resources;
            case LOGISTICS -> logistics;
            case INVENTORY -> inventory;
            case MAINTENANCE -> maintenance;
        };
    }

    public enum ServiceName { RESOURCE, LOGISTICS, INVENTORY, MAINTENANCE }
}
