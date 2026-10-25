-- Keyspace e tabelas de alta vazão (feeds segmentados por janela e telemetria do Gêmeo)
CREATE KEYSPACE IF NOT EXISTS football_ai_social
  WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE football_ai_social;

-- Fan-out de posts por usuário e janela (RF3). Partição = (user_id, feed_window); ordenação por tempo DESC.
CREATE TABLE IF NOT EXISTS feed_entries (
  user_id        uuid,
  feed_window    text,
  published_at   timeuuid,
  post_id        uuid,
  author_id      uuid,
  author_role    text,
  content_type   text,
  severity       text,
  alert_type     text,
  source_role    text,
  PRIMARY KEY ((user_id, feed_window), published_at)
) WITH CLUSTERING ORDER BY (published_at DESC);

-- Telemetria do atleta (RF Gêmeo). Partição por atleta e mês para evitar partições gigantes.
CREATE TABLE IF NOT EXISTS telemetry_data (
  athlete_id     uuid,
  year_month     text,
  recorded_at    timestamp,
  event_type     text,      -- TREINO | PARTIDA
  metric_type    text,      -- FISICA | TECNICA | TATICA
  distance_km    decimal,
  duration_min   int,
  rpe            int,
  load_au        decimal,
  max_speed_kmh  decimal,
  passes_ok      int,
  passes_total   int,
  gps_track      text,      -- só preenchido com locationHistory = true
  PRIMARY KEY ((athlete_id, year_month), recorded_at)
) WITH CLUSTERING ORDER BY (recorded_at DESC);
