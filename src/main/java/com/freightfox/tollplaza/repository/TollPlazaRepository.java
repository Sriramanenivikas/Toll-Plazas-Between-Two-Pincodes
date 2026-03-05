package com.freightfox.tollplaza.repository;

import com.freightfox.tollplaza.model.TollPlaza;
import com.freightfox.tollplaza.util.HaversineCalculator;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TollPlazaRepository {

    private static final double INDIA_LAT_MIN = 6.0;
    private static final double INDIA_LAT_MAX = 37.5;
    private static final double INDIA_LON_MIN = 68.0;
    private static final double INDIA_LON_MAX = 97.5;

    private static final double COORD_DUPLICATE_THRESHOLD_KM = 0.05;

    private final HaversineCalculator haversineCalculator;

    @Value("${toll.data.csv-path}")
    private String csvPath;

    private List<TollPlaza> tollPlazas;

    @PostConstruct
    void loadCsvData() {
        ParseResult parseResult = readCsv();
        List<TollPlaza> deduplicated = deduplicate(parseResult.valid());
        this.tollPlazas = Collections.unmodifiableList(deduplicated);

        int raw        = parseResult.valid().size() + parseResult.skipped();
        int duplicates = parseResult.valid().size() - deduplicated.size();

        log.info("Toll plaza CSV loaded — raw rows: {}, skipped (bad data): {}, duplicates removed: {}, final count: {}",
                raw, parseResult.skipped(), duplicates, deduplicated.size());
    }

    public List<TollPlaza> getAllTollPlazas() {
        return tollPlazas;
    }


    private ParseResult readCsv() {
        List<TollPlaza> valid = new ArrayList<>();
        int skipped = 0;

        try (CSVReader reader = new CSVReaderBuilder(
                new InputStreamReader(new ClassPathResource(csvPath).getInputStream(), StandardCharsets.UTF_8))
                .withSkipLines(1)
                .build()) {

            String[] row;
            int lineNumber = 1;

            while ((row = reader.readNext()) != null) {
                lineNumber++;


                if (row.length < 4) {
                    log.warn("Line {}: skipping — expected 4 columns, got {}: {}",
                            lineNumber, row.length, Arrays.toString(row));
                    skipped++;
                    continue;
                }

                String rawLon = row[0].trim();
                String rawLat = row[1].trim();
                String name   = row[2].trim();
                String state  = row[3].trim();


                if (name.isEmpty()) {
                    log.warn("Line {}: skipping — toll name is blank (lon={}, lat={})", lineNumber, rawLon, rawLat);
                    skipped++;
                    continue;
                }


                if (rawLon.isEmpty() || rawLat.isEmpty()) {
                    log.warn("Line {}: skipping '{}' — coordinate field is empty", lineNumber, name);
                    skipped++;
                    continue;
                }


                double lon, lat;
                try {
                    lon = Double.parseDouble(rawLon);
                    lat = Double.parseDouble(rawLat);
                } catch (NumberFormatException e) {
                    log.warn("Line {}: skipping '{}' — unparseable coordinates (lon='{}', lat='{}')",
                            lineNumber, name, rawLon, rawLat);
                    skipped++;
                    continue;
                }


                if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
                    log.warn("Line {}: skipping '{}' — non-finite coordinate (lon={}, lat={})",
                            lineNumber, name, lon, lat);
                    skipped++;
                    continue;
                }


                if (lat == 0.0 && lon == 0.0) {
                    log.warn("Line {}: skipping '{}' — both coordinates are 0.0 (likely missing data)", lineNumber, name);
                    skipped++;
                    continue;
                }

             if (isInIndiaLatRange(lon) && isInIndiaLonRange(lat)) {
                    log.warn("Line {}: '{}' — lat/lon appear swapped (lon={}, lat={}), auto-correcting",
                            lineNumber, name, lon, lat);
                    double tmp = lat;
                    lat = lon;
                    lon = tmp;
                }

                if (!isInIndiaLatRange(lat) || !isInIndiaLonRange(lon)) {
                    log.warn("Line {}: skipping '{}' — coordinates outside India bounds (lat={}, lon={})",
                            lineNumber, name, lat, lon);
                    skipped++;
                    continue;
                }

                valid.add(TollPlaza.builder()
                        .tollName(name)
                        .latitude(lat)
                        .longitude(lon)
                        .state(state.isEmpty() ? "Unknown" : state)
                        .build());
            }

        } catch (Exception e) {
            log.error("Fatal: could not load toll plaza CSV from '{}'", csvPath, e);
            throw new RuntimeException("Cannot start without toll plaza data", e);
        }

        return new ParseResult(valid, skipped);
    }


    private List<TollPlaza> deduplicate(List<TollPlaza> rawList) {


        Set<String> exactKeys = new HashSet<>();
        List<TollPlaza> afterPass1 = new ArrayList<>();

        for (TollPlaza tp : rawList) {
            if (exactKeys.add(buildExactKey(tp))) {
                afterPass1.add(tp);
            }
        }


        Map<String, List<TollPlaza>> byName = new LinkedHashMap<>();
        for (TollPlaza tp : afterPass1) {
            byName.computeIfAbsent(normaliseName(tp.getTollName()), k -> new ArrayList<>()).add(tp);
        }

        List<TollPlaza> result = new ArrayList<>();
        long id = 1;

        for (List<TollPlaza> group : byName.values()) {
            List<TollPlaza> accepted = new ArrayList<>();

            for (TollPlaza candidate : group) {
                boolean nearDuplicate = accepted.stream().anyMatch(existing ->
                        haversineCalculator.distance(candidate.getLatitude(), candidate.getLongitude(),
                                existing.getLatitude(), existing.getLongitude())
                                <= COORD_DUPLICATE_THRESHOLD_KM);

                if (!nearDuplicate) {
                    accepted.add(candidate);
                }
            }

            for (TollPlaza tp : accepted) {
                result.add(TollPlaza.builder()
                        .id(id++)
                        .tollName(tp.getTollName())
                        .latitude(tp.getLatitude())
                        .longitude(tp.getLongitude())
                        .state(tp.getState())
                        .build());
            }
        }

        log.debug("Deduplication: {} after pass-1 (exact), {} after pass-2 (near-coord)",
                afterPass1.size(), result.size());

        return result;
    }


    private String buildExactKey(TollPlaza tp) {
        double lat = Math.round(tp.getLatitude()  * 1_000_000d) / 1_000_000d;
        double lon = Math.round(tp.getLongitude() * 1_000_000d) / 1_000_000d;
        return normaliseName(tp.getTollName()) + "|" + lat + "|" + lon;
    }

    private String normaliseName(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private boolean isInIndiaLatRange(double v) {
        return v >= INDIA_LAT_MIN && v <= INDIA_LAT_MAX;
    }

    private boolean isInIndiaLonRange(double v) {
        return v >= INDIA_LON_MIN && v <= INDIA_LON_MAX;
    }

    private record ParseResult(List<TollPlaza> valid, int skipped) {}
}
