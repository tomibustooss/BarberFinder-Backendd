package cl.barberfinder.reservas.client;

import cl.barberfinder.reservas.exception.ApiException;
import cl.barberfinder.reservas.model.TipoServicio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.function.Supplier;

/**
 * Comunicación entre microservicios: ms-reservas consulta a ms-barberias
 * el precio real del servicio y el dueño de la barbería.
 */
@Component
public class BarberiasRestClient implements CatalogoBarberias {

    private final RestClient restClient;

    public BarberiasRestClient(RestClient.Builder builder,
                               @Value("${app.barberias.url}") String urlBarberias) {
        this.restClient = builder.baseUrl(urlBarberias).build();
    }

    @Override
    public PrecioServicio precio(Long barberiaId, TipoServicio servicio) {
        return llamar(() -> restClient.get()
                .uri("/api/barberias/{id}/precio?servicio={servicio}", barberiaId, servicio.name())
                .retrieve()
                .body(PrecioServicio.class));
    }

    @Override
    public BarberiaInfo barberia(Long barberiaId) {
        return llamar(() -> restClient.get()
                .uri("/api/barberias/{id}", barberiaId)
                .retrieve()
                .body(BarberiaInfo.class));
    }

    private <T> T llamar(Supplier<T> llamada) {
        try {
            T respuesta = llamada.get();
            if (respuesta == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "ms-barberias respondió sin datos.");
            }
            return respuesta;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw ApiException.noEncontrado("La barbería no existe.");
            }
            if (e.getStatusCode().is4xxClientError()) {
                throw ApiException.solicitudInvalida("La barbería o el servicio no son válidos.");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ms-barberias respondió con error.");
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de barberías no está disponible.");
        }
    }
}
