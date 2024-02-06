package training.configclientdemo;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@EnableConfigurationProperties(DemoProperties.class)
@Slf4j
public class HelloController
{

    private DemoProperties demoProperties;

    @GetMapping("/api/hello")
    public String hello() {
        log.debug("Hello called");
        return demoProperties.getPrefix();
    }
}
