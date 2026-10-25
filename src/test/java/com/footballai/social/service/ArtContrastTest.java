package com.footballai.social.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contraste da arte de fundo (styles.css · ".pcard.has-art"): a tinta trava a luminância em OKLCH — L .25 a .33 e croma ≤ .11 no tema
 * escuro; L .90 a .94 e croma ≤ .06 no claro. Este teste refaz a conta para todas as cores da paleta e cores extremas, inclusive com o
 * brilho mais forte dos materiais por cima (branco 10% no escuro, preto 12% no claro): título ≥ 7:1 e teaser/metadados ≥ 4,5:1 (WCAG AA).
 */
class ArtContrastTest {
    static final List<String> COLORS = List.of("#00d47a", "#3b82f6", "#f7c948", "#a06bff", "#ff9f43", "#ff5d5d", "#f5c542", "#2dd4bf",
            "#f472b6", "#22d3ee", "#5c6b62", "#eaf2ee", "#16181a", "#e11d48", "#ffffff", "#000000", "#ffff00");

    @Test
    void darkTintsKeepTitleAndTeaserReadable() {
        double[] text = rgb("#eaf2ee"), teaser = rgb("#b6c6bc");
        for (String c : COLORS) for (double l : new double[]{.25, .30, .33}) {
            double[] bg = over(lock(c, l, .11), new double[]{1, 1, 1}, .10);
            assertThat(contrast(bg, text)).as("título sobre %s L%.2f", c, l).isGreaterThanOrEqualTo(7);
            assertThat(contrast(bg, teaser)).as("teaser sobre %s L%.2f", c, l).isGreaterThanOrEqualTo(4.5);
        }
    }

    @Test
    void lightTintsKeepTitleAndTeaserReadable() {
        double[] text = rgb("#0e1512"), teaser = rgb("#3b4a42");
        for (String c : COLORS) for (double l : new double[]{.90, .91, .94}) {
            double[] bg = over(lock(c, l, .06), new double[]{0, 0, 0}, .12);
            assertThat(contrast(bg, text)).as("título sobre %s L%.2f", c, l).isGreaterThanOrEqualTo(7);
            assertThat(contrast(bg, teaser)).as("teaser sobre %s L%.2f", c, l).isGreaterThanOrEqualTo(4.5);
        }
    }

    // ---- OKLab (Björn Ottosson) e luminância relativa WCAG 2.x ----
    static double[] rgb(String hex) { return new double[]{Integer.parseInt(hex.substring(1, 3), 16) / 255.0, Integer.parseInt(hex.substring(3, 5), 16) / 255.0, Integer.parseInt(hex.substring(5, 7), 16) / 255.0}; }
    static double lin(double c) { return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4); }
    static double gam(double c) { c = Math.max(0, Math.min(1, c)); return c <= 0.0031308 ? 12.92 * c : 1.055 * Math.pow(c, 1 / 2.4) - 0.055; }

    /** Cor com a luminância OKLCH travada em {@code l} e croma limitado a {@code cMax} (o mesmo que oklch(from c l min(c,cMax) h)). */
    static double[] lock(String hex, double l, double cMax) {
        double[] s = rgb(hex); double r = lin(s[0]), g = lin(s[1]), b = lin(s[2]);
        double L = Math.cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b), M = Math.cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b),
               S = Math.cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b);
        double A = 1.9779984951 * L - 2.4285922050 * M + 0.4505937099 * S, B = 0.0259040371 * L + 0.7827717662 * M - 0.8086757660 * S;
        double c = Math.min(Math.hypot(A, B), cMax), h = Math.atan2(B, A); A = c * Math.cos(h); B = c * Math.sin(h);
        double l_ = Math.pow(l + 0.3963377774 * A + 0.2158037573 * B, 3), m_ = Math.pow(l - 0.1055613458 * A - 0.0638541728 * B, 3), s_ = Math.pow(l - 0.0894841775 * A - 1.2914855480 * B, 3);
        return new double[]{gam(4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_), gam(-1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_), gam(-0.0041960863 * l_ - 0.7034186147 * m_ + 1.7076147010 * s_)};
    }
    static double[] over(double[] bg, double[] top, double alpha) { return new double[]{bg[0] * (1 - alpha) + top[0] * alpha, bg[1] * (1 - alpha) + top[1] * alpha, bg[2] * (1 - alpha) + top[2] * alpha}; }
    static double lum(double[] c) { return 0.2126 * lin(c[0]) + 0.7152 * lin(c[1]) + 0.0722 * lin(c[2]); }
    static double contrast(double[] a, double[] b) { double x = lum(a), y = lum(b); return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05); }
}
