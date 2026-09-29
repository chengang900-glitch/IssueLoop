package com.rnd.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ThirdPartyLoginProvidersDto {
    private boolean enterprise;
    private boolean thirdParty;
    private List<String> providers;
}
