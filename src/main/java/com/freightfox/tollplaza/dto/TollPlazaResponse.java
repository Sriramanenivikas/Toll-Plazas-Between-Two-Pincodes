package com.freightfox.tollplaza.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Builder
@Data
@NoArgsConstructor
public class TollPlazaResponse {

    private RouteInfo route;
    private List<TollPlazaInfo> tollPlazas;

}
