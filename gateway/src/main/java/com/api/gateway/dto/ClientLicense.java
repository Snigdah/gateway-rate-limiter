package com.api.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ClientLicense {
    private String clientSecret;
    private Boolean active;
    private String clientExpiresAt;
    private Features features;
}
