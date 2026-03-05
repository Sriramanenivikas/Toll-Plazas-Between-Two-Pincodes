package com.freightfox.tollplaza.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineCalculatorTest {

    private HaversineCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new HaversineCalculator();
    }

    @Test
    @DisplayName("Delhi to Mumbai straight-line distance should be ~1,153 km")
    void testKnownDistanceDelhiMumbai() {
        double distance = calculator.distance(28.7041, 77.1025, 19.0760, 72.8777);

        assertThat(distance).isCloseTo(1153.0, within(10.0));
    }

    @Test
    @DisplayName("Bengaluru to Pune straight-line distance should be ~740 km")
    void testKnownDistanceBengaluruPune() {
        double distance = calculator.distance(12.9716, 77.5946, 18.5204, 73.8567);

        assertThat(distance).isCloseTo(740.0, within(10.0));
    }

    @Test
    @DisplayName("Same point distance should be 0")
    void testZeroDistance() {
        double distance = calculator.distance(12.9716, 77.5946, 12.9716, 77.5946);

        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Short distance: two nearby points in Bengaluru")
    void testShortDistance() {
        double distance = calculator.distance(12.9758, 77.6045, 12.9784, 77.6408);

        assertThat(distance).isBetween(2.0, 6.0);
    }

    @Test
    @DisplayName("Point ON the segment distance should be ~0 km")
    void testPointToSegmentDistanceOnSegment() {
        double segStartLat = 13.3379, segStartLon = 77.1020;
        double segEndLat = 14.2226, segEndLon = 76.3984;
        double pointLat = (segStartLat + segEndLat) / 2;
        double pointLon = (segStartLon + segEndLon) / 2;

        double distance = calculator.pointToSegmentDistance(
                pointLat, pointLon,
                segStartLat, segStartLon,
                segEndLat, segEndLon);

        assertThat(distance).isLessThan(1.0);
    }

    @Test
    @DisplayName("Point OFF the segment (50+ km away)")
    void testPointToSegmentDistanceOffSegment() {
        double segStartLat = 13.3379, segStartLon = 77.1020;
        double segEndLat = 14.2226, segEndLon = 76.3984;
        double pointLat = 14.0, pointLon = 75.5;

        double distance = calculator.pointToSegmentDistance(
                pointLat, pointLon,
                segStartLat, segStartLon,
                segEndLat, segEndLon);

        assertThat(distance).isGreaterThan(50.0);
    }

    @Test
    @DisplayName("Point closest to segment START")
    void testPointToSegmentDistanceClosestToStart() {
        double segStartLat = 13.0, segStartLon = 77.0;
        double segEndLat = 14.0, segEndLon = 77.0;
        double pointLat = 12.95, pointLon = 77.05;

        double distance = calculator.pointToSegmentDistance(
                pointLat, pointLon,
                segStartLat, segStartLon,
                segEndLat, segEndLon);

        double expectedDistance = calculator.distance(pointLat, pointLon, segStartLat, segStartLon);
        assertThat(distance).isCloseTo(expectedDistance, within(1.0));
    }

    @Test
    @DisplayName("Point closest to segment END")
    void testPointToSegmentDistanceClosestToEnd() {
        double segStartLat = 13.0, segStartLon = 77.0;
        double segEndLat = 14.0, segEndLon = 77.0;
        double pointLat = 14.05, pointLon = 77.05;

        double distance = calculator.pointToSegmentDistance(
                pointLat, pointLon,
                segStartLat, segStartLon,
                segEndLat, segEndLon);

        double expectedDistance = calculator.distance(pointLat, pointLon, segEndLat, segEndLon);
        assertThat(distance).isCloseTo(expectedDistance, within(1.0));
    }
}
