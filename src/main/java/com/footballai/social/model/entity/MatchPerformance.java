package com.footballai.social.model.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Desempenho em partida (30 % do Rarity), fonte das estatísticas da temporada do card e do histórico da Vitrine (passes e dribles certos). */
@Entity
@Table(name = "match_performances")
public class MatchPerformance {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(columnDefinition = "BINARY(16)") private UUID id;
    @Column(name = "athlete_id", nullable = false, columnDefinition = "BINARY(16)") private UUID athleteId;
    @Column(name = "match_id", columnDefinition = "BINARY(16)") private UUID matchId;
    @Column(name = "match_date", nullable = false) private LocalDate matchDate;
    @Column(length = 80) private String competition;
    @Column(nullable = false) private boolean official = true;
    @Column(name = "minutes_played", nullable = false) private int minutesPlayed;
    @Column(precision = 4, scale = 2) private BigDecimal rating;
    @Column(nullable = false) private int goals;
    @Column(nullable = false) private int assists;
    @Column(name = "passes_completed", nullable = false) private int passesCompleted;
    @Column(name = "passes_attempted", nullable = false) private int passesAttempted;
    @Column(name = "dribbles_completed", nullable = false) private int dribblesCompleted;
    @Column(name = "dribbles_attempted", nullable = false) private int dribblesAttempted;
    @Column(name = "clean_sheet", nullable = false) private boolean cleanSheet;
    @Column(nullable = false) private int tackles;
    @Column(name = "yellow_cards", nullable = false) private int yellowCards;
    @Column(name = "red_cards", nullable = false) private int redCards;
    @Column(nullable = false) private boolean mvp;
    @Column(nullable = false, length = 9) private String season;

    public UUID getId() { return id; }
    public UUID getAthleteId() { return athleteId; }
    public void setAthleteId(UUID athleteId) { this.athleteId = athleteId; }
    public UUID getMatchId() { return matchId; }
    public void setMatchId(UUID matchId) { this.matchId = matchId; }
    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }
    public String getCompetition() { return competition; }
    public void setCompetition(String competition) { this.competition = competition; }
    public boolean isOfficial() { return official; }
    public void setOfficial(boolean official) { this.official = official; }
    public int getMinutesPlayed() { return minutesPlayed; }
    public void setMinutesPlayed(int minutesPlayed) { this.minutesPlayed = minutesPlayed; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public int getGoals() { return goals; }
    public void setGoals(int goals) { this.goals = goals; }
    public int getAssists() { return assists; }
    public void setAssists(int assists) { this.assists = assists; }
    public int getPassesCompleted() { return passesCompleted; }
    public void setPassesCompleted(int passesCompleted) { this.passesCompleted = passesCompleted; }
    public int getPassesAttempted() { return passesAttempted; }
    public void setPassesAttempted(int passesAttempted) { this.passesAttempted = passesAttempted; }
    public int getDribblesCompleted() { return dribblesCompleted; }
    public void setDribblesCompleted(int dribblesCompleted) { this.dribblesCompleted = dribblesCompleted; }
    public int getDribblesAttempted() { return dribblesAttempted; }
    public void setDribblesAttempted(int dribblesAttempted) { this.dribblesAttempted = dribblesAttempted; }
    public boolean isCleanSheet() { return cleanSheet; }
    public void setCleanSheet(boolean cleanSheet) { this.cleanSheet = cleanSheet; }
    public int getTackles() { return tackles; }
    public void setTackles(int tackles) { this.tackles = tackles; }
    public int getYellowCards() { return yellowCards; }
    public void setYellowCards(int yellowCards) { this.yellowCards = yellowCards; }
    public int getRedCards() { return redCards; }
    public void setRedCards(int redCards) { this.redCards = redCards; }
    public boolean isMvp() { return mvp; }
    public void setMvp(boolean mvp) { this.mvp = mvp; }
    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
}
