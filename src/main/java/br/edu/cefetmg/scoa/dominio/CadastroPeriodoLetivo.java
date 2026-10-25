package br.edu.cefetmg.scoa.dominio;

import java.util.Objects;

/** Cadastro persistido; PeriodoLetivo continua representando o intervalo de datas. */
public record CadastroPeriodoLetivo(Long id, String descricao, PeriodoLetivo datas, String situacao) {
    public CadastroPeriodoLetivo {
        if (id != null) HorarioAula.validarId(id);
        descricao = DadosAcademicos.texto(descricao,"a descrição do período",50);
        situacao = DadosAcademicos.texto(situacao,"a situação",20);
        Objects.requireNonNull(datas,"Informe as datas do período.");
        if (datas.inicio().getYear()<1000 || datas.fim().getYear()>9999)
            throw new IllegalArgumentException("As datas devem estar entre os anos 1000 e 9999, conforme DATE do MySQL.");
    }
}
