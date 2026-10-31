package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.model.entity.Athlete;
import com.footballai.social.model.entity.HealthStaff;
import com.footballai.social.model.entity.Post;
import com.footballai.social.model.entity.Supporter;
import com.footballai.social.model.entity.User;
import com.footballai.social.model.entity.UserLgpdPreferences;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.FollowRepository;
import com.footballai.social.repository.UserLgpdPreferencesRepository;
import com.footballai.social.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Matriz de acesso (papel × consentimento × visibilidade × idade): perfil/Gêmeo, dado de saúde e boletim de lesão.
 * Cobre os P0 #3 (lesão visível a todos), #4 (Gêmeo sem checagem) e #5 (menor público sem consentimento) do estudo.
 */
class AccessPolicyTest {
    private final UserRepository users = mock(UserRepository.class);
    private final UserLgpdPreferencesRepository prefs = mock(UserLgpdPreferencesRepository.class);
    private final FollowRepository follows = mock(FollowRepository.class);
    private final AccessPolicy policy = new AccessPolicy(users, prefs, follows, Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC));

    private final UUID viewerId = UUID.randomUUID();
    private final AuthPrincipal viewer = principal(viewerId, UserRole.TORCEDOR);

    private static AuthPrincipal principal(UUID id, UserRole role) { return new AuthPrincipal(id, role, "s", "j", Instant.MAX); }

    private <T extends User> T save(T u, ProfileVisibility vis, int age) {
        ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
        u.setStatus(AccountStatus.ACTIVE); u.setVisibility(vis); u.setBirthDate(LocalDate.now().minusYears(age).minusDays(10));
        when(users.findById(u.getId())).thenReturn(Optional.of(u));
        return u;
    }

    private void shareMedical(UUID id) {
        UserLgpdPreferences p = new UserLgpdPreferences(id);
        p.set(LgpdPreference.SHARE_MEDICAL_DATA, true, Instant.now());
        when(prefs.findById(id)).thenReturn(Optional.of(p));
    }

    private void connect(UUID a, UUID b) {
        when(follows.existsByFollowerIdAndTargetId(a, b)).thenReturn(true);
        when(follows.existsByFollowerIdAndTargetId(b, a)).thenReturn(true);
    }

    @Test
    void profileFollowsVisibilityStatusAndAge() {
        Athlete adultPublic = save(new Athlete(), ProfileVisibility.PUBLICO, 22);
        Athlete adultPrivate = save(new Athlete(), ProfileVisibility.SOMENTE_EU, 22);
        Athlete adultConnections = save(new Athlete(), ProfileVisibility.CONEXOES, 22);
        Athlete minorNoConsent = save(new Athlete(), ProfileVisibility.PUBLICO, 17);
        Athlete minorConsent = save(new Athlete(), ProfileVisibility.PUBLICO, 17);
        minorConsent.setGuardianConsentAt(Instant.now());

        assertThat(policy.canViewProfile(viewer, adultPublic)).isTrue();
        assertThat(policy.canViewProfile(null, adultPublic)).isTrue();
        assertThat(policy.canViewProfile(viewer, adultPrivate)).isFalse();
        assertThat(policy.canViewProfile(principal(adultPrivate.getId(), UserRole.ATLETA), adultPrivate)).isTrue();   // o dono sempre
        assertThat(policy.canViewProfile(viewer, adultConnections)).isFalse();
        connect(viewerId, adultConnections.getId());
        assertThat(policy.canViewProfile(viewer, adultConnections)).isTrue();

        // menor público sem consentimento do responsável: fora do público geral, visível para conexões
        assertThat(policy.canViewProfile(viewer, minorNoConsent)).isFalse();
        assertThat(policy.canViewProfile(null, minorNoConsent)).isFalse();
        connect(viewerId, minorNoConsent.getId());
        assertThat(policy.canViewProfile(viewer, minorNoConsent)).isTrue();
        assertThat(policy.canViewProfile(viewer, minorConsent)).isTrue();

        adultPublic.setStatus(AccountStatus.PENDING_DELETE);
        assertThat(policy.canViewProfile(viewer, adultPublic)).isFalse();
        assertThatThrownBy(() -> policy.requireProfile(viewer, adultPrivate.getId(), "Gêmeo")).isInstanceOf(ApiException.class).hasMessageContaining("Gêmeo");
        assertThatThrownBy(() -> policy.requireProfile(viewer, UUID.randomUUID(), "Gêmeo")).isInstanceOf(ApiException.class);
    }

    @Test
    void medicalDataNeedsConsentAndAVisibleProfile() {
        Athlete sharingPublic = save(new Athlete(), ProfileVisibility.PUBLICO, 24);
        Athlete sharingPrivate = save(new Athlete(), ProfileVisibility.SOMENTE_EU, 24);
        Athlete notSharing = save(new Athlete(), ProfileVisibility.PUBLICO, 24);
        shareMedical(sharingPublic.getId()); shareMedical(sharingPrivate.getId());
        when(prefs.findById(notSharing.getId())).thenReturn(Optional.empty());

        assertThat(policy.canSeeMedical(viewer, sharingPublic.getId())).isTrue();
        assertThat(policy.canSeeMedical(viewer, sharingPrivate.getId())).isFalse();   // consentiu, mas o perfil é privado
        assertThat(policy.canSeeMedical(viewer, notSharing.getId())).isFalse();
        assertThat(policy.canSeeMedical(null, sharingPublic.getId())).isFalse();
        assertThat(policy.canSeeMedical(principal(notSharing.getId(), UserRole.ATLETA), notSharing.getId())).isTrue();

        HealthStaff verified = save(new HealthStaff(), ProfileVisibility.SOMENTE_EU, 40);
        verified.setVerificationLevel(VerificationLevel.VERIFICADO);
        HealthStaff community = save(new HealthStaff(), ProfileVisibility.SOMENTE_EU, 40);
        community.setVerificationLevel(VerificationLevel.COMUNIDADE);
        assertThat(policy.canSeeMedical(principal(verified.getId(), UserRole.STAFF_SAUDE), notSharing.getId())).isTrue();
        assertThat(policy.canSeeMedical(principal(community.getId(), UserRole.STAFF_SAUDE), notSharing.getId())).isFalse();
    }

    @Test
    void injuryPostFollowsTheSubjectsHealthRule() {
        Athlete subject = save(new Athlete(), ProfileVisibility.PUBLICO, 24);
        when(prefs.findById(subject.getId())).thenReturn(Optional.empty());
        UUID medic = UUID.randomUUID();
        Post injury = new Post(); injury.setContentType(PostContentType.LESAO_ALERTA); injury.setAuthorId(medic); injury.setSubjectAthleteId(subject.getId());
        Post training = new Post(); training.setContentType(PostContentType.TREINO); training.setAuthorId(medic);

        assertThat(policy.canSeePost(viewer, training)).isTrue();
        assertThat(policy.canSeePost(null, training)).isTrue();
        assertThat(policy.canSeePost(viewer, injury)).isFalse();      // P0 #3: antes aparecia para qualquer usuário
        assertThat(policy.canSeePost(null, injury)).isFalse();
        assertThat(policy.canSeePost(principal(medic, UserRole.STAFF_SAUDE), injury)).isTrue();          // o autor
        assertThat(policy.canSeePost(principal(subject.getId(), UserRole.ATLETA), injury)).isTrue();     // o sujeito
        shareMedical(subject.getId());
        assertThat(policy.canSeePost(viewer, injury)).isTrue();       // o atleta compartilha dados de saúde

        Post orphan = new Post(); orphan.setContentType(PostContentType.LESAO_ALERTA); orphan.setAuthorId(medic);
        assertThat(policy.canSeePost(viewer, orphan)).isFalse();      // sem sujeito: só o autor
        assertThat(policy.canSeePost(principal(medic, UserRole.STAFF_SAUDE), orphan)).isTrue();
    }

    @Test
    void minorsShowCategoryInsteadOfExactAge() {
        assertThat(AccessPolicy.youthCategory(13)).isEqualTo("SUB-15");
        assertThat(AccessPolicy.youthCategory(16)).isEqualTo("SUB-17");
        assertThat(AccessPolicy.youthCategory(17)).isEqualTo("SUB-20");
        Supporter adultFan = save(new Supporter(), ProfileVisibility.PUBLICO, 30);
        assertThat(AccessPolicy.openToPublic(adultFan)).isTrue();
        when(follows.existsByFollowerIdAndTargetId(any(), any())).thenReturn(false);
        assertThat(policy.isConnection(viewerId, adultFan.getId())).isFalse();
    }
}
