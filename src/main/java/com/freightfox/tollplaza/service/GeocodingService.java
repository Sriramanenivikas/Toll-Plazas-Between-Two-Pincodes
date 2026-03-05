package com.freightfox.tollplaza.service;

import com.freightfox.tollplaza.exception.InvalidPincodeException;
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
public class GeocodingService {

    private final RestClient restClient;

    @Value("${nominatim.base-url}")
    private String baseUrl;

    public double[] geocode(String pincode) {
        List<Map<String, Object>> results = restClient.get()
                .uri(baseUrl + "/search?postalcode={pincode}&country=India&format=json&limit=1", pincode)
                .header("User-Agent", "TollPlazaAPI/1.0")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (results == null || results.isEmpty()) {
            log.warn("Geocoding failed for pincode: {}", pincode);
            throw new InvalidPincodeException("Invalid pincode: " + pincode);
        }

        Map<String, Object> location = results.get(0);
        double lat = Double.parseDouble(location.get("lat").toString());
        double lon = Double.parseDouble(location.get("lon").toString());

        log.debug("Geocoded pincode {} -> [{}, {}]", pincode, lat, lon);
        return new double[] { lat, lon };
    }
}
