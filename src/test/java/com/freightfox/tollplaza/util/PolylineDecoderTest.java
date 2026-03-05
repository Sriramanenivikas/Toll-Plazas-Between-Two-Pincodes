package com.freightfox.tollplaza.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PolylineDecoderTest {

    private PolylineDecoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new PolylineDecoder();
    }

    @Test
    @DisplayName("Decode Google's reference polyline from docs")
    void testDecodeGoogleReferencePolyline() {

        String encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@";

        List<double[]> points = decoder.decode(encoded);

        assertThat(points).hasSize(3);


        assertThat(points.get(0)[0]).isCloseTo(38.5, within(0.01));
        assertThat(points.get(0)[1]).isCloseTo(-120.2, within(0.01));

        assertThat(points.get(1)[0]).isCloseTo(40.7, within(0.01));
        assertThat(points.get(1)[1]).isCloseTo(-120.95, within(0.01));


        assertThat(points.get(2)[0]).isCloseTo(43.252, within(0.01));
        assertThat(points.get(2)[1]).isCloseTo(-126.453, within(0.01));
    }

    @Test
    @DisplayName("Decode empty string returns empty list")
    void testDecodeEmptyString() {
        List<double[]> points = decoder.decode("");

        assertThat(points).isEmpty();
    }

    @Test
    @DisplayName("Decode null input returns empty list")
    void testDecodeNullInput() {
        List<double[]> points = decoder.decode(null);

        assertThat(points).isEmpty();
    }

}
