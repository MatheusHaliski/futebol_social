package com.footballai.social.model.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Atributos de scouting (0–100) materializados por semana — base dos filtros attr[velocidade]=gt:80. */
@Entity
@Table(name = "scouting_attributes", uniqueConstraints = @UniqueConstraint(columnNames = {"athlete_id", "iso_week"}))
public class ScoutingAttribute {
    public static final java.util.Set<String> NAMES = java.util.Set.of("velocidade", "passe", "finalizacao", "drible", "desarme", "posicionamento", "resistencia");

    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "athlete_id", nullable = false, columnDefinition = "BINARY(16)") private UUID athleteId;
    @Column(name = "iso_week", nullable = false, length = 8) private String isoWeek;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal velocidade = BigDecimal.ZERO;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal passe = BigDecimal.ZERO;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal finalizacao = BigDecimal.ZERO;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal drible = BigDecimal.ZERO;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal desarme = BigDecimal.ZERO;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal posicionamento = BigDecimal.ZERO;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal resistencia = BigDecimal.ZERO;

    public UUID getId() { return id; }
    public UUID getAthleteId() { return athleteId; }
    public void setAthleteId(UUID athleteId) { this.athleteId = athleteId; }
    public String getIsoWeek() { return isoWeek; }
    public void setIsoWeek(String isoWeek) { this.isoWeek = isoWeek; }
    public BigDecimal getVelocidade() { return velocidade; }
    public void setVelocidade(BigDecimal v) { this.velocidade = v; }
    public BigDecimal getPasse() { return passe; }
    public void setPasse(BigDecimal v) { this.passe = v; }
    public BigDecimal getFinalizacao() { return finalizacao; }
    public void setFinalizacao(BigDecimal v) { this.finalizacao = v; }
    public BigDecimal getDrible() { return drible; }
    public void setDrible(BigDecimal v) { this.drible = v; }
    public BigDecimal getDesarme() { return desarme; }
    public void setDesarme(BigDecimal v) { this.desarme = v; }
    public BigDecimal getPosicionamento() { return posicionamento; }
    public void setPosicionamento(BigDecimal v) { this.posicionamento = v; }
    public BigDecimal getResistencia() { return resistencia; }
    public void setResistencia(BigDecimal v) { this.resistencia = v; }
}
