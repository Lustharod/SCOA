package br.edu.cefetmg.scoa.dominio;

import java.util.List;

public record Elegibilidade(List<String> motivos, int vagas, JanelaInscricao janela, JanelaInscricao segundaFase) {
    public Elegibilidade { motivos = List.copyOf(motivos); }
    public boolean permite() { return motivos.isEmpty(); }
    public boolean dentroDaJanela(java.time.LocalDate data) {
        return janela.permite(data) || segundaFase.permite(data);
    }
    public void exigir() {
        if (!permite()) throw new IllegalArgumentException(String.join("; ", motivos));
    }
}
