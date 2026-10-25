package br.edu.cefetmg.scoa.dominio;

public record Turma(Long id, long idDisciplina, long idProfessor, long idPeriodo,
                    String codigo, int capacidade, String situacao) {
    public Turma {
        if (id != null) HorarioAula.validarId(id);
        HorarioAula.validarId(idDisciplina);
        HorarioAula.validarId(idProfessor);
        HorarioAula.validarId(idPeriodo);
        codigo = DadosAcademicos.texto(codigo,"o código da turma",40);
        situacao = DadosAcademicos.texto(situacao,"a situação",20);
        if (capacidade<1 || capacidade>65_535) throw new IllegalArgumentException("A capacidade deve estar entre 1 e 65535.");
    }
}
