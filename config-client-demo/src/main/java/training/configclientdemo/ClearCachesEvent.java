package training.configclientdemo;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.cloud.bus.event.Destination;
import org.springframework.cloud.bus.event.RemoteApplicationEvent;

@NoArgsConstructor
public class ClearCachesEvent extends RemoteApplicationEvent {

    public ClearCachesEvent(Object source, String originService, Destination destination) {
        super(source, originService, destination);
    }
}
