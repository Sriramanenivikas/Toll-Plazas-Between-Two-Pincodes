package com.freightfox.tollplaza.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PolylineDecoder {

    private static final double PRECISION = 1e5;

    public List<double[]> decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return Collections.emptyList();
        }

        List<double[]> points = new ArrayList<>();
        int[] index = {0};
        int lat = 0;
        int lng = 0;

        while (index[0] < encoded.length()) {
            lat += decodeNextValue(encoded, index);
            lng += decodeNextValue(encoded, index);
            points.add(new double[] { lat / PRECISION, lng / PRECISION });
        }

        return points;
    }

    private int decodeNextValue(String encoded, int[] index) {
        int result = 0;
        int shift = 0;
        int chunk;

        do {
            chunk = encoded.charAt(index[0]++) - 63;
            result |= (chunk & 0x1F) << shift;
            shift += 5;
        } while (chunk >= 0x20);

        return (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
    }
}
