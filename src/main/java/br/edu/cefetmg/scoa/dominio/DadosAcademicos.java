package br.edu.cefetmg.scoa.dominio;

final class DadosAcademicos {
    private DadosAcademicos() { }
    static String texto(String valor, String campo, int limite) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Informe " + campo + ".");
        String texto = valor.strip();
        if (texto.codePointCount(0,texto.length()) > limite) throw new IllegalArgumentException(campo + " deve ter até " + limite + " caracteres.");
        return texto;
    }
}
