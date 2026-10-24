package com.footballai.social.service;

import com.footballai.social.model.enums.*;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filtros segmentados por janela (RF3) — espelham o {@code <aside class="filters-panel">} de cada aba.
 * Comuns: sort, role, state, clubId, q, cursor, size. PERFIL: tier, position, ageMin/ageMax, contractStatus.
 * GEMEO: period, metricType, eventType, athleteId. SCOUTING: attr[velocidade]=gt:80, compare, source, position, ageMax.
 * ALERTA: severity, alertType, source.
 */
public final class FeedQuery {
    public record AttributeFilter(String name, String op, double value) { }
    private static final Pattern ATTR = Pattern.compile("^attr\\[(\\w+)]$");
    private static final Pattern OPV = Pattern.compile("^(gt|gte|lt|lte|eq):(\\d+(?:\\.\\d+)?)$");

    public final FeedWindow window;
    public final FeedSort sort;
    public final List<UserRole> roles;
    public final String state;
    public final UUID clubId;
    public final String q;
    public final String cursor;
    public final int size;
    public final List<RarityTier> tiers;
    public final Position position;
    public final Integer ageMin, ageMax;
    public final ContractStatus contractStatus;
    public final Period period;
    public final List<MetricType> metricTypes;
    public final List<EventType> eventTypes;
    public final UUID athleteId;
    public final List<AttributeFilter> attributes;
    public final List<UUID> compare;
    public final ReportSource source;
    public final List<AlertSeverity> severities;
    public final List<AlertType> alertTypes;
    public final List<UserRole> sources;

    private FeedQuery(FeedWindow window, MultiValueMap<String, String> p) {
        this.window = window;
        this.sort = enumOr(FeedSort.class, first(p, "sort"), FeedSort.FOR_YOU);
        this.roles = enums(UserRole.class, p.get("role"));
        this.state = upper(first(p, "state"));
        this.clubId = uuid(first(p, "clubId"));
        this.q = blankToNull(first(p, "q"));
        this.cursor = blankToNull(first(p, "cursor"));
        int s = intOr(first(p, "size"), 20); this.size = Math.max(1, Math.min(50, s));
        this.tiers = enums(RarityTier.class, p.get("tier"));
        this.position = enumOr(Position.class, upper(first(p, "position")), null);
        this.ageMin = intOrNull(first(p, "ageMin")); this.ageMax = intOrNull(first(p, "ageMax"));
        this.contractStatus = enumOr(ContractStatus.class, upper(first(p, "contractStatus")), null);
        this.period = enumOr(Period.class, upper(first(p, "period")), null);
        this.metricTypes = enums(MetricType.class, p.get("metricType"));
        this.eventTypes = enums(EventType.class, p.get("eventType"));
        this.athleteId = uuid(first(p, "athleteId"));
        List<AttributeFilter> attrs = new ArrayList<>();
        for (var e : p.entrySet()) {
            Matcher m = ATTR.matcher(e.getKey());
            if (!m.matches()) continue;
            for (String v : e.getValue()) {
                Matcher o = OPV.matcher(v == null ? "" : v.trim());
                if (o.matches()) attrs.add(new AttributeFilter(m.group(1).toLowerCase(), o.group(1), Double.parseDouble(o.group(2))));
            }
        }
        this.attributes = List.copyOf(attrs);
        List<UUID> cmp = new ArrayList<>();
        for (String raw : split(p.get("compare"))) { UUID u = uuid(raw); if (u != null) cmp.add(u); }
        this.compare = List.copyOf(cmp);
        String src = upper(first(p, "source"));
        this.source = window == FeedWindow.SCOUTING ? enumOr(ReportSource.class, src, null) : null;
        this.severities = enums(AlertSeverity.class, p.get("severity"));
        this.alertTypes = enums(AlertType.class, p.get("alertType"));
        this.sources = window == FeedWindow.ALERTA ? enums(UserRole.class, p.get("source")) : List.of();
    }

    public static FeedQuery parse(FeedWindow window, MultiValueMap<String, String> params) { return new FeedQuery(window, params); }

    public Duration periodDuration() {
        if (period == null) return null;
        return switch (period) { case WEEK -> Duration.ofDays(7); case MONTH -> Duration.ofDays(30); case SEASON -> Duration.ofDays(365); };
    }

    /** Eco dos filtros aplicados (chips da filterbar) e chave de cache. */
    public Map<String, Object> applied() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sort", sort.name());
        if (!roles.isEmpty()) m.put("role", roles.stream().map(Enum::name).toList());
        if (state != null) m.put("state", state);
        if (clubId != null) m.put("clubId", clubId.toString());
        if (q != null) m.put("q", q);
        if (!tiers.isEmpty()) m.put("tier", tiers.stream().map(Enum::name).toList());
        if (position != null) m.put("position", position.name());
        if (ageMin != null) m.put("ageMin", ageMin); if (ageMax != null) m.put("ageMax", ageMax);
        if (contractStatus != null) m.put("contractStatus", contractStatus.name());
        if (period != null) m.put("period", period.name());
        if (!metricTypes.isEmpty()) m.put("metricType", metricTypes.stream().map(Enum::name).toList());
        if (!eventTypes.isEmpty()) m.put("eventType", eventTypes.stream().map(Enum::name).toList());
        if (athleteId != null) m.put("athleteId", athleteId.toString());
        if (!attributes.isEmpty()) m.put("attr", attributes.stream().map(a -> a.name() + " " + a.op() + " " + a.value()).toList());
        if (!compare.isEmpty()) m.put("compare", compare.stream().map(UUID::toString).toList());
        if (source != null) m.put("source", source.name());
        if (!severities.isEmpty()) m.put("severity", severities.stream().map(Enum::name).toList());
        if (!alertTypes.isEmpty()) m.put("alertType", alertTypes.stream().map(Enum::name).toList());
        if (!sources.isEmpty()) m.put("sourceRole", sources.stream().map(Enum::name).toList());
        m.put("size", size);
        return m;
    }

    public String cacheKey(UUID viewer) { return "feed:" + window + ":" + (viewer == null ? "anon" : viewer) + ":" + RedisSessionService.hash(applied().toString() + "|" + cursor); }

    /* helpers */
    private static String first(MultiValueMap<String, String> p, String k) { List<String> v = p.get(k); return v == null || v.isEmpty() ? null : v.get(0); }
    private static List<String> split(List<String> raw) {
        if (raw == null) return List.of();
        List<String> out = new ArrayList<>();
        for (String r : raw) if (r != null) for (String s : r.split(",")) if (!s.isBlank()) out.add(s.trim());
        return out;
    }
    private static <E extends Enum<E>> List<E> enums(Class<E> type, List<String> raw) {
        List<E> out = new ArrayList<>();
        for (String s : split(raw)) { E e = enumOr(type, s.toUpperCase(), null); if (e != null && !out.contains(e)) out.add(e); }
        return List.copyOf(out);
    }
    private static <E extends Enum<E>> E enumOr(Class<E> type, String s, E def) {
        if (s == null || s.isBlank()) return def;
        try { return Enum.valueOf(type, s.trim().toUpperCase()); } catch (IllegalArgumentException e) { return def; }
    }
    private static String upper(String s) { return s == null || s.isBlank() ? null : s.trim().toUpperCase(); }
    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
    private static UUID uuid(String s) { try { return s == null || s.isBlank() ? null : UUID.fromString(s.trim()); } catch (IllegalArgumentException e) { return null; } }
    private static int intOr(String s, int d) { try { return s == null ? d : Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return d; } }
    private static Integer intOrNull(String s) { try { return s == null || s.isBlank() ? null : Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; } }
}
