package br.edu.cefetmg.scoa.dominio;

public record Disciplina(Long id, String codigo, String nome, int cargaHoraria, String situacao) {
    public Disciplina {
        if (id != null) HorarioAula.validarId(id);
        codigo = DadosAcademicos.texto(codigo, "o código da disciplina", 30);
        nome = DadosAcademicos.texto(nome, "o nome da disciplina", 150);
        situacao = DadosAcademicos.texto(situacao, "a situação da disciplina", 20);
        if (cargaHoraria < 1 || cargaHoraria > 65_535)
            throw new IllegalArgumentException("Carga horária fora dos limites do cadastro.");
    }
}
