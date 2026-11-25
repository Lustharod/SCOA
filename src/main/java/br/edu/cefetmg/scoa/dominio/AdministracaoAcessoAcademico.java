package br.edu.cefetmg.scoa.dominio;

import java.time.LocalDateTime;
import java.util.*;

/** Perfil técnico não cria aluno, professor, coordenação ou gestão. */
public final class AdministracaoAcessoAcademico {
    private AdministracaoAcessoAcademico() { }
    public enum Codigo {
        ADMIN("SCOA_ACESSO_ADMIN","Administrar acesso e operações técnicas; não substitui responsabilidade docente/gestão"),
        CONSULTAR("SCOA_ACADEMICO_CONSULTAR","Consultar cadastros e processos acadêmicos da secretaria"),
        MANTER("SCOA_ACADEMICO_MANTER","Manter cadastros e operações acadêmicas da secretaria"),
        SOLICITAR("SCOA_PROCESSO_SOLICITAR","Solicitar conciliação/revisão acadêmica"),
        DECIDIR("SCOA_PROCESSO_DECIDIR","Analisar conciliação/revisão acadêmica"),
        DOCUMENTOS_CONSULTAR("SCOA_DOCUMENTOS_CONSULTAR","Consultar fila de documentos acadêmicos"),
        DOCUMENTOS_ATENDER("SCOA_DOCUMENTOS_ATENDER","Atender documentos acadêmicos na secretaria"),
        RELATORIOS("SCOA_RELATORIOS_GERAR","Conferir, gerar e consultar demonstrativos acadêmicos MEC");
        private final String codigo,descricao;
        Codigo(String codigo,String descricao) { this.codigo=codigo; this.descricao=descricao; }
        public String codigo() { return codigo; }
        public String descricao() { return descricao; }
        public static boolean conhecido(String codigo) { return Arrays.stream(values()).anyMatch(c->c.codigo.equals(codigo)); }
    }
    public record Perfil(long id,String nome,String descricao,String situacao,int versao,List<String> permissoes,List<Long> contas) {
        public Perfil { permissoes=List.copyOf(permissoes); contas=List.copyOf(contas); }
        public boolean escopoAcademico() { return permissoes.stream().allMatch(Codigo::conhecido); }
    }
    public record Conta(long id,String identificacao,String situacao,int versao,List<Long> perfis,List<String> permissoesEfetivas) {
        public Conta { perfis=List.copyOf(perfis); permissoesEfetivas=List.copyOf(permissoesEfetivas); }
    }
    public record NovoPerfil(String nome,String descricao,List<String> permissoes) {
        public NovoPerfil { nome=Equivalencia.texto(nome,"nome do perfil",100); descricao=Equivalencia.texto(descricao,"descrição",255); permissoes=codigos(permissoes); }
    }
    public record AlterarPerfil(long perfil,int versao,String nome,String descricao,String situacao,List<String> permissoes,String motivo) {
        public AlterarPerfil {
            HorarioAula.validarId(perfil); validarVersao(versao);
            nome=Equivalencia.texto(nome,"nome do perfil",100); descricao=Equivalencia.texto(descricao,"descrição",255);
            situacao=estado(situacao); permissoes=codigos(permissoes); motivo=Matricula.validarMotivo(motivo);
        }
    }
    public record VincularPerfil(long usuario,int versaoConta,long perfil,int versaoPerfil,boolean conceder,String motivo) {
        public VincularPerfil { HorarioAula.validarId(usuario); HorarioAula.validarId(perfil); validarVersao(versaoConta); validarVersao(versaoPerfil); motivo=Matricula.validarMotivo(motivo); }
    }
    public record AlterarConta(long usuario,int versao,String situacao,String motivo) {
        public AlterarConta { HorarioAula.validarId(usuario); validarVersao(versao); situacao=estado(situacao); motivo=Matricula.validarMotivo(motivo); }
    }
    public record Evento(long id,Long conta,Long perfil,String operacao,int versao,String anterior,String novo,String motivo,long autor,LocalDateTime data) { }
    private static void validarVersao(int versao) { if(versao<0 || versao==Integer.MAX_VALUE) throw new IllegalArgumentException("Versão inválida."); }
    private static String estado(String valor) {
        if(!Set.of("ATIVO","INATIVO").contains(valor)) throw new IllegalArgumentException("Use ATIVO ou INATIVO."); return valor;
    }
    private static List<String> codigos(List<String> valores) {
        if(valores==null || new HashSet<>(valores).size()!=valores.size() || valores.stream().anyMatch(c->c==null || !Codigo.conhecido(c)))
            throw new IllegalArgumentException("Selecione permissões acadêmicas conhecidas, sem repetição.");
        return valores.stream().sorted().toList();
    }
}
