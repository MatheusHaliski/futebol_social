-- Football AI · Social — esquema inicial (MySQL 8, utf8mb4). Booleans como BIT(1), UUID como BINARY(16), Instant como DATETIME(6).
-- Privacy by design: todos os consentimentos DEFAULT FALSE; visibilidade padrão SOMENTE_EU.

CREATE TABLE users (
  id                      BINARY(16)    NOT NULL,
  role                    VARCHAR(32)   NOT NULL,
  email_enc               VARCHAR(512)  NOT NULL,
  email_hash              VARCHAR(64)   NOT NULL,
  password_hash           VARCHAR(72)   NOT NULL,
  username                VARCHAR(30)   NOT NULL,
  display_name            VARCHAR(80)   NOT NULL,
  birth_date              DATE          NULL,
  bio                     VARCHAR(280)  NULL,
  avatar_key              VARCHAR(255)  NULL,
  status                  VARCHAR(24)   NOT NULL DEFAULT 'PENDING_VERIFICATION',
  visibility              VARCHAR(16)   NOT NULL DEFAULT 'SOMENTE_EU',
  locale                  VARCHAR(5)    NOT NULL DEFAULT 'pt-BR',
  dark_mode               BIT(1)        NOT NULL DEFAULT b'0',
  font_scale              DECIMAL(2,1)  NOT NULL DEFAULT 1.0,
  country_code            VARCHAR(2)    NULL,
  state_code              VARCHAR(2)    NULL,
  city                    VARCHAR(80)   NULL,
  verified                BIT(1)        NOT NULL DEFAULT b'0',
  created_at              DATETIME(6)   NOT NULL,
  updated_at              DATETIME(6)   NULL,
  deletion_requested_at   DATETIME(6)   NULL,
  scheduled_purge_at      DATETIME(6)   NULL,
  anonymized_at           DATETIME(6)   NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email_hash (email_hash),
  UNIQUE KEY uk_users_username (username),
  KEY ix_users_status_purge (status, scheduled_purge_at),
  KEY ix_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE user_athletes (
  user_id             BINARY(16)   NOT NULL,
  position            VARCHAR(16)  NULL,
  dominant_foot       VARCHAR(5)   NULL,
  height_cm           INT          NULL,
  weight_kg           DECIMAL(5,2) NULL,
  current_club_id     BINARY(16)   NULL,
  current_club_name   VARCHAR(120) NULL,
  contract_status     VARCHAR(16)  NOT NULL DEFAULT 'AGENTE_LIVRE',
  contract_until      DATE         NULL,
  shirt_number        INT          NULL,
  is_captain          BIT(1)       NOT NULL DEFAULT b'0',
  categoria           VARCHAR(20)  NULL,
  guardian_consent_at DATETIME(6)  NULL,
  PRIMARY KEY (user_id),
  KEY ix_athletes_position (position),
  KEY ix_athletes_club (current_club_id),
  CONSTRAINT fk_athletes_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_staff (
  user_id         BINARY(16)  NOT NULL,
  specialty       VARCHAR(24) NULL,
  cbf_license     VARCHAR(40) NULL,
  license_level   VARCHAR(20) NULL,
  current_club_id BINARY(16)  NULL,
  is_head_coach   BIT(1)      NOT NULL DEFAULT b'0',
  PRIMARY KEY (user_id),
  CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_health_staff (
  user_id               BINARY(16)   NOT NULL,
  specialty             VARCHAR(24)  NULL,
  professional_registry VARCHAR(512) NULL,
  registry_uf           VARCHAR(2)   NULL,
  current_club_id       BINARY(16)   NULL,
  verification_level    VARCHAR(16)  NOT NULL DEFAULT 'COMUNIDADE',
  PRIMARY KEY (user_id),
  CONSTRAINT fk_health_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_clubs (
  user_id         BINARY(16)   NOT NULL,
  legal_name      VARCHAR(160) NULL,
  federation_code VARCHAR(40)  NULL,
  clube_tipo      VARCHAR(24)  NULL,
  division        INT          NULL,
  founded_year    INT          NULL,
  stadium         VARCHAR(120) NULL,
  crest_key       VARCHAR(255) NULL,
  verified_by     BINARY(16)   NULL,
  ccf_valid_until DATE         NULL,
  lat             DECIMAL(9,6) NULL,
  lng             DECIMAL(9,6) NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_clubs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_scouts (
  user_id             BINARY(16)   NOT NULL,
  agency              VARCHAR(120) NULL,
  regions_of_interest JSON         NULL,
  verification_level  VARCHAR(16)  NOT NULL DEFAULT 'COMUNIDADE',
  affiliation_id      BINARY(16)   NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_scouts_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_agents (
  user_id         BINARY(16)   NOT NULL,
  fifa_license_id VARCHAR(40)  NULL,
  license_status  VARCHAR(12)  NULL,
  agency_name     VARCHAR(120) NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_agents_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_brands (
  user_id                  BINARY(16)   NOT NULL,
  trade_name               VARCHAR(120) NULL,
  categoria                VARCHAR(16)  NULL,
  segment                  VARCHAR(60)  NULL,
  website                  VARCHAR(255) NULL,
  cnpj                     VARCHAR(18)  NULL,
  fazenda_authorization_id VARCHAR(60)  NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_brands_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_press (
  user_id                BINARY(16)   NOT NULL,
  outlet                 VARCHAR(120) NULL,
  midia_tipo             VARCHAR(24)  NULL,
  press_card_id          VARCHAR(40)  NULL,
  credential_issuer      VARCHAR(80)  NULL,
  credential_valid_until DATE         NULL,
  beat                   VARCHAR(60)  NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_press_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_league_officials (
  user_id       BINARY(16)   NOT NULL,
  entity_name   VARCHAR(120) NULL,
  entidade_tipo VARCHAR(24)  NULL,
  jurisdiction  VARCHAR(80)  NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_league_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_supporters (
  user_id          BINARY(16) NOT NULL,
  favorite_club_id BINARY(16) NULL,
  member_since     DATE       NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_supporters_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_ai_agents (
  user_id                BINARY(16)   NOT NULL,
  operated_by            BINARY(16)   NOT NULL,
  model_id               VARCHAR(80)  NULL,
  purpose                VARCHAR(160) NULL,
  disclosure_version     VARCHAR(20)  NULL,
  delegated_capabilities JSON         NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_ai_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE terms_versions (
  id                 BINARY(16)   NOT NULL,
  version            VARCHAR(20)  NOT NULL,
  owner              VARCHAR(80)  NOT NULL,
  document_url       VARCHAR(255) NOT NULL,
  privacy_policy_url VARCHAR(255) NOT NULL,
  effective_from     DATETIME(6)  NOT NULL,
  is_current         BIT(1)       NOT NULL DEFAULT b'0',
  PRIMARY KEY (id),
  UNIQUE KEY uk_terms_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE terms_acceptances (
  id            BINARY(16)   NOT NULL,
  user_id       BINARY(16)   NOT NULL,
  terms_version VARCHAR(20)  NOT NULL,
  accepted_at   DATETIME(6)  NOT NULL,
  ip_address    VARCHAR(45)  NULL,
  user_agent    VARCHAR(255) NULL,
  PRIMARY KEY (id),
  KEY ix_terms_acc_user (user_id, terms_version),
  CONSTRAINT fk_terms_acc_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_lgpd_preferences (
  user_id                     BINARY(16)  NOT NULL,
  share_telemetry_with_clubs  BIT(1)      NOT NULL DEFAULT b'0',
  share_telemetry_with_scouts BIT(1)      NOT NULL DEFAULT b'0',
  share_medical_data          BIT(1)      NOT NULL DEFAULT b'0',
  share_biometric_data        BIT(1)      NOT NULL DEFAULT b'0',
  location_history            BIT(1)      NOT NULL DEFAULT b'0',
  share_with_brands           BIT(1)      NOT NULL DEFAULT b'0',
  personalized_ads            BIT(1)      NOT NULL DEFAULT b'0',
  ai_agent_processing         BIT(1)      NOT NULL DEFAULT b'0',
  updated_at                  DATETIME(6) NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_lgpd_prefs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE consent_audit_log (
  id           BINARY(16)   NOT NULL,
  user_id      BINARY(16)   NOT NULL,
  preference   VARCHAR(40)  NOT NULL,
  old_value    BIT(1)       NOT NULL,
  new_value    BIT(1)       NOT NULL,
  lgpd_article VARCHAR(24)  NOT NULL,
  ip_address   VARCHAR(45)  NULL,
  user_agent   VARCHAR(255) NULL,
  changed_at   DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY ix_consent_user (user_id, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE digital_twins (
  id                   BINARY(16)  NOT NULL,
  user_id              BINARY(16)  NOT NULL,
  status               VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  physical_status      VARCHAR(16) NOT NULL DEFAULT 'READY',
  score_total          INT         NOT NULL DEFAULT 0,
  rarity_tier          VARCHAR(12) NOT NULL DEFAULT 'COMMON',
  reward_tier          VARCHAR(12) NOT NULL DEFAULT 'BRONZE',
  last_calculated_week VARCHAR(8)  NULL,
  activated_at         DATETIME(6) NULL,
  suspended_at         DATETIME(6) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_twin_user (user_id),
  KEY ix_twin_status_score (status, score_total),
  CONSTRAINT fk_twin_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE weekly_score_snapshots (
  id              BINARY(16)   NOT NULL,
  twin_id         BINARY(16)   NOT NULL,
  iso_week        VARCHAR(8)   NOT NULL,
  physical_score  DECIMAL(7,4) NOT NULL,
  technical_score DECIMAL(7,4) NOT NULL,
  match_score     DECIMAL(7,4) NOT NULL,
  badge_bonus     DECIMAL(7,4) NOT NULL,
  score_total     INT          NOT NULL,
  rarity_tier     VARCHAR(12)  NOT NULL,
  inputs_hash     VARCHAR(64)  NOT NULL,
  calculated_at   DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_snapshot_week (twin_id, iso_week),
  CONSTRAINT fk_snapshot_twin FOREIGN KEY (twin_id) REFERENCES digital_twins (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE technical_evaluations (
  id                BINARY(16)   NOT NULL,
  athlete_id        BINARY(16)   NOT NULL,
  evaluator_id      BINARY(16)   NOT NULL,
  evaluator_role    VARCHAR(32)  NOT NULL,
  rating            DECIMAL(4,2) NOT NULL,
  weight            DECIMAL(3,2) NOT NULL,
  dimension         VARCHAR(12)  NOT NULL,
  week_of           DATE         NOT NULL,
  comment           VARCHAR(500) NULL,
  counts_for_rarity BIT(1)       NOT NULL DEFAULT b'1',
  PRIMARY KEY (id),
  KEY ix_eval_athlete_week (athlete_id, week_of)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE match_performances (
  id             BINARY(16)   NOT NULL,
  athlete_id     BINARY(16)   NOT NULL,
  match_id       BINARY(16)   NULL,
  match_date     DATE         NOT NULL,
  competition    VARCHAR(80)  NULL,
  official       BIT(1)       NOT NULL DEFAULT b'1',
  minutes_played INT          NOT NULL DEFAULT 0,
  rating         DECIMAL(4,2) NULL,
  goals          INT          NOT NULL DEFAULT 0,
  assists        INT          NOT NULL DEFAULT 0,
  clean_sheet    BIT(1)       NOT NULL DEFAULT b'0',
  tackles        INT          NOT NULL DEFAULT 0,
  yellow_cards   INT          NOT NULL DEFAULT 0,
  red_cards      INT          NOT NULL DEFAULT 0,
  mvp            BIT(1)       NOT NULL DEFAULT b'0',
  season         VARCHAR(9)   NOT NULL,
  PRIMARY KEY (id),
  KEY ix_match_athlete_date (athlete_id, match_date),
  KEY ix_match_athlete_season (athlete_id, season)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE training_summaries (
  id               BINARY(16)   NOT NULL,
  athlete_id       BINARY(16)   NOT NULL,
  iso_week         VARCHAR(8)   NOT NULL,
  sessions_planned INT          NOT NULL DEFAULT 0,
  sessions_done    INT          NOT NULL DEFAULT 0,
  load_au          DECIMAL(9,2) NOT NULL DEFAULT 0,
  chronic_load_au  DECIMAL(9,2) NOT NULL DEFAULT 0,
  avg_rpe          DECIMAL(4,2) NULL,
  distance_km      DECIMAL(7,2) NULL,
  readiness        INT          NOT NULL DEFAULT 100,
  PRIMARY KEY (id),
  UNIQUE KEY uk_training_week (athlete_id, iso_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE scouting_attributes (
  id             BINARY(16)   NOT NULL,
  athlete_id     BINARY(16)   NOT NULL,
  iso_week       VARCHAR(8)   NOT NULL,
  velocidade     DECIMAL(5,2) NOT NULL DEFAULT 0,
  passe          DECIMAL(5,2) NOT NULL DEFAULT 0,
  finalizacao    DECIMAL(5,2) NOT NULL DEFAULT 0,
  drible         DECIMAL(5,2) NOT NULL DEFAULT 0,
  desarme        DECIMAL(5,2) NOT NULL DEFAULT 0,
  posicionamento DECIMAL(5,2) NOT NULL DEFAULT 0,
  resistencia    DECIMAL(5,2) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_scouting_week (athlete_id, iso_week)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE athlete_badges (
  id         BINARY(16)  NOT NULL,
  athlete_id BINARY(16)  NOT NULL,
  badge_type VARCHAR(24) NOT NULL,
  counter    INT         NOT NULL DEFAULT 0,
  season     VARCHAR(9)  NOT NULL,
  awarded_at DATETIME(6) NOT NULL,
  awarded_by BINARY(16)  NULL,
  active     BIT(1)      NOT NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uk_badge_season (athlete_id, badge_type, season),
  KEY ix_badge_athlete_active (athlete_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE badge_audit_log (
  id         BINARY(16)   NOT NULL,
  athlete_id BINARY(16)   NOT NULL,
  badge_type VARCHAR(24)  NOT NULL,
  action     VARCHAR(16)  NOT NULL,
  reason     VARCHAR(160) NULL,
  at         DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY ix_badge_audit_athlete (athlete_id, at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE injury_records (
  id                   BINARY(16)    NOT NULL,
  athlete_id           BINARY(16)    NOT NULL,
  injury_type          VARCHAR(512)  NOT NULL,
  body_part            VARCHAR(40)   NULL,
  severity             VARCHAR(12)   NOT NULL,
  started_at           DATE          NOT NULL,
  expected_return_date DATE          NULL,
  cleared_at           DATE          NULL,
  reported_by          BINARY(16)    NULL,
  notes                VARCHAR(2048) NULL,
  PRIMARY KEY (id),
  KEY ix_injury_athlete_active (athlete_id, cleared_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE posts (
  id             BINARY(16)    NOT NULL,
  author_id      BINARY(16)    NOT NULL,
  author_role    VARCHAR(32)   NOT NULL,
  content_type   VARCHAR(24)   NOT NULL,
  window_name    VARCHAR(12)   NOT NULL,
  title          VARCHAR(90)   NOT NULL,
  subtitle       VARCHAR(160)  NULL,
  content        VARCHAR(5000) NULL,
  media_keys     JSON          NULL,
  age_rating     VARCHAR(12)   NOT NULL DEFAULT 'SAFE',
  ai_generated   BIT(1)        NOT NULL DEFAULT b'0',
  status         VARCHAR(12)   NOT NULL DEFAULT 'PUBLISHED',
  severity       VARCHAR(12)   NULL,
  alert_type     VARCHAR(16)   NULL,
  club_id        BINARY(16)    NULL,
  state_code     VARCHAR(2)    NULL,
  drill_id       BINARY(16)    NULL,
  published_at   DATETIME(6)   NOT NULL,
  created_at     DATETIME(6)   NOT NULL,
  like_count     INT           NOT NULL DEFAULT 0,
  comment_count  INT           NOT NULL DEFAULT 0,
  share_count    INT           NOT NULL DEFAULT 0,
  save_count     INT           NOT NULL DEFAULT 0,
  view_count     BIGINT        NOT NULL DEFAULT 0,
  trending_score DECIMAL(8,3)  NOT NULL DEFAULT 0,
  trending_delta DECIMAL(8,3)  NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_posts_window_published (window_name, published_at),
  KEY ix_posts_author (author_id),
  KEY ix_posts_trending (window_name, trending_score),
  CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_reactions (
  id         BINARY(16) NOT NULL,
  post_id    BINARY(16) NOT NULL,
  user_id    BINARY(16) NOT NULL,
  kind       VARCHAR(8) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reaction (post_id, user_id, kind),
  KEY ix_reaction_user (user_id),
  CONSTRAINT fk_reaction_post FOREIGN KEY (post_id) REFERENCES posts (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE data_export_requests (
  id             BINARY(16)   NOT NULL,
  user_id        BINARY(16)   NOT NULL,
  status         VARCHAR(12)  NOT NULL DEFAULT 'REQUESTED',
  requested_at   DATETIME(6)  NOT NULL,
  ready_at       DATETIME(6)  NULL,
  expires_at     DATETIME(6)  NULL,
  s3_object_key  VARCHAR(255) NULL,
  size_bytes     BIGINT       NULL,
  failure_reason VARCHAR(255) NULL,
  attempts       INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_export_user (user_id, requested_at),
  KEY ix_export_status_exp (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Versão vigente dos Termos de Uso / Política de Privacidade (Velion, proprietária da Orange Social)
INSERT INTO terms_versions (id, version, owner, document_url, privacy_policy_url, effective_from, is_current)
VALUES (UUID_TO_BIN(UUID()), '2026.10', 'Velion (Orange Social)', 'https://<PREENCHER_DOMINIO>/legal/termos-2026-10', 'https://<PREENCHER_DOMINIO>/legal/privacidade-2026-10', UTC_TIMESTAMP(6), b'1');
