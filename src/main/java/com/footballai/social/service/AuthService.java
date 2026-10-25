package com.footballai.social.service;

import com.footballai.social.config.AppProperties;
import com.footballai.social.config.AuthPrincipal;
import com.footballai.social.controller.dto.AuthDtos.*;
import com.footballai.social.model.entity.*;
import com.footballai.social.model.enums.*;
import com.footballai.social.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * RF1 + RF2: cadastro polimórfico com aceite versionado dos termos Velion, login com bloqueio por tentativas,
 * sessões Redis, access 15 min + refresh 7 d com rotação, logout (blacklist jti) e logout de todos os dispositivos.
 */
@Service
public class AuthService {

    private final UserRepository users;
    private final TermsVersionRepository termsVersions;
    private final TermsAcceptanceRepository acceptances;
    private final UserLgpdPreferencesRepository prefs;
    private final DigitalTwinRepository twins;
    private final PasswordEncoder encoder;
    private final JwtTokenService jwt;
    private final RedisSessionService sessions;
    private final UserService userService;
    private final AppProperties props;
    private final Clock clock;

    public AuthService(UserRepository users, TermsVersionRepository termsVersions, TermsAcceptanceRepository acceptances, UserLgpdPreferencesRepository prefs,
                       DigitalTwinRepository twins, PasswordEncoder encoder, JwtTokenService jwt, RedisSessionService sessions, UserService userService, AppProperties props) {
        this.users = users; this.termsVersions = termsVersions; this.acceptances = acceptances; this.prefs = prefs; this.twins = twins;
        this.encoder = encoder; this.jwt = jwt; this.sessions = sessions; this.userService = userService; this.props = props; this.clock = Clock.systemUTC();
    }

    /* ---------------- termos ---------------- */

    public TermsDTO currentTerms() {
        return termsVersions.findFirstByCurrentTrueOrderByEffectiveFromDesc()
                .map(t -> new TermsDTO(t.getVersion(), t.getOwner(), t.getDocumentUrl(), t.getPrivacyPolicyUrl(), t.getEffectiveFrom()))
                .orElse(new TermsDTO(props.terms().currentVersion(), "Velion (Orange Social)", "<PREENCHER_URL_TERMOS>", "<PREENCHER_URL_PRIVACIDADE>", null));
    }

    private String currentTermsVersion() { return currentTerms().version(); }

    @Transactional
    public void acceptTerms(UUID userId, String version, String ip, String ua) {
        if (!currentTermsVersion().equals(version)) throw ApiException.badRequest("TERMS_VERSION_STALE", "Versão dos termos não é a vigente (" + currentTermsVersion() + ")");
        if (!acceptances.existsByUserIdAndTermsVersion(userId, version)) acceptances.save(new TermsAcceptance(userId, version, clock.instant(), ip, ua));
    }

    /* ---------------- cadastro ---------------- */

    @Transactional
    public AuthResponse register(RegisterRequest r, String ip, String ua) {
        if (r.role() == UserRole.ARBITRO || r.role() == UserRole.TORCIDA_ORGANIZADA)
            throw ApiException.badRequest("ROLE_RESERVED", "Papel reservado — habilitado via RoleRegistry por agentes");
        if (!r.acceptTerms() || !currentTermsVersion().equals(r.termsVersion()))
            throw new ApiException(HttpStatus.PRECONDITION_REQUIRED, "TERMS_REQUIRED", "É obrigatório aceitar os Termos de Uso e a Política de Privacidade da Velion (versão " + currentTermsVersion() + ")");
        String emailHash = RedisSessionService.hash(r.email().trim().toLowerCase());
        if (users.existsByEmailHash(emailHash)) throw ApiException.conflict("EMAIL_IN_USE", "E-mail já cadastrado");
        if (users.existsByUsername(r.username())) throw ApiException.conflict("USERNAME_IN_USE", "Username indisponível");
        if (r.birthDate() != null && Period.between(r.birthDate(), LocalDate.now(clock)).getYears() < 13)
            throw ApiException.forbidden("MIN_AGE", "Idade mínima de 13 anos (Art. 14)");
        if (r.role() == UserRole.ATLETA && r.birthDate() == null) throw ApiException.badRequest("BIRTH_DATE_REQUIRED", "Atletas precisam informar a data de nascimento");

        User u = newUserFor(r.role(), r.profile() == null ? Map.of() : r.profile());
        u.setEmail(r.email().trim().toLowerCase()); u.setEmailHash(emailHash);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setUsername(r.username()); u.setDisplayName(r.displayName()); u.setBirthDate(r.birthDate());
        if (r.stateCode() != null) u.setStateCode(r.stateCode().toUpperCase());
        u.setCity(r.city());
        u.setStatus(AccountStatus.ACTIVE); // verificação de e-mail gate apenas publicação/verificado
        u = users.save(u);

        acceptances.save(new TermsAcceptance(u.getId(), r.termsVersion(), clock.instant(), ip, ua));
        prefs.save(new UserLgpdPreferences(u.getId())); // tudo FALSE
        if (u.getRole().hasDigitalTwin()) {
            DigitalTwin twin = new DigitalTwin(u.getId());
            if (!u.isMinor()) twin.transitionTo(TwinStatus.ACTIVE, clock.instant()); // menores: DRAFT até consentimento do responsável
            twins.save(twin);
        }
        return issue(u, ip, ua);
    }

    private User newUserFor(UserRole role, Map<String, String> p) {
        switch (role) {
            case ATLETA -> {
                Athlete a = new Athlete();
                if (p.get("position") != null) a.setPosition(Position.valueOf(p.get("position").toUpperCase()));
                a.setDominantFoot(p.get("dominantFoot"));
                if (p.get("heightCm") != null) a.setHeightCm(Integer.parseInt(p.get("heightCm")));
                if (p.get("weightKg") != null) a.setWeightKg(new BigDecimal(p.get("weightKg")));
                a.setCurrentClubName(p.get("clubName"));
                if (p.get("clubId") != null) a.setCurrentClubId(UUID.fromString(p.get("clubId")));
                if (p.get("contractStatus") != null) a.setContractStatus(ContractStatus.valueOf(p.get("contractStatus").toUpperCase()));
                a.setCategoria(p.get("categoria"));
                return a;
            }
            case STAFF -> {
                Staff s = new Staff();
                if (p.get("specialty") != null) s.setSpecialty(StaffSpecialty.valueOf(p.get("specialty").toUpperCase()));
                s.setCbfLicense(p.get("cbfLicense")); s.setLicenseLevel(p.get("licenseLevel"));
                s.setHeadCoach(Boolean.parseBoolean(p.getOrDefault("headCoach", "false")));
                return s;
            }
            case STAFF_SAUDE -> {
                HealthStaff h = new HealthStaff();
                if (p.get("specialty") != null) h.setSpecialty(HealthSpecialty.valueOf(p.get("specialty").toUpperCase()));
                h.setProfessionalRegistry(p.get("professionalRegistry")); h.setRegistryUf(p.get("registryUf"));
                return h;
            }
            case CLUBE -> {
                Club c = new Club();
                c.setLegalName(p.get("legalName")); c.setFederationCode(p.get("federationCode"));
                if (p.get("clubeTipo") != null) c.setClubeTipo(ClubeTipo.valueOf(p.get("clubeTipo").toUpperCase()));
                if (p.get("division") != null) c.setDivision(Integer.parseInt(p.get("division")));
                if (p.get("foundedYear") != null) c.setFoundedYear(Integer.parseInt(p.get("foundedYear")));
                c.setStadium(p.get("stadium"));
                if (p.get("lat") != null) c.setLat(new BigDecimal(p.get("lat")));
                if (p.get("lng") != null) c.setLng(new BigDecimal(p.get("lng")));
                return c;
            }
            case SCOUT -> { Scout s = new Scout(); s.setAgency(p.get("agency")); s.setRegionsOfInterest(p.get("regionsOfInterest")); return s; }
            case AGENTE -> { Agent a = new Agent(); a.setFifaLicenseId(p.get("fifaLicenseId")); a.setAgencyName(p.get("agencyName")); return a; }
            case MARCA -> {
                Brand b = new Brand(); b.setTradeName(p.get("tradeName")); b.setSegment(p.get("segment")); b.setWebsite(p.get("website")); b.setCnpj(p.get("cnpj"));
                if (p.get("categoria") != null) b.setCategoria(MarcaCategoria.valueOf(p.get("categoria").toUpperCase()));
                return b;
            }
            case IMPRENSA -> {
                Press pr = new Press(); pr.setOutlet(p.get("outlet")); pr.setPressCardId(p.get("pressCardId")); pr.setBeat(p.get("beat"));
                if (p.get("midiaTipo") != null) pr.setMidiaTipo(MidiaTipo.valueOf(p.get("midiaTipo").toUpperCase()));
                return pr;
            }
            case OFICIAL_LIGA -> {
                LeagueOfficial l = new LeagueOfficial(); l.setEntityName(p.get("entityName")); l.setJurisdiction(p.get("jurisdiction"));
                if (p.get("entidadeTipo") != null) l.setEntidadeTipo(EntidadeTipo.valueOf(p.get("entidadeTipo").toUpperCase()));
                return l;
            }
            case TORCEDOR -> { Supporter s = new Supporter(); if (p.get("favoriteClubId") != null) s.setFavoriteClubId(UUID.fromString(p.get("favoriteClubId"))); return s; }
            case AGENTE_IA -> {
                if (p.get("operatedBy") == null) throw ApiException.badRequest("OPERATOR_REQUIRED", "Agente de IA exige operatedBy (usuário responsável)");
                AiAgent ai = new AiAgent(); ai.setOperatedBy(UUID.fromString(p.get("operatedBy"))); ai.setModelId(p.get("modelId"));
                ai.setPurpose(p.get("purpose")); ai.setDisclosureVersion(p.getOrDefault("disclosureVersion", "1.0"));
                return ai;
            }
            default -> throw ApiException.badRequest("ROLE_UNSUPPORTED", "Papel não suportado: " + role);
        }
    }

    /* ---------------- login / refresh / logout ---------------- */

    @Transactional
    public AuthResponse login(LoginRequest r, String ip, String ua) {
        String emailHash = RedisSessionService.hash(r.email().trim().toLowerCase());
        if (sessions.loginFailures(emailHash) >= props.rateLimit().maxLoginFailures())
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_LOCKED", "Muitas tentativas — aguarde 15 minutos");
        Optional<User> found = users.findByEmailHash(emailHash);
        if (found.isEmpty() || !encoder.matches(r.password(), found.get().getPasswordHash())) {
            sessions.recordLoginFailure(emailHash);
            throw ApiException.unauthorized("BAD_CREDENTIALS", "E-mail ou senha inválidos");
        }
        User u = found.get();
        switch (u.getStatus()) {
            case ANONYMIZED -> throw ApiException.forbidden("ACCOUNT_ANONYMIZED", "Conta encerrada");
            case SUSPENDED -> throw ApiException.forbidden("ACCOUNT_SUSPENDED", "Conta suspensa");
            case PENDING_DELETE -> { u.cancelDeletion(); twins.findByUserId(u.getId()).ifPresent(t -> { if (t.getStatus() == TwinStatus.PENDING_DELETE) t.transitionTo(TwinStatus.ACTIVE, clock.instant()); }); users.save(u); }
            default -> { }
        }
        if (!acceptances.existsByUserIdAndTermsVersion(u.getId(), currentTermsVersion()))
            throw new ApiException(HttpStatus.PRECONDITION_REQUIRED, "TERMS_REQUIRED", "Há uma nova versão dos Termos (" + currentTermsVersion() + ") pendente de aceite");
        sessions.clearLoginFailures(emailHash);
        return issue(u, ip, ua);
    }

    public AuthResponse refresh(String refreshToken) {
        RedisSessionService.Session s = sessions.rotateRefresh(refreshToken)
                .orElseThrow(() -> ApiException.unauthorized("REFRESH_INVALID", "Refresh token inválido, expirado ou reutilizado — sessão encerrada"));
        User u = userService.require(s.userId());
        if (u.getStatus() == AccountStatus.ANONYMIZED || u.getStatus() == AccountStatus.SUSPENDED) { sessions.revokeSession(u.getId(), s.sessionId()); throw ApiException.forbidden("ACCOUNT_BLOCKED", "Conta bloqueada"); }
        JwtTokenService.IssuedToken at = jwt.issueAccessToken(u.getId(), u.getRole(), s.sessionId());
        return new AuthResponse("Bearer", at.token(), jwt.accessTtlSeconds(), s.refreshToken(), s.sessionId(), userService.summary(u));
    }

    public void logout(AuthPrincipal p) {
        sessions.blacklist(p.jti(), p.expiresAt());
        sessions.revokeSession(p.userId(), p.sessionId());
    }

    public void logoutAll(AuthPrincipal p) {
        sessions.blacklist(p.jti(), p.expiresAt());
        sessions.revokeAllSessions(p.userId());
    }

    private AuthResponse issue(User u, String ip, String ua) {
        RedisSessionService.Session s = sessions.createSession(u.getId(), ip, ua);
        JwtTokenService.IssuedToken at = jwt.issueAccessToken(u.getId(), u.getRole(), s.sessionId());
        return new AuthResponse("Bearer", at.token(), jwt.accessTtlSeconds(), s.refreshToken(), s.sessionId(), userService.summary(u));
    }

    public boolean checkPassword(UUID userId, String password) {
        return encoder.matches(password, userService.require(userId).getPasswordHash());
    }

    public Instant now() { return clock.instant(); }
}
