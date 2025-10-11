package com.api.license.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "allowed_endpoint")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowedEndpointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "path")
    private String path;

    @Column(name = "endpoint_expires_at")
    private String endPointExpiresAt;

    @Embedded
    private EndpointLimitEmbeddable limits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientLicenseEntity clientLicense;
}
