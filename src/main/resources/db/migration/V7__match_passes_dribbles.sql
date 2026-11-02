-- Vitrine do Jogador: passes e dribles certos por partida (súmula oficial ou autorrelato).
-- "certos" ≤ "tentados" é validado na API (MATCH_INVALID). Partidas antigas ficam com 0/0: sem tentativa registrada,
-- a Vitrine não calcula aproveitamento (nunca mostra 0 % por falta de dado).
ALTER TABLE match_performances
  ADD COLUMN passes_completed   INT NOT NULL DEFAULT 0 AFTER assists,
  ADD COLUMN passes_attempted   INT NOT NULL DEFAULT 0 AFTER passes_completed,
  ADD COLUMN dribbles_completed INT NOT NULL DEFAULT 0 AFTER passes_attempted,
  ADD COLUMN dribbles_attempted INT NOT NULL DEFAULT 0 AFTER dribbles_completed;
