package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDate;

/** Período nulo é permitido na leitura de registros anteriores à v0.6. */
public record Matricula(Long id, long idAluno, long idCurso, LocalDate dataIngresso,
                        Long idPeriodoIngresso, String situacao) {
    public Matricula {
        if (id != null) HorarioAula.validarId(id);
        HorarioAula.validarId(idAluno); HorarioAula.validarId(idCurso);
        if (idPeriodoIngresso != null) HorarioAula.validarId(idPeriodoIngresso);
        validarData(dataIngresso);
        situacao = DadosAcademicos.texto(situacao, "a situação", 20);
    }
    public static LocalDate validarData(LocalDate data) {
        if (data == null || data.getYear() < 1000 || data.getYear() > 9999)
            throw new IllegalArgumentException("Informe data de ingresso entre os anos 1000 e 9999.");
        return data;
    }
    public static String validarMotivo(String motivo) { return DadosAcademicos.texto(motivo, "o motivo", 500); }
    public static void validarTransicao(String origem, String destino) {
        if (!("ATIVO".equals(origem) && ("TRANCADO".equals(destino) || "CANCELADO".equals(destino)))
                && !("TRANCADO".equals(origem) && ("ATIVO".equals(destino) || "CANCELADO".equals(destino))))
            throw new IllegalArgumentException("Transição acadêmica não permitida nesta entrega.");
    }
}
