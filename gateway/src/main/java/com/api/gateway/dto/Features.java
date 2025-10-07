package com.api.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Features {
    private List<AllowedEndpoint> allowedEndpoints = new ArrayList<>();
    private List<String> blockedEndpoints = new ArrayList<>();
}
