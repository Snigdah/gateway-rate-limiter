package com.api.gateway;

import com.api.gateway.dto.AllowedEndpoint;
import com.api.gateway.dto.ClientLicense;
import com.api.gateway.dto.ClientLicensesConfig;
import com.api.gateway.dto.Features;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileLicenseService {
    private Map<String, ClientLicense> licenses = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() throws IOException {
        // Load from classpath - looks in src/main/resources
        ClassPathResource classPathResource = new ClassPathResource("license.json");
        if (classPathResource.exists()) {
            // Use the new root DTO class for direct mapping
            ClientLicensesConfig config = objectMapper.readValue(classPathResource.getInputStream(), ClientLicensesConfig.class);
            licenses = config.getClients();
        } else {
            throw new IOException("license.json file not found in classpath");
        }
    }

    // Alternative method for external file
    /*
    @PostConstruct
    public void init() throws IOException {
        // Load from external license.json file
        FileSystemResource externalFile = new FileSystemResource("license.json");
        if (externalFile.exists()) {
            ClientLicensesConfig config = objectMapper.readValue(externalFile.getInputStream(), ClientLicensesConfig.class);
            licenses = config.getClients();
        } else {
            throw new IOException("license.json file not found");
        }
    }
    */

    // Get license by id
    public ClientLicense getLicense(String clientId) {
        return licenses.get(clientId);
    }

    // Check if license exist
    public boolean licenseExists(String clientId) {
        return licenses.containsKey(clientId);
    }

    // Get all clients
    public Map<String, ClientLicense> getAllClients() {
        return new HashMap<>(licenses);
    }

    // Get client secret
    public String getClientSecret(String clientId) {
        ClientLicense license = licenses.get(clientId);
        return license != null ? license.getClientSecret() : null;
    }

    // Get is license Active
    public Boolean isActive(String clientId) {
        ClientLicense license = licenses.get(clientId);
        return license != null ? license.getActive() : null;
    }

    // Get client expiration date
    public String getClientExpiresAt(String clientId) {
        ClientLicense license = licenses.get(clientId);
        return license != null ? license.getClientExpiresAt() : null;
    }

    // Get Client Features
    public Features getFeatures(String clientId) {
        ClientLicense license = licenses.get(clientId);
        return license != null ? license.getFeatures() : null;
    }

    // Get allowed endpoints for a client
    public List<AllowedEndpoint> getAllowedEndpoints(String clientId) {
        ClientLicense license = licenses.get(clientId);
        return license != null && license.getFeatures() != null ? license.getFeatures().getAllowedEndpoints() : new ArrayList<>();
    }

    // Get blocked endpoints for a client
    public List<String> getBlockedEndpoints(String clientId) {
        ClientLicense license = licenses.get(clientId);
        return license != null && license.getFeatures() != null ? license.getFeatures().getBlockedEndpoints() : new ArrayList<>();
    }

    // Get all allowed endpoints with their limits
    public List<AllowedEndpoint> getAllowedEndpointsWithLimits(String clientId) {
        Features features = getFeatures(clientId);
        return features != null ? features.getAllowedEndpoints() : new ArrayList<>();
    }

    // Get endpoint limits for a specific path pattern
    public AllowedEndpoint getAllowedEndpointConfig(String clientId, String pathPattern) {
        List<AllowedEndpoint> allowedEndpoints = getAllowedEndpoints(clientId);
        return allowedEndpoints.stream()
                .filter(endpoint -> endpoint.getPath().equals(pathPattern))
                .findFirst()
                .orElse(null);
    }

    // Check if endpoint is blocked for a client
    public boolean isEndpointBlocked(String clientId, String requestPath) {
        List<String> blockedEndpoints = getBlockedEndpoints(clientId);
        return blockedEndpoints.stream()
                .anyMatch(blockedPattern -> matchesPathPattern(requestPath, blockedPattern));
    }

    // Check if endpoint is allowed for a client
    public boolean isEndpointAllowed(String clientId, String requestPath) {
        List<AllowedEndpoint> allowedEndpoints = getAllowedEndpoints(clientId);
        return allowedEndpoints.stream()
                .anyMatch(endpoint -> matchesPathPattern(requestPath, endpoint.getPath()));
    }

    // Helper method to check if a request path matches a pattern
    public boolean matchesPathPattern(String requestPath, String pattern) {
        // Convert pattern to regex
        String regex = pattern
                .replace("**", ".*") // ** matches any characters
                .replace("*", "[^/]*") // * matches any characters except /
                .replace("?", ".");
        return requestPath.matches(regex);
    }

    // Get endpoint expiration for a specific path
    public String getEndpointExpiresAt(String clientId, String pathPattern) {
        AllowedEndpoint endpointConfig = getAllowedEndpointConfig(clientId, pathPattern);
        return endpointConfig != null ? endpointConfig.getEndPointExpiresAt() : null;
    }

    // Validate if client license is still valid
    public boolean isLicenseValid(String clientId) {
        ClientLicense license = getLicense(clientId);
        if (license == null || !Boolean.TRUE.equals(license.getActive())) {
            return false;
        }
        // Check client expiration
        String expiresAt = license.getClientExpiresAt();
        if (expiresAt != null) {
            // Add date validation logic here if needed
            // For now, just return true since all dates are 2025-12-31
            return true;
        }
        return true;
    }

    // Get allowed endpoint configuration for a specific path
    public AllowedEndpoint getAllowedEndpointForPath(String clientId, String requestPath) {
        List<AllowedEndpoint> allowedEndpoints = getAllowedEndpoints(clientId);
        return allowedEndpoints.stream()
                .filter(endpoint -> matchesPathPattern(requestPath, endpoint.getPath()))
                .findFirst()
                .orElse(null);
    }
}