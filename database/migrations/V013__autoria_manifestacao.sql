-- Aplicar UMA vez após V012, no schema explicitamente selecionado e com backup.
-- Preserva manifestações e autores desconhecidos do legado, sem reconstrução por suposição.
ALTER TABLE manifestacao_aluno
    ADD COLUMN id_usuario_autor INT UNSIGNED NULL AFTER id_aluno,
    ADD CONSTRAINT fk_manifestacao_autor FOREIGN KEY (id_usuario_autor)
        REFERENCES usuario(id_usuario) ON DELETE RESTRICT,
    ADD INDEX ix_manifestacao_aluno_data (id_aluno,data_hora,id_manifestacao);
