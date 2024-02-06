package training.configclientdemo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClearCachesEventListener {

    @EventListener
    public void handleEvent(ClearCachesEvent event) {
        log.info("Event handled: {}", event);
    }
}
