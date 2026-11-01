package br.edu.cefetmg.scoa.dominio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class BaseAcademicaTest {
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings={" ","\t\n"})
    void p0501CamposObrigatorios(String texto) {
        assertThrows(IllegalArgumentException.class, () -> new Aluno(null,null,texto,"ATIVO"));
        assertThrows(IllegalArgumentException.class, () -> new Curso(null,texto,"ATIVO"));
        assertThrows(IllegalArgumentException.class, () -> new EstruturaCurricular(null,1,texto,"ATIVO"));
    }
    @Test void p0501LimitesUnicodeENormalizacao() {
        String cem="😀".repeat(100), centoCinquenta="á".repeat(150);
        assertEquals(cem,new Aluno(null,null,"  "+cem+"  "," ATIVO ").identificacao());
        assertEquals(centoCinquenta,new Curso(null,centoCinquenta,"ATIVO").nome());
        assertEquals(cem,new EstruturaCurricular(null,1,cem,"ATIVO").identificacao());
        assertThrows(IllegalArgumentException.class,()->new Aluno(null,null,cem+"a","ATIVO"));
        assertThrows(IllegalArgumentException.class,()->new Curso(null,centoCinquenta+"a","ATIVO"));
        assertThrows(IllegalArgumentException.class,()->new EstruturaCurricular(null,1,cem+"a","ATIVO"));
    }
    @ParameterizedTest @ValueSource(longs={0,-1,4294967296L})
    void p0508IdentificadoresInvalidos(long id) {
        assertThrows(IllegalArgumentException.class,()->new Aluno(id,null,"Aluno","ATIVO"));
        assertThrows(IllegalArgumentException.class,()->new Aluno(null,id,"Aluno","ATIVO"));
        assertThrows(IllegalArgumentException.class,()->new Curso(id,"Curso","ATIVO"));
        assertThrows(IllegalArgumentException.class,()->new EstruturaCurricular(null,id,"Estrutura","ATIVO"));
    }
    @Test void p0501SituacoesEIdentidadesValidas() {
        assertEquals(4294967295L,new Aluno(4294967295L,1L,"Aluno","INATIVO").id());
        assertThrows(IllegalArgumentException.class,()->new Curso(null,"Curso"," "));
        assertThrows(IllegalArgumentException.class,()->new Aluno(null,null,"Aluno","x".repeat(21)));
        assertThrows(IllegalArgumentException.class,()->new EstruturaCurricular(0L,1,"E","ATIVO"));
    }
}
