package training.configserverdemo;

import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

public class ClearCachesEvent extends RemoteApplicationEvent {

    public ClearCachesEvent(Object source, String originService, Destination destination) {
        super(source, originService, destination);
    }
}
