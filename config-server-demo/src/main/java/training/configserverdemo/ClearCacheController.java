package training.configserverdemo;

import lombok.AllArgsConstructor;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor

public class ClearCacheController {

    private ApplicationEventPublisher publisher;

    private BusProperties busProperties;

    private Destination.Factory factory;

    @DeleteMapping("/api/caches")
    public void clear() {
        publisher.publishEvent(new ClearCachesEvent(this, busProperties.getId(), factory.getDestination("config-client-demo")));
    }
}
