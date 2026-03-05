# Toll Plazas Between Two Pincodes

A Spring Boot REST API that takes a source and destination pincode, figures out the driving route between them, and returns all the toll plazas that fall along that route — along with how far each one is from the starting point.

---

## How it works

1. Both pincodes are geocoded to lat/lon coordinates using the **Nominatim (OpenStreetMap)** API
2. A driving route is fetched between those coordinates using the **Mappls Directions API**
3. The route comes back as an encoded polyline, which gets decoded into a series of GPS points
4. Every toll plaza in the local CSV dataset is checked against the route — if a toll is within **0.6 km** of any route segment, it's included
5. Matched tolls are returned sorted by distance from the source

---

## Tech stack

- Java 21
- Spring Boot 4.x
- Spring Cache (Caffeine) — repeated requests for the same pincode pair don't hit external APIs again
- OpenCSV — for loading toll plaza data from CSV at startup
- Nominatim API — free geocoding (no key needed)
- Mappls Directions API — route computation (requires credentials)
- JUnit 5 + Mockito — unit tests

---

## Project structure

```
src/
├── main/java/com/freightfox/tollplaza/
│   ├── config/
│   │   ├── MapplsTokenProvider.java     # handles OAuth token refresh for Mappls
│   │   ├── RestClientConfig.java
│   │   └── CacheConfig.java
│   ├── controller/
│   │   └── TollPlazaController.java     # single POST endpoint
│   ├── service/
│   │   ├── TollPlazaService.java        # orchestrates the full flow
│   │   ├── GeocodingService.java        # pincode → coordinates
│   │   ├── RoutingService.java          # coordinates → encoded polyline
│   │   └── TollMatchingService.java     # polyline → matched toll plazas
│   ├── util/
│   │   ├── HaversineCalculator.java     # great-circle distance + point-to-segment
│   │   └── PolylineDecoder.java         # decodes Google-style encoded polylines
│   ├── repository/
│   │   └── TollPlazaRepository.java     # loads & caches toll data from CSV
│   ├── dto/
│   │   ├── TollPlazaRequest.java
│   │   ├── TollPlazaResponse.java
│   │   ├── RouteInfo.java
│   │   ├── TollPlazaInfo.java
│   │   └── ErrorResponse.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── InvalidPincodeException.java
│   │   ├── RouteNotFoundException.java
│   │   └── SamePincodeException.java
│   └── model/
│       └── TollPlaza.java
└── resources/
    ├── application.properties
    └── data/
        └── toll_plaza_india.csv
```

---

## Prerequisites

- Java 21+
- Maven 3.8+
- Mappls API credentials — get them from [https://apis.mappls.com](https://apis.mappls.com)

---

## Setup

**1. Clone the repo**

```bash
git clone <repo-url>
cd tollplaza
```

**2. Create a `.env` file in the project root**

```bash
touch .env
```

Add your Mappls credentials to it:

```
MAPPLS_CLIENT_ID=your_client_id_here
MAPPLS_CLIENT_SECRET=your_client_secret_here
```

> The `.env` file is git-ignored — never commit your credentials.

**3. Build and run**

```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8080`

---

## API

### POST `/api/v1/toll-plazas`

Takes a source and destination Indian pincode, returns the toll plazas along the driving route.

**Request**

```json
{
  "sourcePincode": "110001",
  "destinationPincode": "400001"
}
```

**Validations**
- Both fields are required
- Must be valid 6-digit Indian pincodes (no leading zero)
- Source and destination cannot be the same

**Success Response — 200 OK**

```json
{
  "route": {
    "sourcePincode": "110001",
    "destinationPincode": "400001",
    "distanceInKm": 1423.6
  },
  "tollPlazas": [
    {
      "name": "Dhaula Kuan Toll Plaza",
      "latitude": 28.5975,
      "longitude": 77.1731,
      "distanceFromSource": 12.4
    },
    {
      "name": "Murthal Toll Plaza",
      "latitude": 29.0956,
      "longitude": 77.0513,
      "distanceFromSource": 56.1
    }
  ]
}
```

Tolls are sorted by `distanceFromSource` (ascending).

---

## Error responses

All errors come back in this shape:

```json
{
  "error": "error message here"
}
```

| Scenario | HTTP Status |
|---|---|
| Same source and destination pincode | `400 Bad Request` |
| Pincode format is invalid | `400 Bad Request` |
| Pincode not found / unrecognised | `400 Bad Request` |
| No driving route found | `503 Service Unavailable` |
| Mappls API down or credentials missing | `503 Service Unavailable` |
| Anything unexpected | `500 Internal Server Error` |

---

## Running tests

```bash
mvn test
```

Test coverage includes:

- `HaversineCalculatorTest` — distance calculations, same-point edge case, point-to-segment projection
- `PolylineDecoderTest` — encoded polyline decoding, empty input, single point
- `TollMatchingServiceTest` — toll matching within threshold, tolls outside threshold, empty route
- `TollPlazaServiceTest` — full flow with mocked dependencies, same-pincode validation, cache behaviour

---

## Configuration

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | Port the server runs on |
| `toll.matching.threshold-km` | `0.6` | How close a toll must be to the route (in km) to be included |
| `toll.data.csv-path` | `data/toll_plaza_india.csv` | Path to the toll data file (classpath-relative) |
| `mappls.token-url` | Mappls OAuth URL | Token endpoint |
| `mappls.directions-url` | Mappls Directions URL | Route computation endpoint |
| `nominatim.base-url` | OpenStreetMap Nominatim | Geocoding endpoint |

---

## CSV data handling

The toll plaza dataset (`toll_plaza_india.csv`) has **2386 raw rows**. It's messy — the same plaza appears multiple times, some rows have bad coordinates, and a handful have swapped lat/lon columns. All of this is handled at startup before any request is served.

### Row-level validation (bad rows are skipped, logged as WARN)

| Problem | What it looks like | What happens |
|---|---|---|
| Too few columns | row has fewer than 4 fields | skipped |
| Blank toll name | `,,Karnataka` | skipped |
| Empty coordinate field | `,23.45,Name,State` | skipped |
| Unparseable coordinate | `abc,23.45,...` | skipped |
| NaN / Infinity | edge case from float serialisation | skipped |
| Both coords are `0.0` | default/sentinel value for missing data | skipped |
| Coordinates outside India | lat outside 6–37.5°N, lon outside 68–97.5°E | skipped |
| **Swapped lat/lon** | value in the longitude column fits India's lat range and vice-versa | **auto-corrected and kept** |

### Deduplication (two passes)

The dataset has a lot of duplicate entries. A single `name + lat + lon` string key isn't enough because of floating-point noise — the same plaza can appear as `28.597500` in one row and `28.5975001` in another.

**Pass 1 — exact match**
Key = normalised name + coordinates rounded to 6 decimal places (≈ 0.1 m precision). Catches rows that are character-for-character identical.

**Pass 2 — near-coordinate match**
Within each group of same-named plazas, any entry whose coordinates land within **50 metres** of an already-accepted entry is dropped. This handles GPS jitter and minor data-entry variance.

**What is kept intentionally:**
Same name, clearly different location (e.g. *Dhamnod Toll Plaza* appears on two different highways 30+ km apart). These are genuinely different physical plazas that share a name — both are kept.

### Startup log

On every start you'll see a line like:

```
Toll plaza CSV loaded — raw rows: 2386, skipped (bad data): 3, duplicates removed: 1198, final count: 1185
```

---

## Notes

- Responses for the same pincode pair are cached in-memory. Restarting the server clears the cache.
- The toll dataset is loaded once at startup. No database, no migrations — just the CSV.

