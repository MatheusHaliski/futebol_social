-- Criador de cards (modo de geração + arte de fundo), Modo FLAIR (RF6) e UI dark-first

ALTER TABLE users ALTER COLUMN dark_mode SET DEFAULT b'1';
UPDATE users SET dark_mode = b'1';

ALTER TABLE posts
  ADD COLUMN generation_mode VARCHAR(8)  NOT NULL DEFAULT 'MANUAL' AFTER trending_delta,
  ADD COLUMN art_color       VARCHAR(7)  NULL AFTER generation_mode,
  ADD COLUMN art_material    VARCHAR(16) NULL AFTER art_color,
  ADD COLUMN art_layout      VARCHAR(16) NULL AFTER art_material,
  ADD COLUMN art_style       VARCHAR(16) NULL AFTER art_layout;

CREATE TABLE flair_cards (
  id               BINARY(16)  NOT NULL,
  owner_id         BINARY(16)  NOT NULL,
  athlete_id       BINARY(16)  NOT NULL,
  athlete_name     VARCHAR(80) NOT NULL,
  athlete_position VARCHAR(16) NULL,
  tier             VARCHAR(12) NOT NULL,
  pp               INT         NOT NULL DEFAULT 0,
  rp               INT         NOT NULL DEFAULT 0,
  mp               INT         NOT NULL DEFAULT 0,
  score_total      INT         NOT NULL DEFAULT 0,
  season           VARCHAR(9)  NOT NULL,
  source           VARCHAR(12) NOT NULL,
  acquired_at      DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY ix_flair_owner (owner_id),
  KEY ix_flair_owner_source (owner_id, source, acquired_at),
  CONSTRAINT fk_flair_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE flair_decks (
  owner_id      BINARY(16)  NOT NULL,
  card1_id      BINARY(16)  NULL,
  card2_id      BINARY(16)  NULL,
  card3_id      BINARY(16)  NULL,
  captain_index INT         NOT NULL DEFAULT 0,
  updated_at    DATETIME(6) NOT NULL,
  PRIMARY KEY (owner_id),
  CONSTRAINT fk_deck_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE flair_matches (
  id              BINARY(16)  NOT NULL,
  challenger_id   BINARY(16)  NOT NULL,
  opponent_id     BINARY(16)  NULL,
  opponent_label  VARCHAR(80) NOT NULL,
  status          VARCHAR(12) NOT NULL DEFAULT 'IN_PROGRESS',
  rounds_json     JSON        NOT NULL,
  rounds_played   INT         NOT NULL DEFAULT 0,
  challenger_wins INT         NOT NULL DEFAULT 0,
  opponent_wins   INT         NOT NULL DEFAULT 0,
  snap_used       BIT(1)      NOT NULL DEFAULT b'0',
  points_awarded  INT         NOT NULL DEFAULT 0,
  deck_json       JSON        NOT NULL,
  created_at      DATETIME(6) NOT NULL,
  finished_at     DATETIME(6) NULL,
  PRIMARY KEY (id),
  KEY ix_flair_match_user (challenger_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE points_ledger (
  id      BINARY(16)  NOT NULL,
  user_id BINARY(16)  NOT NULL,
  delta   INT         NOT NULL,
  reason  VARCHAR(24) NOT NULL,
  ref_id  BINARY(16)  NULL,
  at      DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY ix_points_user (user_id, at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
