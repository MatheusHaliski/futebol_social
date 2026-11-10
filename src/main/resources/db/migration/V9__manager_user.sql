-- Manager é um papel humano próprio; vínculos desconhecidos permanecem NULL.
CREATE TABLE user_managers (
  user_id BINARY(16) NOT NULL,
  role_function VARCHAR(32) NULL,
  nationalities VARCHAR(120) NULL,
  languages VARCHAR(120) NULL,
  license_name VARCHAR(120) NULL,
  license_level VARCHAR(40) NULL,
  preferred_formation VARCHAR(40) NULL,
  playing_style VARCHAR(120) NULL,
  career_started_on DATE NULL,
  has_current_club BIT(1) NULL,
  club_status_source VARCHAR(240) NULL,
  club_status_as_of DATE NULL,
  transfer_situation VARCHAR(24) NOT NULL DEFAULT 'NOT_FOR_SALE',
  PRIMARY KEY(user_id),
  CONSTRAINT fk_managers_user FOREIGN KEY(user_id) REFERENCES users(id),
  KEY ix_manager_club_status(has_current_club)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE club_manager_tenures
  ADD CONSTRAINT fk_tenures_manager_user FOREIGN KEY (manager_user_id) REFERENCES user_managers(user_id);

CREATE TABLE manager_career_titles (
  id BINARY(16) NOT NULL,
  manager_id BINARY(16) NOT NULL,
  club_id BINARY(16) NULL,
  club_name VARCHAR(120) NOT NULL,
  competition VARCHAR(120) NOT NULL,
  season VARCHAR(30) NOT NULL,
  category VARCHAR(40) NULL,
  awarded_on DATE NOT NULL,
  source VARCHAR(240) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY(id),
  KEY ix_manager_titles(manager_id,awarded_on),
  CONSTRAINT fk_titles_manager FOREIGN KEY(manager_id) REFERENCES user_managers(user_id),
  CONSTRAINT fk_titles_club FOREIGN KEY(club_id) REFERENCES user_clubs(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
