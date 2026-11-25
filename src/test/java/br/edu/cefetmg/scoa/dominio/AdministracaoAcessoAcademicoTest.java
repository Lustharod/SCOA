package br.edu.cefetmg.scoa.dominio;

import br.edu.cefetmg.scoa.dominio.AdministracaoAcessoAcademico.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdministracaoAcessoAcademicoTest {
    @Test void catalogoNaoAceitaCodigoArbitrarioOuRepetido() {
        assertThrows(IllegalArgumentException.class,()->new NovoPerfil("Nome","Descrição",List.of("PERMISSAO_INVENTADA")));
        assertThrows(IllegalArgumentException.class,()->new NovoPerfil("Nome","Descrição",List.of(Codigo.ADMIN.codigo(),Codigo.ADMIN.codigo())));
        assertDoesNotThrow(()->new NovoPerfil("Nome","Descrição",List.of()));
    }
    @Test void alteracoesExigemVersaoEstadoMotivoEReferenciasValidas() {
        assertThrows(IllegalArgumentException.class,()->new AlterarConta(1,-1,"INATIVO","Motivo"));
        assertThrows(IllegalArgumentException.class,()->new AlterarConta(1,0,"ATIVO"," "));
        assertThrows(IllegalArgumentException.class,()->new AlterarConta(1,0,"DESCONHECIDO","Motivo"));
        assertThrows(IllegalArgumentException.class,()->new VincularPerfil(0,0,1,0,true,"Motivo"));
    }
}
