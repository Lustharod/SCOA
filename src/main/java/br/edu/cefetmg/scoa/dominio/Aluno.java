package br.edu.cefetmg.scoa.dominio;

/** Cadastro básico; a matrícula institucional pertence a outro fluxo. */
public record Aluno(Long id, Long idUsuario, String identificacao, String situacao) {
    public Aluno {
        if (id != null) HorarioAula.validarId(id);
        if (idUsuario != null) HorarioAula.validarId(idUsuario);
        identificacao = DadosAcademicos.texto(identificacao, "a identificação do aluno", 100);
        situacao = DadosAcademicos.texto(situacao, "a situação do aluno", 20);
    }
}
