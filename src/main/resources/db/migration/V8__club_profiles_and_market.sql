CREATE TABLE club_teams (
  id BINARY(16) NOT NULL,
  club_id BINARY(16) NOT NULL,
  source VARCHAR(240) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  name VARCHAR(120) NOT NULL,
  category VARCHAR(30) NOT NULL,
  modality VARCHAR(30) NOT NULL,
  gender VARCHAR(30) NULL,
  playing_style VARCHAR(80) NULL,
  wanted_positions VARCHAR(200) NULL,
  preferred_foot VARCHAR(5) NULL,
  languages VARCHAR(120) NULL,
  vacancies INT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_club_teams_club (club_id),
  CONSTRAINT fk_club_teams_club FOREIGN KEY (club_id) REFERENCES user_clubs(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE club_manager_tenures (
  id BINARY(16) NOT NULL,
  club_id BINARY(16) NOT NULL,
  source VARCHAR(240) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  team_id BINARY(16) NOT NULL,
  name VARCHAR(120) NOT NULL,
  role_function VARCHAR(32) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NULL,
  interim BIT(1) NOT NULL,
  nationality VARCHAR(2) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_club_manager_tenures_club (club_id),
  CONSTRAINT fk_club_manager_tenures_club FOREIGN KEY (club_id) REFERENCES user_clubs(user_id),
  CONSTRAINT fk_club_manager_tenures_team FOREIGN KEY (team_id) REFERENCES club_teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE club_competition_entries (
  id BINARY(16) NOT NULL,
  club_id BINARY(16) NOT NULL,
  source VARCHAR(240) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  team_id BINARY(16) NOT NULL,
  competition VARCHAR(120) NOT NULL,
  organizer VARCHAR(120) NULL,
  country VARCHAR(2) NULL,
  season VARCHAR(30) NOT NULL,
  format VARCHAR(32) NOT NULL,
  division INT NULL,
  stage VARCHAR(80) NULL,
  group_name VARCHAR(80) NULL,
  outcome VARCHAR(80) NULL,
  position INT NULL,
  round INT NULL,
  played INT NULL,
  wins INT NULL,
  draws INT NULL,
  losses INT NULL,
  goals_for INT NULL,
  goals_against INT NULL,
  points INT NULL,
  champion BIT(1) NOT NULL,
  as_of DATE NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_club_competition_entries_club (club_id),
  CONSTRAINT fk_club_competition_entries_club FOREIGN KEY (club_id) REFERENCES user_clubs(user_id),
  CONSTRAINT fk_club_competition_entries_team FOREIGN KEY (team_id) REFERENCES club_teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE club_finance_snapshots (
  id BINARY(16) NOT NULL,
  club_id BINARY(16) NOT NULL,
  source VARCHAR(240) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  team_id BINARY(16) NULL,
  season VARCHAR(30) NOT NULL,
  currency VARCHAR(32) NOT NULL,
  payroll DECIMAL(18,6) NULL,
  payroll_period VARCHAR(32) NOT NULL,
  squad_value DECIMAL(18,6) NULL,
  recruitment_budget DECIMAL(18,6) NULL,
  valuation_type VARCHAR(32) NOT NULL,
  as_of DATE NOT NULL,
  published BIT(1) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_club_finance_snapshots_club (club_id),
  CONSTRAINT fk_club_finance_snapshots_club FOREIGN KEY (club_id) REFERENCES user_clubs(user_id),
  CONSTRAINT fk_club_finance_snapshots_team FOREIGN KEY (team_id) REFERENCES club_teams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE club_venues (
  id BINARY(16) NOT NULL,
  club_id BINARY(16) NOT NULL,
  source VARCHAR(240) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  name VARCHAR(120) NOT NULL,
  venue_type VARCHAR(32) NOT NULL,
  street VARCHAR(160) NOT NULL,
  number VARCHAR(20) NULL,
  district VARCHAR(80) NULL,
  city VARCHAR(80) NOT NULL,
  state VARCHAR(80) NULL,
  postal_code VARCHAR(20) NULL,
  country VARCHAR(2) NOT NULL,
  capacity INT NULL,
  surface VARCHAR(40) NULL,
  ownership VARCHAR(40) NULL,
  latitude DECIMAL(18,6) NULL,
  longitude DECIMAL(18,6) NULL,
  start_date DATE NOT NULL,
  end_date DATE NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_club_venues_club (club_id),
  CONSTRAINT fk_club_venues_club FOREIGN KEY (club_id) REFERENCES user_clubs(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE club_memberships (
  id BINARY(16) NOT NULL,
  club_id BINARY(16) NOT NULL,
  source VARCHAR(240) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  team_id BINARY(16) NOT NULL,
  athlete_id BINARY(16) NOT NULL,
  parent_club_id BINARY(16) NULL,
  kind VARCHAR(32) NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NULL,
  shirt_number INT NULL,
  captain BIT(1) NOT NULL,
  status VARCHAR(32) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY ix_club_memberships_club (club_id),
  CONSTRAINT fk_club_memberships_club FOREIGN KEY (club_id) REFERENCES user_clubs(user_id),
  CONSTRAINT fk_club_memberships_team FOREIGN KEY (team_id) REFERENCES club_teams(id),
  KEY ix_memberships_athlete (athlete_id, status),
  CONSTRAINT fk_memberships_athlete FOREIGN KEY (athlete_id) REFERENCES user_athletes(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE user_clubs
  ADD COLUMN short_name VARCHAR(40),
  ADD COLUMN website VARCHAR(240),
  ADD COLUMN public_contact VARCHAR(160),
  ADD COLUMN federation VARCHAR(120),
  ADD COLUMN colors VARCHAR(80),
  ADD COLUMN ownership_type VARCHAR(40),
  ADD COLUMN president VARCHAR(120),
  ADD COLUMN history VARCHAR(2000),
  ADD COLUMN honours VARCHAR(2000);

ALTER TABLE user_athletes
  ADD COLUMN affiliation_status VARCHAR(20) DEFAULT 'UNKNOWN',
  ADD COLUMN free_transfer_confirmed BIT(1) NOT NULL DEFAULT b'0',
  ADD COLUMN market_verified_on DATE,
  ADD COLUMN market_source VARCHAR(240),
  ADD COLUMN last_club_name VARCHAR(120),
  ADD COLUMN nationalities VARCHAR(120),
  ADD COLUMN languages VARCHAR(120),
  ADD COLUMN playing_style VARCHAR(80);

UPDATE user_athletes SET affiliation_status='WITHOUT_CLUB' WHERE contract_status='AGENTE_LIVRE' AND current_club_id IS NULL AND (current_club_name IS NULL OR TRIM(current_club_name)='');

ALTER TABLE club_manager_tenures
  ADD COLUMN manager_user_id BINARY(16) NULL,
  ADD COLUMN contract_until DATE NULL,
  ADD COLUMN transfer_fee DECIMAL(18,6) NULL,
  ADD COLUMN currency VARCHAR(3) NULL,
  ADD COLUMN transfer_fee_public BIT(1) NOT NULL DEFAULT b'0';
CREATE INDEX ix_tenures_manager ON club_manager_tenures(manager_user_id);

ALTER TABLE user_athletes ADD COLUMN transfer_situation VARCHAR(24) NOT NULL DEFAULT 'NOT_FOR_SALE';
UPDATE user_athletes SET transfer_situation='FREE_AGENT' WHERE affiliation_status='WITHOUT_CLUB' AND contract_status='AGENTE_LIVRE';
