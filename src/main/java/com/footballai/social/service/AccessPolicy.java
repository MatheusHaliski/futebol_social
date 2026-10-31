package com.footballai.social.service;

import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.model.entity.Athlete;
import com.footballai.social.model.entity.HealthStaff;
import com.footballai.social.model.entity.Post;
import com.footballai.social.model.entity.User;
import com.footballai.social.model.entity.UserLgpdPreferences;
import com.footballai.social.model.enums.AccountStatus;
import com.footballai.social.model.enums.PostContentType;
import com.footballai.social.model.enums.ProfileVisibility;
import com.footballai.social.model.enums.UserRole;
import com.footballai.social.repository.FollowRepository;
import com.footballai.social.repository.UserLgpdPreferencesRepository;
import com.footballai.social.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Regra única de acesso a dados de outra pessoa (LGPD Art. 11 e 14, ECA Digital):
 * <ul>
 *   <li><b>perfil e Gêmeo</b> (card, evolução, DNA, comparador): o dono; senão conta ATIVA e {@code PUBLICO} — menor de 18 só com
 *       consentimento do responsável — ou {@code CONEXOES}/{@code PUBLICO} para conexões (seguir mútuo);</li>
 *   <li><b>dado de saúde</b>: o próprio atleta, staff de saúde verificado, ou quem pode ver o perfil quando o atleta liga {@code shareMedicalData};</li>
 *   <li><b>boletim de lesão</b> ({@code LESAO_ALERTA}): o autor e quem pode ver a saúde do atleta-sujeito; sem sujeito, só o autor.</li>
 * </ul>
 * Fora dessas regras a API responde 404 (não revela que o perfil ou o boletim existe).
 */
@Service
public class AccessPolicy {
    private final UserRepository users;
    private final UserLgpdPreferencesRepository prefs;
    private final FollowRepository follows;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public AccessPolicy(UserRepository users, UserLgpdPreferencesRepository prefs, FollowRepository follows) { this(users, prefs, follows, Clock.systemUTC()); }
    AccessPolicy(UserRepository users, UserLgpdPreferencesRepository prefs, FollowRepository follows, Clock clock) {
        this.users = users; this.prefs = prefs; this.follows = follows; this.clock = clock;
    }

    /** Menor de 18 sem consentimento do responsável não aparece para o público geral (descoberta, ranking, FLAIR). */
    public static boolean openToPublic(User u) {
        return !(u instanceof Athlete a && a.isMinor() && a.getGuardianConsentAt() == null);
    }

    /** Categoria de base no lugar da idade exata de menores (minimização, ECA Digital): SUB-15, SUB-17 ou SUB-20. */
    public static String youthCategory(int age) {
        return age < 15 ? "SUB-15" : age < 17 ? "SUB-17" : "SUB-20";
    }

    public boolean isConnection(UUID viewer, UUID target) {
        return viewer != null && follows.existsByFollowerIdAndTargetId(viewer, target) && follows.existsByFollowerIdAndTargetId(target, viewer);
    }

    public boolean canViewProfile(AuthPrincipal viewer, User target) {
        if (target == null) return false;
        if (viewer != null && viewer.userId().equals(target.getId())) return true;
        if (target.getStatus() != AccountStatus.ACTIVE) return false;
        return switch (target.getVisibility()) {
            case PUBLICO -> openToPublic(target) || (viewer != null && isConnection(viewer.userId(), target.getId()));
            case CONEXOES -> viewer != null && isConnection(viewer.userId(), target.getId());
            default -> false;
        };
    }

    /** Perfil visível ou 404 (mesma resposta para "não existe" e "não pode ver"). */
    public User requireProfile(AuthPrincipal viewer, UUID id, String what) {
        User u = users.findById(id).orElse(null);
        if (!canViewProfile(viewer, u)) throw ApiException.notFound(what);
        return u;
    }

    public boolean canSeeMedical(AuthPrincipal viewer, UUID athleteId) {
        if (viewer == null || athleteId == null) return false;
        if (viewer.userId().equals(athleteId)) return true;
        if (isVerifiedHealthStaff(viewer)) return true;   // só staff de saúde VERIFICADO/HOMOLOGADO (Art. 11, II); cadastro COMUNIDADE não basta
        if (!prefs.findById(athleteId).map(UserLgpdPreferences::isShareMedicalData).orElse(false)) return false;
        return canViewProfile(viewer, users.findById(athleteId).orElse(null));
    }

    public boolean isVerifiedHealthStaff(AuthPrincipal viewer) {
        return viewer != null && viewer.role() == UserRole.STAFF_SAUDE
                && users.findById(viewer.userId()).orElse(null) instanceof HealthStaff hs && hs.canOriginateInjuryRecord();
    }

    public boolean canSeePost(AuthPrincipal viewer, Post p) {
        if (p.getContentType() != PostContentType.LESAO_ALERTA) return true;
        if (viewer == null) return false;
        if (viewer.userId().equals(p.getAuthorId())) return true;
        return p.getSubjectAthleteId() != null && canSeeMedical(viewer, p.getSubjectAthleteId());
    }

    /**
     * A mesma regra de {@link #canSeePost} como filtro de consulta dos feeds: boletins de lesão só do próprio autor, sobre o próprio
     * leitor, ou de atletas que compartilham saúde e cujo perfil o leitor pode ver. Demais tipos passam.
     */
    public Specification<Post> visiblePosts(AuthPrincipal viewer) {
        boolean staff = isVerifiedHealthStaff(viewer);
        Set<UUID> connections = viewer == null ? Set.of() : new HashSet<>(follows.findConnectionIds(viewer.userId()));
        LocalDate adultBorn = LocalDate.now(clock).minusYears(18);
        return (root, cq, cb) -> {
            if (staff) return cb.conjunction();
            Predicate notInjury = cb.notEqual(root.get("contentType"), PostContentType.LESAO_ALERTA);
            if (viewer == null) return notInjury;
            var subject = root.<UUID>get("subjectAthleteId");
            Subquery<UUID> sharing = cq.subquery(UUID.class);
            var pr = sharing.from(UserLgpdPreferences.class);
            sharing.select(pr.get("userId")).where(cb.isTrue(pr.get("shareMedicalData")));
            Subquery<UUID> open = cq.subquery(UUID.class);   // perfil aberto ao público (menor só com consentimento do responsável)
            var a = open.from(Athlete.class);
            open.select(a.get("id")).where(cb.equal(a.get("status"), AccountStatus.ACTIVE), cb.equal(a.get("visibility"), ProfileVisibility.PUBLICO),
                    cb.or(cb.lessThanOrEqualTo(a.get("birthDate"), adultBorn), cb.isNotNull(a.get("guardianConsentAt"))));
            Predicate profileVisible = cb.in(subject).value(open);
            if (!connections.isEmpty()) {
                Subquery<UUID> conn = cq.subquery(UUID.class);
                var c = conn.from(Athlete.class);
                conn.select(c.get("id")).where(c.get("id").in(connections), cb.equal(c.get("status"), AccountStatus.ACTIVE),
                        c.get("visibility").in(List.of(ProfileVisibility.PUBLICO, ProfileVisibility.CONEXOES)));
                profileVisible = cb.or(profileVisible, cb.in(subject).value(conn));
            }
            return cb.or(notInjury, cb.equal(root.get("authorId"), viewer.userId()), cb.equal(subject, viewer.userId()),
                    cb.and(cb.in(subject).value(sharing), profileVisible));
        };
    }
}
