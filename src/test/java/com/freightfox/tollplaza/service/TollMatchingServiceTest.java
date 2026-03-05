package com.freightfox.tollplaza.service;

import com.freightfox.tollplaza.dto.TollPlazaInfo;
import com.freightfox.tollplaza.model.TollPlaza;
import com.freightfox.tollplaza.repository.TollPlazaRepository;
import com.freightfox.tollplaza.util.HaversineCalculator;
import com.freightfox.tollplaza.util.PolylineDecoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TollMatchingServiceTest {

    @Mock
    private TollPlazaRepository tollPlazaRepository;

    @Mock
    private HaversineCalculator haversineCalculator;

    @Mock
    private PolylineDecoder polylineDecoder;

    @InjectMocks
    private TollMatchingService tollMatchingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tollMatchingService, "thresholdKm", 1.0);
    }

    @Test
    void testFindTollsTollOnRoute() {
        List<double[]> routePoints = List.of(
                new double[]{12.0, 77.0},
                new double[]{12.1, 77.1},
                new double[]{12.2, 77.2});

        TollPlaza toll = TollPlaza.builder()
                .id(1L)
                .tollName("Near Route Toll")
                .latitude(12.05)
                .longitude(77.05)
                .state("KA")
                .build();

        when(polylineDecoder.decode("polyline")).thenReturn(routePoints);
        when(tollPlazaRepository.getAllTollPlazas()).thenReturn(List.of(toll));
        when(haversineCalculator.pointToSegmentDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(0.4, 0.9);

        List<TollPlazaInfo> result = tollMatchingService.findTollsAlongRoute("polyline", new double[]{12.0, 77.0});

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Near Route Toll");
        assertThat(result.getFirst().getDistanceFromSource()).isEqualTo(0.0);
    }

    @Test
    void testFindTollsTollFarFromRoute() {
        List<double[]> routePoints = List.of(
                new double[]{12.0, 77.0},
                new double[]{12.1, 77.1},
                new double[]{12.2, 77.2});

        TollPlaza toll = TollPlaza.builder()
                .id(1L)
                .tollName("Far Toll")
                .latitude(13.0)
                .longitude(78.0)
                .state("KA")
                .build();

        when(polylineDecoder.decode("polyline")).thenReturn(routePoints);
        when(tollPlazaRepository.getAllTollPlazas()).thenReturn(List.of(toll));
        when(haversineCalculator.pointToSegmentDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(2.2, 3.1);

        List<TollPlazaInfo> result = tollMatchingService.findTollsAlongRoute("polyline", new double[]{12.0, 77.0});

        assertThat(result).isEmpty();
    }

    @Test
    void testFindTollsMultipleTollsSortedByDistance() {
        List<double[]> routePoints = List.of(
                new double[]{0.0, 0.0},
                new double[]{0.0, 1.0},
                new double[]{0.0, 2.0},
                new double[]{0.0, 3.0});

        TollPlaza tollSegment2 = TollPlaza.builder().id(1L).tollName("Third").latitude(0.0).longitude(2.1).state("X").build();
        TollPlaza tollSegment0 = TollPlaza.builder().id(2L).tollName("First").latitude(0.0).longitude(0.1).state("X").build();
        TollPlaza tollSegment1 = TollPlaza.builder().id(3L).tollName("Second").latitude(0.0).longitude(1.1).state("X").build();

        when(polylineDecoder.decode("polyline")).thenReturn(routePoints);
        when(tollPlazaRepository.getAllTollPlazas()).thenReturn(List.of(tollSegment2, tollSegment0, tollSegment1));

        when(haversineCalculator.pointToSegmentDistance(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(
                        9.0, 9.0, 0.2,
                        0.2, 9.0, 9.0,
                        9.0, 0.2, 9.0);

        when(haversineCalculator.distance(0.0, 0.0, 0.0, 1.0)).thenReturn(50.0);
        when(haversineCalculator.distance(0.0, 1.0, 0.0, 2.0)).thenReturn(70.0);

        List<TollPlazaInfo> result = tollMatchingService.findTollsAlongRoute("polyline", new double[]{0.0, 0.0});

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getName()).isEqualTo("First");
        assertThat(result.get(0).getDistanceFromSource()).isEqualTo(0.0);
        assertThat(result.get(1).getName()).isEqualTo("Second");
        assertThat(result.get(1).getDistanceFromSource()).isEqualTo(50.0);
        assertThat(result.get(2).getName()).isEqualTo("Third");
        assertThat(result.get(2).getDistanceFromSource()).isEqualTo(120.0);
    }

    @Test
    void testFindTollsEmptyPolyline() {
        when(polylineDecoder.decode("polyline")).thenReturn(List.of());

        List<TollPlazaInfo> result = tollMatchingService.findTollsAlongRoute("polyline", new double[]{12.0, 77.0});

        assertThat(result).isEmpty();
        verifyNoInteractions(tollPlazaRepository, haversineCalculator);
    }

    @Test
    void testFindTollsNoTollsInRepository() {
        when(polylineDecoder.decode("polyline"))
                .thenReturn(List.of(new double[]{12.0, 77.0}, new double[]{12.5, 77.5}));
        when(tollPlazaRepository.getAllTollPlazas()).thenReturn(List.of());

        List<TollPlazaInfo> result = tollMatchingService.findTollsAlongRoute("polyline", new double[]{12.0, 77.0});

        assertThat(result).isEmpty();
        verifyNoInteractions(haversineCalculator);
    }
}
