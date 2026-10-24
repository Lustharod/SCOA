package br.edu.cefetmg.scoa.dominio;

/** Sala de aula; limites dos campos compatíveis com o modelo físico v7.5. */
public final class Sala {
    private final Long id;
    private final String identificacao;
    private final String localizacao;
    private final int capacidade;
    private final String situacao;

    public Sala(Long id, String identificacao, String localizacao, int capacidade, String situacao) {
        if (id != null) validarId(id);
        if (capacidade < 1 || capacidade > 65_535) {
            throw new IllegalArgumentException("A capacidade deve estar entre 1 e 65535.");
        }
        this.id = id;
        this.identificacao = validarTexto(identificacao, "Identificação", 50);
        this.localizacao = validarTexto(localizacao, "Localização", 150);
        this.capacidade = capacidade;
        this.situacao = validarTexto(situacao, "Situação", 20);
    }

    public static Sala nova(String identificacao, String localizacao, int capacidade) {
        return new Sala(null, identificacao, localizacao, capacidade, "ATIVO");
    }

    public static void validarId(long id) {
        // INT UNSIGNED do MySQL ultrapassa o limite positivo de int em Java.
        if (id < 1 || id > 4_294_967_295L) {
            throw new IllegalArgumentException("O identificador da sala é inválido.");
        }
    }

    private static String validarTexto(String valor, String campo, int tamanhoMaximo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " é obrigatória.");
        }
        String texto = valor.strip();
        if (texto.codePointCount(0, texto.length()) > tamanhoMaximo) {
            throw new IllegalArgumentException(campo + " deve ter até " + tamanhoMaximo + " caracteres.");
        }
        return texto;
    }

    public Long getId() { return id; }
    public String getIdentificacao() { return identificacao; }
    public String getLocalizacao() { return localizacao; }
    public int getCapacidade() { return capacidade; }
    public String getSituacao() { return situacao; }
}
