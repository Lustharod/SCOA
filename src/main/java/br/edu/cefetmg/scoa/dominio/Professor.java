package br.edu.cefetmg.scoa.dominio;

/** O vínculo com conta de acesso é opcional e não é administrado por este cadastro. */
public record Professor(Long id, Long idUsuario, String identificacao, String situacao) {
    public Professor {
        if (id != null) HorarioAula.validarId(id);
        if (idUsuario != null) HorarioAula.validarId(idUsuario);
        identificacao = DadosAcademicos.texto(identificacao, "a identificação do professor", 100);
        situacao = DadosAcademicos.texto(situacao, "a situação do professor", 20);
    }
}
