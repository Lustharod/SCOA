package br.edu.cefetmg.scoa.dominio;

import java.util.List;

public record Elegibilidade(List<String> motivos, int vagas, JanelaInscricao janela) {
    public Elegibilidade { motivos = List.copyOf(motivos); }
    public boolean permite() { return motivos.isEmpty(); }
    public void exigir() {
        if (!permite()) throw new IllegalArgumentException(String.join("; ", motivos));
    }
}
