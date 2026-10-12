package dev.codelad.lunchservice.repository;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * name = "venue-service" — имя сервиса в Service Discovery (Eureka, Consul, Kubernetes DNS). В Kubernetes это будет имя Deployment/Service: venue-service.
 * Не маппим JSON-ответ, а просто проверяем статус. Если Venue есть — будет 200 OK, если нет — 404 Not Found.
 */
@Repository
@FeignClient(
        name = "${venue.service.name:venue-service}",
        path = "${venue.service.path:/api/v1/venues}",
        url = "${venue.service.url:http://localhost:8081}"
)
public interface VenueClient {

    @GetMapping("/{venueId}")
    ResponseEntity<Void> getVenue(@PathVariable Long venueId);
}

