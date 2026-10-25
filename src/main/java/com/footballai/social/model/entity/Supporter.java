package com.footballai.social.model.entity;

import com.footballai.social.model.enums.UserRole;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

/** Torcedor — acompanha, reage e coleciona cards (FLAIR); não possui Gêmeo Digital. */
@Entity
@Table(name = "user_supporters")
@DiscriminatorValue("TORCEDOR")
@PrimaryKeyJoinColumn(name = "user_id")
public class Supporter extends User {
    @Column(name = "favorite_club_id", columnDefinition = "BINARY(16)") private UUID favoriteClubId;
    @Column(name = "member_since") private LocalDate memberSince;

    @Override public UserRole role() { return UserRole.TORCEDOR; }
    public UUID getFavoriteClubId() { return favoriteClubId; }
    public void setFavoriteClubId(UUID favoriteClubId) { this.favoriteClubId = favoriteClubId; }
    public LocalDate getMemberSince() { return memberSince; }
    public void setMemberSince(LocalDate memberSince) { this.memberSince = memberSince; }
}
