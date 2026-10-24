package com.footballai.social.model.entity;

import com.footballai.social.config.CryptoConverter;
import com.footballai.social.model.enums.AccountStatus;
import com.footballai.social.model.enums.ProfileVisibility;
import com.footballai.social.model.enums.UserRole;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Raiz do cadastro polimórfico (RF2): herança JOINED, uma tabela por papel.
 * Nasce com visibilidade SOMENTE_EU e status PENDING_VERIFICATION (privacy by default).
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING, length = 32)
@EntityListeners(AuditingEntityListener.class)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    /** E-mail cifrado em repouso (AES-256-GCM); busca pelo hash. */
    @Convert(converter = CryptoConverter.class)
    @Column(name = "email_enc", nullable = false, length = 512)
    private String email;

    @Column(name = "email_hash", nullable = false, unique = true, length = 64)
    private String emailHash;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 280)
    private String bio;

    @Column(name = "avatar_key")
    private String avatarKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", insertable = false, updatable = false, length = 32)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AccountStatus status = AccountStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProfileVisibility visibility = ProfileVisibility.SOMENTE_EU;

    @Column(nullable = false, length = 5)
    private String locale = "pt-BR";

    @Column(name = "dark_mode", nullable = false)
    private boolean darkMode = false;

    @Column(name = "font_scale", nullable = false, precision = 2, scale = 1)
    private BigDecimal fontScale = BigDecimal.ONE;

    @Column(name = "country_code", length = 2)
    private String countryCode = "BR";

    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(length = 80)
    private String city;

    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;

    @Column(name = "scheduled_purge_at")
    private Instant scheduledPurgeAt;

    @Column(name = "anonymized_at")
    private Instant anonymizedAt;

    protected User() { }

    /** Papel do usuário — definido pela subclasse concreta (coluna discriminadora). */
    public abstract UserRole role();

    /* ---------- regras de domínio ---------- */

    public String ringColor() { return role().ringColor(); }

    public Integer age() {
        return birthDate == null ? null : Period.between(birthDate, LocalDate.now()).getYears();
    }

    public boolean isMinor() {
        Integer a = age();
        return a != null && a < 18;
    }

    public void activate() {
        if (status == AccountStatus.ANONYMIZED) throw new IllegalStateException("Conta anonimizada não pode ser reativada");
        this.status = AccountStatus.ACTIVE;
    }

    /** Art. 18, VI — soft delete com graça de 30 dias. */
    public void requestDeletion(Instant now, int graceDays) {
        if (status == AccountStatus.ANONYMIZED) throw new IllegalStateException("Conta já anonimizada");
        this.status = AccountStatus.PENDING_DELETE;
        this.deletionRequestedAt = now;
        this.scheduledPurgeAt = now.plus(graceDays, ChronoUnit.DAYS);
    }

    public void cancelDeletion() {
        if (status != AccountStatus.PENDING_DELETE) throw new IllegalStateException("Não há exclusão pendente");
        this.status = AccountStatus.ACTIVE;
        this.deletionRequestedAt = null;
        this.scheduledPurgeAt = null;
    }

    public boolean isPurgeDue(Instant now) {
        return status == AccountStatus.PENDING_DELETE && scheduledPurgeAt != null && !now.isBefore(scheduledPurgeAt);
    }

    /** Pseudonimização irreversível (job diário). */
    public void anonymize(Instant now) {
        UUID uid = getId() == null ? UUID.randomUUID() : getId();
        String shortId = uid.toString().substring(0, 8);
        this.email = "anon_" + shortId + "@anon.invalid";
        this.emailHash = "anon_" + uid;
        this.username = "anon_" + shortId;
        this.displayName = "Usuário removido";
        this.bio = null;
        this.avatarKey = null;
        this.city = null;
        this.birthDate = null;
        this.passwordHash = "!";
        this.status = AccountStatus.ANONYMIZED;
        this.anonymizedAt = now;
    }

    /* ---------- getters / setters ---------- */

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEmailHash() { return emailHash; }
    public void setEmailHash(String emailHash) { this.emailHash = emailHash; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getAvatarKey() { return avatarKey; }
    public void setAvatarKey(String avatarKey) { this.avatarKey = avatarKey; }
    public UserRole getRole() { return role != null ? role : role(); }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public ProfileVisibility getVisibility() { return visibility; }
    public void setVisibility(ProfileVisibility visibility) { this.visibility = visibility; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }
    public BigDecimal getFontScale() { return fontScale; }
    public void setFontScale(BigDecimal fontScale) { this.fontScale = fontScale; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getStateCode() { return stateCode; }
    public void setStateCode(String stateCode) { this.stateCode = stateCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getDeletionRequestedAt() { return deletionRequestedAt; }
    public Instant getScheduledPurgeAt() { return scheduledPurgeAt; }
    public Instant getAnonymizedAt() { return anonymizedAt; }
}
