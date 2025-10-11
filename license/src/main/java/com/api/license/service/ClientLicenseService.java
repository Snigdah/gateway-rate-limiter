package com.api.license.service;

import com.api.license.dto.AllowedEndpointDto;
import com.api.license.dto.ClientLicenseDto;
import com.api.license.dto.EndpointLimitDto;
import com.api.license.dto.FeaturesDto;
import com.api.license.entity.AllowedEndpointEntity;
import com.api.license.entity.ClientLicenseEntity;
import com.api.license.entity.EndpointLimitEmbeddable;
import com.api.license.repository.ClientLicenseRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ClientLicenseService {

    private final ClientLicenseRepository clientLicenseRepository;
    private final KafkaTemplate<String, ClientLicenseDto> kafkaTemplate;

    public ClientLicenseService(ClientLicenseRepository clientLicenseRepository,
                                KafkaTemplate<String, ClientLicenseDto> kafkaTemplate) {
        this.clientLicenseRepository = clientLicenseRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Value("${app.kafka.topic.client-license}")
    private String topicName;


    // 🔹 Create or Update Client License
    @Transactional
    public ClientLicenseDto saveOrUpdate(ClientLicenseDto dto) {
        ClientLicenseEntity saved = clientLicenseRepository.save(toEntity(dto));
        ClientLicenseDto savedDto = toDto(saved);

        publishKafkaEvent(savedDto);
        return savedDto;
    }

    // 🔹 Create or Update Multiple Client Licenses (Bulk)
    @Transactional
    public List<ClientLicenseDto> saveAll(List<ClientLicenseDto> dtos) {
        List<ClientLicenseEntity> entities = dtos.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());

        List<ClientLicenseEntity> savedEntities = clientLicenseRepository.saveAll(entities);
        List<ClientLicenseDto> savedDtos = savedEntities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        // ✅ publish one event per client (not the whole list)
        savedDtos.forEach(this::publishKafkaEvent);
        log.info("✅ Published {} client license events to topic={}", savedDtos.size(), topicName);

        return savedDtos;
    }

    // 🔹 Get all Clients
    public List<ClientLicenseDto> getAll() {
        return clientLicenseRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // 🔹 Get Client by ID
    public ClientLicenseDto getById(String clientId) {
        return clientLicenseRepository.findById(clientId)
                .map(this::toDto)
                .orElse(null);
    }

    // 🔹 Delete Client by ID
    @Transactional
    public void deleteById(String clientId) {
        clientLicenseRepository.deleteById(clientId);

        // Publish delete event
        ClientLicenseDto deletedEvent = new ClientLicenseDto();
        deletedEvent.setClientId(clientId);
        deletedEvent.setActive(false);

        kafkaTemplate.send(topicName, clientId, deletedEvent);
        log.warn("🗑️ Published delete event for clientId={} to topic={}", clientId, topicName);
    }


    private void publishKafkaEvent(ClientLicenseDto dto) {
        kafkaTemplate.send(topicName, dto.getClientId(), dto);
        log.info("📤 Published Kafka event for clientId={} to topic={}", dto.getClientId(), topicName);
    }



    // 🔄 Convert Entity to DTO
    private ClientLicenseDto toDto(ClientLicenseEntity entity) {
        if (entity == null) {
            return null;
        }

        // Convert allowed endpoints
        List<AllowedEndpointDto> allowedEndpointDtos = entity.getAllowedEndpoints()
                .stream()
                .map(this::toAllowedEndpointDto)
                .collect(Collectors.toList());

        // Build FeaturesDto
        FeaturesDto featuresDto = FeaturesDto.builder()
                .allowedEndpoints(allowedEndpointDtos)
                .blockedEndpoints(entity.getBlockedEndpoints())
                .build();

        // Build ClientLicenseDto
        return ClientLicenseDto.builder()
                .clientId(entity.getClientId())
                .clientSecret(entity.getClientSecret())
                .active(entity.getActive())
                .clientExpiresAt(entity.getClientExpiresAt())
                .features(featuresDto)
                .build();
    }

    // 🔄 Convert AllowedEndpointEntity to AllowedEndpointDto
    private AllowedEndpointDto toAllowedEndpointDto(AllowedEndpointEntity entity) {
        if (entity == null) {
            return null;
        }

        // Convert EndpointLimitEmbeddable to EndpointLimitDto
        EndpointLimitDto limitDto = EndpointLimitDto.builder()
                .perSecond(entity.getLimits().getPerSecond())
                .perMinute(entity.getLimits().getPerMinute())
                .perHour(entity.getLimits().getPerHour())
                .perDay(entity.getLimits().getPerDay())
                .build();

        return AllowedEndpointDto.builder()
                .path(entity.getPath())
                .endPointExpiresAt(entity.getEndPointExpiresAt())
                .limits(limitDto)
                .build();
    }

    // 🔄 Convert DTO to Entity
    private ClientLicenseEntity toEntity(ClientLicenseDto dto) {
        if (dto == null) {
            return null;
        }

        // Build main entity
        ClientLicenseEntity entity = ClientLicenseEntity.builder()
                .clientId(dto.getClientId())
                .clientSecret(dto.getClientSecret())
                .active(dto.getActive())
                .clientExpiresAt(dto.getClientExpiresAt())
                .build();

        // Convert allowed endpoints if features exist
        if (dto.getFeatures() != null && dto.getFeatures().getAllowedEndpoints() != null) {
            List<AllowedEndpointEntity> allowedEndpointEntities = dto.getFeatures()
                    .getAllowedEndpoints()
                    .stream()
                    .map(allowedEndpointDto -> toAllowedEndpointEntity(allowedEndpointDto, entity))
                    .collect(Collectors.toList());
            entity.setAllowedEndpoints(allowedEndpointEntities);
        }

        // Set blocked endpoints if features exist
        if (dto.getFeatures() != null) {
            entity.setBlockedEndpoints(dto.getFeatures().getBlockedEndpoints());
        }

        return entity;
    }

    // 🔄 Convert AllowedEndpointDto to AllowedEndpointEntity
    private AllowedEndpointEntity toAllowedEndpointEntity(AllowedEndpointDto dto, ClientLicenseEntity clientLicense) {
        if (dto == null) {
            return null;
        }

        // Convert EndpointLimitDto to EndpointLimitEmbeddable
        EndpointLimitEmbeddable limitEmbeddable = EndpointLimitEmbeddable.builder()
                .perSecond(dto.getLimits().getPerSecond())
                .perMinute(dto.getLimits().getPerMinute())
                .perHour(dto.getLimits().getPerHour())
                .perDay(dto.getLimits().getPerDay())
                .build();

        return AllowedEndpointEntity.builder()
                .path(dto.getPath())
                .endPointExpiresAt(dto.getEndPointExpiresAt())
                .limits(limitEmbeddable)
                .clientLicense(clientLicense)
                .build();
    }
}
