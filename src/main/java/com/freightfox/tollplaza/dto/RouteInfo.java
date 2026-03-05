package com.freightfox.tollplaza.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RouteInfo {

    private String sourcePincode;
    private String destinationPincode;
    private double distanceInKm;

}
