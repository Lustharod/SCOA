package br.edu.cefetmg.scoa.aplicacao;

import java.util.List;
import java.util.Objects;
import java.util.function.LongFunction;

public final class ServicoConsultaProcessosAcademicos {
    private final LongFunction<List<String>> consultaPorMatricula;

    public ServicoConsultaProcessosAcademicos(LongFunction<List<String>> consultaPorMatricula) {
        this.consultaPorMatricula = Objects.requireNonNull(consultaPorMatricula);
    }

    public List<String> listar(long matricula) {
        return consultaPorMatricula.apply(matricula);
    }
}
