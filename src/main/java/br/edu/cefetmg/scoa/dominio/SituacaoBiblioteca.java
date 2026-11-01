package br.edu.cefetmg.scoa.dominio;

public record SituacaoBiblioteca(Estado estado, String detalhe) {
    public enum Estado { REGULAR, IMPEDITIVA, INDISPONIVEL }
    public void exigirRegular() {
        if (estado != Estado.REGULAR)
            throw new IllegalArgumentException("Matrícula não efetivada. Biblioteca: " + detalhe);
    }
}
