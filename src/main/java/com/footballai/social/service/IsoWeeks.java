package com.footballai.social.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

/** Semana ISO no formato {@code 2026-W43} (chave de idempotência do Rarity Engine). */
public final class IsoWeeks {
    private IsoWeeks() { }

    public static String of(LocalDate date) {
        return String.format("%d-W%02d", date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    public static String previous(LocalDate today) { return of(today.minusWeeks(1)); }

    /** [segunda, domingo] da semana ISO. */
    public static LocalDate[] range(String isoWeek) {
        String[] p = isoWeek.split("-W");
        int year = Integer.parseInt(p[0]), week = Integer.parseInt(p[1]);
        LocalDate monday = LocalDate.of(year, 1, 4).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new LocalDate[]{monday, monday.plusDays(6)};
    }

    public static String season(LocalDate date) { return String.valueOf(date.getYear()); }
}
