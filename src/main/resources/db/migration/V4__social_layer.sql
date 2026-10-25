-- Camada social (RF3): seguir, comentários em cards e notificações pessoais

CREATE TABLE follows (
  id          BINARY(16)  NOT NULL,
  follower_id BINARY(16)  NOT NULL,
  target_id   BINARY(16)  NOT NULL,
  target_type VARCHAR(8)  NOT NULL,           -- USER | CLUBE | LIGA
  since       DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_follow (follower_id, target_id),
  KEY ix_follow_target (target_id),
  CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES users (id),
  CONSTRAINT fk_follow_target   FOREIGN KEY (target_id)   REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE post_comments (
  id         BINARY(16)   NOT NULL,
  post_id    BINARY(16)   NOT NULL,
  author_id  BINARY(16)   NOT NULL,
  parent_id  BINARY(16)   NULL,               -- um nível de resposta
  body       VARCHAR(500) NOT NULL,
  status     VARCHAR(8)   NOT NULL DEFAULT 'VISIBLE',
  created_at DATETIME(6)  NOT NULL,
  removed_at DATETIME(6)  NULL,
  PRIMARY KEY (id),
  KEY ix_comment_post (post_id, status, created_at),
  KEY ix_comment_author (author_id),
  CONSTRAINT fk_comment_post   FOREIGN KEY (post_id)   REFERENCES posts (id),
  CONSTRAINT fk_comment_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
  id         BINARY(16)   NOT NULL,
  user_id    BINARY(16)   NOT NULL,           -- destinatário
  type       VARCHAR(16)  NOT NULL,           -- LIKE | COMMENT | REPLY | FOLLOW | FLAIR_RESULT | TIER_CHANGE | SYSTEM
  actor_id   BINARY(16)   NULL,               -- quem fez (nulo após anonimização do ator)
  ref_type   VARCHAR(12)  NULL,               -- POST | COMMENT | USER | MATCH | TWIN
  ref_id     BINARY(16)   NULL,
  title      VARCHAR(120) NOT NULL,
  body       VARCHAR(280) NULL,
  created_at DATETIME(6)  NOT NULL,
  read_at    DATETIME(6)  NULL,
  PRIMARY KEY (id),
  KEY ix_notification_user (user_id, read_at, created_at),
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
