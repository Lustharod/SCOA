-- Aplicar uma vez após V011 no schema selecionado.
-- Coordena alterações do calendário realizadas pelo console.
CREATE TABLE controle_calendario (
    id TINYINT NOT NULL PRIMARY KEY,
    CHECK (id=1)
) ENGINE=InnoDB;
INSERT INTO controle_calendario(id) VALUES (1);
