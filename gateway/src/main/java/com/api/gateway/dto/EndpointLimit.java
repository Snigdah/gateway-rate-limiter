package com.api.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EndpointLimit {
    private int perSecond;
    private int perMinute;
    private int perHour;
    private int perDay;
}
