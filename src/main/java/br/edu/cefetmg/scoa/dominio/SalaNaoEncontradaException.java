package br.edu.cefetmg.scoa.dominio;

public final class SalaNaoEncontradaException extends RuntimeException {
    public SalaNaoEncontradaException(long id) {
        super("Sala " + id + " não encontrada.");
    }
}
