package com.freightfox.tollplaza.service;

import com.freightfox.tollplaza.dto.RouteInfo;
import com.freightfox.tollplaza.dto.TollPlazaInfo;
import com.freightfox.tollplaza.dto.TollPlazaRequest;
import com.freightfox.tollplaza.dto.TollPlazaResponse;
import com.freightfox.tollplaza.exception.SamePincodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TollPlazaService {

    private final GeocodingService geocodingService;
    private final RoutingService routingService;
    private final TollMatchingService tollMatchingService;

    @Cacheable(value = "tollPlazas", key = "#request.sourcePincode + '-' + #request.destinationPincode")
    public TollPlazaResponse findTollPlazas(TollPlazaRequest request) {
        if (request.getSourcePincode().equals(request.getDestinationPincode())) {
            throw new SamePincodeException("Source and destination pincodes cannot be the same");
        }

        log.info("Finding toll plazas: {} -> {}", request.getSourcePincode(), request.getDestinationPincode());

        double[] sourceCoords = geocodingService.geocode(request.getSourcePincode());
        double[] destCoords = geocodingService.geocode(request.getDestinationPincode());

        RoutingService.RouteResult routeResult = routingService.getRoute(sourceCoords, destCoords);

        List<TollPlazaInfo> tolls = tollMatchingService.findTollsAlongRoute(
                routeResult.encodedPolyline(), sourceCoords);

        log.info("Found {} toll plazas on route ({} km)",
                tolls.size(), String.format("%.1f", routeResult.distanceInKm()));

        return TollPlazaResponse.builder()
                .route(RouteInfo.builder()
                        .sourcePincode(request.getSourcePincode())
                        .destinationPincode(request.getDestinationPincode())
                        .distanceInKm(Math.round(routeResult.distanceInKm() * 10.0) / 10.0)
                        .build())
                .tollPlazas(tolls)
                .build();
    }
}
