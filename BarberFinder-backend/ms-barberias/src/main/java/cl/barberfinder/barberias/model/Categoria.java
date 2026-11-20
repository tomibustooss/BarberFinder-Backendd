package cl.barberfinder.barberias.model;

/** Mismas categorías que ShopCategory en la app. */
public enum Categoria {
    ECONOMICA("Económica"),
    INTERMEDIA("Intermedia"),
    PREMIUM("Premium");

    private final String etiqueta;

    Categoria(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
