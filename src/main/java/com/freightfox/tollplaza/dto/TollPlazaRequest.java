package com.freightfox.tollplaza.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


@Data
public class TollPlazaRequest {

    @NotBlank(message = "Source pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid source pincode format")
    private String sourcePincode;

    @NotBlank(message = "Destination pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Invalid destination pincode format")
    private String destinationPincode;

}
