package com.footballai.social.controller.dto;

import java.util.List;
import java.util.Map;

public final class NavigationDtos {
    private NavigationDtos() { }
    public record NavItemDTO(String window, int order, String icon, String route, String label, String description, String endpoint) { }
    public record NavigationDTO(List<NavItemDTO> bottomBar, Map<String, Object> rules, Map<String, String> tokens) { }
}
