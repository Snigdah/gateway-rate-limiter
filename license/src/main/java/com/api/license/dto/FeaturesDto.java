package com.api.license.dto;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturesDto {
    private List<AllowedEndpointDto> allowedEndpoints = new ArrayList<>();
    private List<String> blockedEndpoints = new ArrayList<>();
}
