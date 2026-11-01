package br.edu.cefetmg.scoa.dominio;

public record Curso(Long id, String nome, String situacao) {
    public Curso {
        if (id != null) HorarioAula.validarId(id);
        nome = DadosAcademicos.texto(nome, "o nome do curso", 150);
        situacao = DadosAcademicos.texto(situacao, "a situação do curso", 20);
    }
}
