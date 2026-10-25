package com.footballai.social.model.enums;

/** As 4 janelas independentes de feed (RF3) — 1:1 com a bottom navigation. */
public enum FeedWindow {
    PERFIL(1, "ic-perfil", "/perfil", "Perfil", "Descoberta & Social Feed"),
    GEMEO(2, "ic-telemetria", "/gemeo", "Gêmeo", "Telemetria & Performance"),
    SCOUTING(3, "ic-scouting", "/scouting", "Scouting", "Prancheta tática & análises"),
    ALERTA(4, "ic-alerta", "/alerta", "Alerta", "Saúde, comunicados & DM");

    private final int order;
    private final String icon;
    private final String route;
    private final String label;
    private final String description;

    FeedWindow(int order, String icon, String route, String label, String description) {
        this.order = order; this.icon = icon; this.route = route; this.label = label; this.description = description;
    }

    public int order() { return order; }
    public String icon() { return icon; }
    public String route() { return route; }
    public String label() { return label; }
    public String description() { return description; }
}
