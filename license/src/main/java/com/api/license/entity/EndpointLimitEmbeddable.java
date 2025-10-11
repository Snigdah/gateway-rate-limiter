package com.api.license.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EndpointLimitEmbeddable {
    private int perSecond;
    private int perMinute;
    private int perHour;
    private int perDay;
}
