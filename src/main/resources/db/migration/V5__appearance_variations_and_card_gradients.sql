-- Aparência de navegação e layout (preferências de interface, não dado pessoal): material das barras, gradiente ambiente,
-- fonte, densidade e contraste. Arte do card: segunda cor e gradiente de fundo.
ALTER TABLE users
  ADD COLUMN ui_material VARCHAR(12) NOT NULL DEFAULT 'FOSCO'       AFTER wallpaper_key,
  ADD COLUMN ui_gradient VARCHAR(16) NOT NULL DEFAULT 'HOLOFOTE'    AFTER ui_material,
  ADD COLUMN ui_font     VARCHAR(12) NOT NULL DEFAULT 'PADRAO'      AFTER ui_gradient,
  ADD COLUMN ui_density  VARCHAR(12) NOT NULL DEFAULT 'CONFORTAVEL' AFTER ui_font,
  ADD COLUMN ui_contrast VARCHAR(8)  NOT NULL DEFAULT 'PADRAO'      AFTER ui_density;

ALTER TABLE posts
  ADD COLUMN art_color2   VARCHAR(7)  NULL AFTER art_color,
  ADD COLUMN art_gradient VARCHAR(16) NULL AFTER art_color2;
