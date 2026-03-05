package com.freightfox.tollplaza.service;

import com.freightfox.tollplaza.dto.TollPlazaInfo;
import com.freightfox.tollplaza.dto.TollPlazaRequest;
import com.freightfox.tollplaza.dto.TollPlazaResponse;
import com.freightfox.tollplaza.exception.InvalidPincodeException;
import com.freightfox.tollplaza.exception.RouteNotFoundException;
import com.freightfox.tollplaza.exception.SamePincodeException;
import com.freightfox.tollplaza.service.RoutingService.RouteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TollPlazaServiceTest {

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private RoutingService routingService;

    @Mock
    private TollMatchingService tollMatchingService;

    @InjectMocks
    private TollPlazaService tollPlazaService;

    private TollPlazaRequest request;

    @BeforeEach
    void setUp() {
        request = new TollPlazaRequest();
        request.setSourcePincode("560064");
        request.setDestinationPincode("411045");
    }

    @Test
    void testFindTollPlazasHappyPath() {
        double[] source = {12.9716, 77.5946};
        double[] destination = {18.5204, 73.8567};

        TollPlazaInfo toll = TollPlazaInfo.builder()
                .name("Anekal Toll Plaza")
                .latitude(12.88)
                .longitude(77.68)
                .distanceFromSource(20.4)
                .build();

        when(geocodingService.geocode("560064")).thenReturn(source);
        when(geocodingService.geocode("411045")).thenReturn(destination);
        when(routingService.getRoute(source, destination)).thenReturn(new RouteResult("polyline", 842.36));
        when(tollMatchingService.findTollsAlongRoute("polyline", source)).thenReturn(List.of(toll));

        TollPlazaResponse response = tollPlazaService.findTollPlazas(request);

        assertThat(response.getRoute().getSourcePincode()).isEqualTo("560064");
        assertThat(response.getRoute().getDestinationPincode()).isEqualTo("411045");
        assertThat(response.getRoute().getDistanceInKm()).isEqualTo(842.4);
        assertThat(response.getTollPlazas()).hasSize(1);
        assertThat(response.getTollPlazas().getFirst().getName()).isEqualTo("Anekal Toll Plaza");

        verify(geocodingService).geocode("560064");
        verify(geocodingService).geocode("411045");
        verify(routingService).getRoute(source, destination);
        verify(tollMatchingService).findTollsAlongRoute("polyline", source);
    }

    @Test
    void testFindTollPlazasSamePincode() {
        request.setDestinationPincode("560064");

        assertThatThrownBy(() -> tollPlazaService.findTollPlazas(request))
                .isInstanceOf(SamePincodeException.class)
                .hasMessage("Source and destination pincodes cannot be the same");

        verifyNoInteractions(geocodingService, routingService, tollMatchingService);
    }

    @Test
    void testFindTollPlazasInvalidPincode() {
        when(geocodingService.geocode("560064"))
                .thenThrow(new InvalidPincodeException("Invalid pincode: 560064"));

        assertThatThrownBy(() -> tollPlazaService.findTollPlazas(request))
                .isInstanceOf(InvalidPincodeException.class)
                .hasMessage("Invalid pincode: 560064");

        verify(geocodingService).geocode("560064");
    }

    @Test
    void testFindTollPlazasNoTollsFound() {
        double[] source = {12.9716, 77.5946};
        double[] destination = {18.5204, 73.8567};

        when(geocodingService.geocode("560064")).thenReturn(source);
        when(geocodingService.geocode("411045")).thenReturn(destination);
        when(routingService.getRoute(source, destination)).thenReturn(new RouteResult("polyline", 842.0));
        when(tollMatchingService.findTollsAlongRoute("polyline", source)).thenReturn(List.of());

        TollPlazaResponse response = tollPlazaService.findTollPlazas(request);

        assertThat(response.getRoute().getDistanceInKm()).isEqualTo(842.0);
        assertThat(response.getTollPlazas()).isEmpty();
    }

    @Test
    void testFindTollPlazasRouteApiFailure() {
        double[] source = {12.9716, 77.5946};
        double[] destination = {18.5204, 73.8567};

        when(geocodingService.geocode("560064")).thenReturn(source);
        when(geocodingService.geocode("411045")).thenReturn(destination);
        when(routingService.getRoute(any(double[].class), any(double[].class)))
                .thenThrow(new RouteNotFoundException("Unable to compute route. Please try again later."));

        assertThatThrownBy(() -> tollPlazaService.findTollPlazas(request))
                .isInstanceOf(RouteNotFoundException.class)
                .hasMessage("Unable to compute route. Please try again later.");

        verify(routingService).getRoute(eq(source), eq(destination));
    }
}
