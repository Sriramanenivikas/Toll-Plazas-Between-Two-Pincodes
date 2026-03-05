package com.freightfox.tollplaza.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TollPlaza {

    private Long id;
    private String tollName;
    private double latitude;
    private double longitude;
    private String state;

}
