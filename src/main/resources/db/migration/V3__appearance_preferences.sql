-- Aparência (preferências de interface, não dado pessoal): cor de destaque, tom das barras/painéis e tela de fundo
ALTER TABLE users
  ADD COLUMN ui_accent     VARCHAR(12)  NOT NULL DEFAULT 'GREEN'    AFTER font_scale,
  ADD COLUMN ui_surface    VARCHAR(12)  NOT NULL DEFAULT 'GRAPHITE' AFTER ui_accent,
  ADD COLUMN wallpaper     VARCHAR(16)  NOT NULL DEFAULT 'NONE'     AFTER ui_surface,
  ADD COLUMN wallpaper_key VARCHAR(255) NULL                        AFTER wallpaper;
