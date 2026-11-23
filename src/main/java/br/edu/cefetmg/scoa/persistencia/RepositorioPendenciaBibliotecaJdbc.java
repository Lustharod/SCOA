package br.edu.cefetmg.scoa.persistencia;

import br.edu.cefetmg.scoa.dominio.Equivalencia;
import br.edu.cefetmg.scoa.dominio.HorarioAula;
import br.edu.cefetmg.scoa.dominio.Matricula;
import java.math.BigDecimal;
import java.sql.Connection;

import static br.edu.cefetmg.scoa.persistencia.JdbcAcademico.*;

public final class RepositorioPendenciaBibliotecaJdbc {
    private final Connection conexao;
    private final SegurancaJdbc.Sessao sessao;

    public RepositorioPendenciaBibliotecaJdbc(Connection conexao,SegurancaJdbc.Sessao sessao) {
        this.conexao=conexao;
        this.sessao=sessao;
    }

    public long registrar(long emprestimo,String tipo,BigDecimal valor,String justificativa) {
        HorarioAula.validarId(emprestimo);
        String natureza=Equivalencia.texto(tipo,"tipo de pendência",30);
        String motivo=Matricula.validarMotivo(justificativa);
        if(valor==null || valor.signum()<0 || valor.stripTrailingZeros().scale()>2 ||
                valor.compareTo(new BigDecimal("9999999999.99"))>0)
            throw new IllegalArgumentException("Informe um valor não negativo com até duas casas decimais.");
        return TransacaoJdbc.executar(conexao,()->{
            long usuario=SegurancaJdbc.exigir(conexao,sessao,SegurancaJdbc.REGISTRAR_PENDENCIA);
            var vinculos=q(conexao,"SELECT id_usuario_biblioteca FROM emprestimo WHERE id_emprestimo=?",
                    r->r.getLong(1),emprestimo);
            if(vinculos.isEmpty()) throw new IllegalArgumentException("Empréstimo não encontrado.");
            long conta=vinculos.getFirst();
            q(conexao,"SELECT id_usuario_biblioteca FROM usuario_biblioteca WHERE id_usuario_biblioteca=? FOR UPDATE",
                    r->r.getLong(1),conta);
            if(q(conexao,"SELECT id_emprestimo FROM emprestimo WHERE id_emprestimo=? AND id_usuario_biblioteca=? FOR UPDATE",
                    r->r.getLong(1),emprestimo,conta).isEmpty())
                throw new IllegalArgumentException("Vínculo do empréstimo alterado. Consulte novamente.");
            long id=g(conexao,"""
                    INSERT INTO pendencia_biblioteca(id_emprestimo,tipo_pendencia,valor,impeditiva,situacao)
                    VALUES (?,?,?,TRUE,'ATIVO')
                    """,emprestimo,natureza,valor);
            evento(conexao,"BIBLIOTECA",id,usuario,"REGISTRAR_PENDENCIA",motivo);
            return id;
        });
    }
}
