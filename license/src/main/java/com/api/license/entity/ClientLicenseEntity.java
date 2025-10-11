package com.api.license.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "client_license")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientLicenseEntity {

    @Id
    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "client_secret", nullable = false)
    private String clientSecret;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "client_expires_at")
    private String clientExpiresAt;

    // One client → many allowed endpoints
    @OneToMany(mappedBy = "clientLicense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<AllowedEndpointEntity> allowedEndpoints;

    // One client → many blocked endpoints (simple string list)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "blocked_endpoints", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "endpoint_path")
    private List<String> blockedEndpoints;
}
