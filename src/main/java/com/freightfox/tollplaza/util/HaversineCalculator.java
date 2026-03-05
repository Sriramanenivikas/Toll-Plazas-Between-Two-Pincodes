package com.freightfox.tollplaza.util;

import org.springframework.stereotype.Component;

@Component
public class HaversineCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    public double distance(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);

        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                        * Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    public double pointToSegmentDistance(
            double pointLat, double pointLon,
            double segStartLat, double segStartLon,
            double segEndLat, double segEndLon) {

        double abLat = segEndLat - segStartLat;
        double abLon = segEndLon - segStartLon;

        double apLat = pointLat - segStartLat;
        double apLon = pointLon - segStartLon;

        double abDotAb = abLat * abLat + abLon * abLon;

        if (abDotAb == 0) {
            return distance(pointLat, pointLon, segStartLat, segStartLon);
        }

        double t = (apLat * abLat + apLon * abLon) / abDotAb;
        t = Math.max(0, Math.min(1, t));

        double projectedLat = segStartLat + t * abLat;
        double projectedLon = segStartLon + t * abLon;

        return distance(pointLat, pointLon, projectedLat, projectedLon);
    }
}
