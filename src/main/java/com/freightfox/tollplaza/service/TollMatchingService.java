package com.freightfox.tollplaza.service;

import com.freightfox.tollplaza.dto.TollPlazaInfo;
import com.freightfox.tollplaza.repository.TollPlazaRepository;
import com.freightfox.tollplaza.model.TollPlaza;
import com.freightfox.tollplaza.util.HaversineCalculator;
import com.freightfox.tollplaza.util.PolylineDecoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TollMatchingService {

    private final TollPlazaRepository tollPlazaRepository;
    private final HaversineCalculator haversineCalculator;
    private final PolylineDecoder polylineDecoder;

    @Value("${toll.matching.threshold-km}")
    private double thresholdKm;

    public List<TollPlazaInfo> findTollsAlongRoute(String encodedPolyline, double[] sourceCoords) {
        List<double[]> routePoints = polylineDecoder.decode(encodedPolyline);

        if (routePoints.size() < 2) {
            log.warn("Route has fewer than 2 points, cannot match tolls");
            return List.of();
        }

        List<TollPlaza> allTolls = tollPlazaRepository.getAllTollPlazas();
        List<TollPlazaInfo> matchedTolls = new ArrayList<>();

        for (TollPlaza toll : allTolls) {
            double minDistance = Double.MAX_VALUE;
            int closestSegmentIndex = -1;

            for (int i = 0; i < routePoints.size() - 1; i++) {
                double[] segStart = routePoints.get(i);
                double[] segEnd = routePoints.get(i + 1);

                double dist = haversineCalculator.pointToSegmentDistance(
                        toll.getLatitude(), toll.getLongitude(),
                        segStart[0], segStart[1],
                        segEnd[0], segEnd[1]);

                if (dist < minDistance) {
                    minDistance = dist;
                    closestSegmentIndex = i;
                }
            }

            if (minDistance <= thresholdKm) {
                double distFromSource = computeDistanceFromSource(routePoints, closestSegmentIndex, sourceCoords);

                matchedTolls.add(TollPlazaInfo.builder()
                        .name(toll.getTollName())
                        .latitude(toll.getLatitude())
                        .longitude(toll.getLongitude())
                        .distanceFromSource(Math.round(distFromSource * 10.0) / 10.0)
                        .build());
            }
        }

        matchedTolls.sort((a, b) ->
                Double.compare(a.getDistanceFromSource(), b.getDistanceFromSource()));

        log.info("Matched {} toll plazas out of {} total (threshold: {} km)",
                matchedTolls.size(), allTolls.size(), thresholdKm);

        return matchedTolls;
    }

    private double computeDistanceFromSource(List<double[]> routePoints,
                                             int segmentIndex, double[] sourceCoords) {
        double totalDistance = 0;

        for (int i = 0; i < segmentIndex; i++) {
            totalDistance += haversineCalculator.distance(
                    routePoints.get(i)[0], routePoints.get(i)[1],
                    routePoints.get(i + 1)[0], routePoints.get(i + 1)[1]);
        }

        return totalDistance;
    }
}
