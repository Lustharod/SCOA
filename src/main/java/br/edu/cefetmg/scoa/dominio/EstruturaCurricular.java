package br.edu.cefetmg.scoa.dominio;

public record EstruturaCurricular(Long id, long idCurso, String identificacao, String situacao) {
    public EstruturaCurricular {
        if (id != null) HorarioAula.validarId(id);
        HorarioAula.validarId(idCurso);
        identificacao = DadosAcademicos.texto(identificacao, "a identificação da estrutura", 100);
        situacao = DadosAcademicos.texto(situacao, "a situação da estrutura", 20);
    }
}
