-- Boletim de lesão (LESAO_ALERTA) passa a apontar o atleta de quem fala: quem vê o boletim segue a regra de saúde
-- (LGPD Art. 11) desse atleta — o próprio, staff de saúde verificado, ou quem vê o perfil quando ele compartilha dados de saúde.
ALTER TABLE posts ADD COLUMN subject_athlete_id BINARY(16) NULL AFTER club_id;
CREATE INDEX idx_posts_subject_athlete ON posts (subject_athlete_id);

-- Boletins já publicados por atletas falam do próprio autor. Os demais ficam sem sujeito — visíveis só ao autor — até serem vinculados.
UPDATE posts SET subject_athlete_id = author_id WHERE content_type = 'LESAO_ALERTA' AND author_role = 'ATLETA';
