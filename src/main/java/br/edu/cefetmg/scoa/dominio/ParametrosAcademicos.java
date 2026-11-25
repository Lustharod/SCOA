package br.edu.cefetmg.scoa.dominio;

import java.math.BigDecimal;
import java.time.*;
import java.util.Objects;

/** Versões imutáveis, selecionadas explicitamente antes do início do período. */
public final class ParametrosAcademicos {
    private ParametrosAcademicos() { }
    public record Regras(int antecedenciaPrimeira,int duracaoPrimeira,int inicioSegunda,int duracaoSegunda,
            int cancelamentoImediatoAte,int cancelamentoPedidoAte,int transferenciaAte,int retificacaoDocenteDias,
            int toleranciaAtraso,BigDecimal frequenciaPercentual,BigDecimal mediaDispensaPf,BigDecimal mediaFinalMinima) {
        public Regras {
            for(int n:new int[]{antecedenciaPrimeira,inicioSegunda,cancelamentoImediatoAte,cancelamentoPedidoAte,transferenciaAte,retificacaoDocenteDias})
                if(n<0 || n>365) throw new IllegalArgumentException("Prazos devem estar entre 0 e 365 dias corridos.");
            if(duracaoPrimeira<1 || duracaoPrimeira>365 || duracaoSegunda<1 || duracaoSegunda>365
                    || antecedenciaPrimeira<duracaoPrimeira || inicioSegunda+duracaoSegunda>366)
                throw new IllegalArgumentException("Confira durações e janelas: primeira fase antes do início e segunda a partir dele.");
            if(cancelamentoImediatoAte>cancelamentoPedidoAte)
                throw new IllegalArgumentException("Liberação imediata não pode ultrapassar o prazo de pedido.");
            if(toleranciaAtraso<0 || toleranciaAtraso>=50)
                throw new IllegalArgumentException("Tolerância de atraso deve ser de 0 a 49 minutos por tempo de 50 minutos.");
            frequenciaPercentual=decimal(frequenciaPercentual,100,"frequência");
            if(frequenciaPercentual.signum()==0) throw new IllegalArgumentException("Frequência mínima deve ser positiva.");
            mediaDispensaPf=decimal(mediaDispensaPf,10,"média que dispensa PF");
            mediaFinalMinima=decimal(mediaFinalMinima,10,"média final mínima");
            if(mediaFinalMinima.compareTo(mediaDispensaPf)>0)
                throw new IllegalArgumentException("Média final mínima não pode superar a média que dispensa PF.");
        }
        public static Regras baseAprovada() { return new Regras(14,7,7,7,13,45,45,7,15,new BigDecimal("75.0"),new BigDecimal("7.0"),new BigDecimal("5.0")); }
        public JanelaInscricao primeiraFase(LocalDate inicio) { var a=inicio.minusDays(antecedenciaPrimeira); return new JanelaInscricao(a,a.plusDays(duracaoPrimeira)); }
        public JanelaInscricao segundaFase(LocalDate inicio) { var a=inicio.plusDays(inicioSegunda); return new JanelaInscricao(a,a.plusDays(duracaoSegunda)); }
        public boolean cancelamentoImediato(LocalDate inicio,LocalDate data) { return !data.isAfter(inicio.plusDays(cancelamentoImediatoAte)); }
        public boolean permiteCancelamento(LocalDate inicio,LocalDate fim,LocalDate data) { return !data.isAfter(inicio.plusDays(cancelamentoPedidoAte)) && !data.isAfter(fim); }
        public boolean permiteTransferencia(LocalDate inicio,LocalDate fim,LocalDate data) { return !data.isAfter(inicio.plusDays(transferenciaAte)) && !data.isAfter(fim); }
        public boolean permiteRetificacao(LocalDate fim,LocalDate data) { return data.isAfter(fim) && !data.isAfter(fim.plusDays(retificacaoDocenteDias)); }
        public String canonico() { return antecedenciaPrimeira+"|"+duracaoPrimeira+"|"+inicioSegunda+"|"+duracaoSegunda+"|"+cancelamentoImediatoAte+"|"+cancelamentoPedidoAte+"|"+transferenciaAte+"|"+retificacaoDocenteDias+"|"+toleranciaAtraso+"|"+frequenciaPercentual.toPlainString()+"|"+mediaDispensaPf.toPlainString()+"|"+mediaFinalMinima.toPlainString(); }
    }
    private static BigDecimal decimal(BigDecimal n,int max,String campo) {
        if(n==null || n.signum()<0 || n.compareTo(BigDecimal.valueOf(max))>0 || n.stripTrailingZeros().scale()>1)
            throw new IllegalArgumentException("Confira "+campo+": de 0 a "+max+", com até uma casa decimal.");
        return n.setScale(1);
    }
    public record Criar(Regras regras,String motivo) { public Criar { Objects.requireNonNull(regras); motivo=Matricula.validarMotivo(motivo); } }
    public record Versao(long id,int numero,Regras regras,String motivo,long autor,LocalDateTime data,String assinatura) { }
    public record Periodo(long id,String descricao,LocalDate inicio,LocalDate fim,String situacao,int versaoVinculo,
            Long versaoParametros,Regras regras,long turmas,long inscricoes,boolean futuro,String assinatura) { }
    public record Vincular(long periodo,int versaoVinculo,Long versaoParametros,String assinatura,String motivo) {
        public Vincular {
            HorarioAula.validarId(periodo); if(versaoParametros!=null) HorarioAula.validarId(versaoParametros);
            if(versaoVinculo<0 || versaoVinculo==Integer.MAX_VALUE) throw new IllegalArgumentException("Versão de vínculo inválida.");
            if(assinatura==null || !assinatura.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Confira a prévia do período.");
            motivo=Matricula.validarMotivo(motivo);
        }
    }
    public record Historico(long id,long periodo,int versaoVinculo,Long anterior,Long nova,String motivo,long autor,LocalDateTime data) { }
}
