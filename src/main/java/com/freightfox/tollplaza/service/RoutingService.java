package com.freightfox.tollplaza.service;

import com.freightfox.tollplaza.config.MapplsTokenProvider;
import com.freightfox.tollplaza.exception.RouteNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RestClient restClient;
    private final MapplsTokenProvider tokenProvider;

    @Value("${mappls.directions-url}")
    private String directionsUrl;

    @SuppressWarnings("unchecked")
    public RouteResult getRoute(double[] source, double[] destination) {
        String token = tokenProvider.getAccessToken();
        String coordinates = source[1] + "," + source[0] + ";" + destination[1] + "," + destination[0];

        try {
            Map<String, Object> response = restClient.get()
                    .uri(directionsUrl + "/{token}/route_adv/driving/{coords}?geometries=polyline&overview=full",
                            token, coordinates)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null) {
                throw new RouteNotFoundException("Empty response from routing API");
            }

            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
            if (routes == null || routes.isEmpty()) {
                throw new RouteNotFoundException("No route found between given coordinates");
            }

            Map<String, Object> firstRoute = routes.get(0);
            String geometry = (String) firstRoute.get("geometry");
            double distanceMeters = ((Number) firstRoute.get("distance")).doubleValue();

            log.debug("Route found: {} km, polyline length: {} chars",
                    String.format("%.1f", distanceMeters / 1000), geometry.length());

            return new RouteResult(geometry, distanceMeters / 1000);

        } catch (RouteNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Routing API failed: {}", e.getMessage());
            throw new RouteNotFoundException("Unable to compute route. Please try again later.");
        }
    }

    public record RouteResult(String encodedPolyline, double distanceInKm) {
    }
}
