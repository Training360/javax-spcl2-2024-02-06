package employees;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Random;

@Component
@Slf4j

public class ChaosFilter extends OncePerRequestFilter {

    private int delay = 0;

    private int faultPercent = 0;

    private final Random random = new Random();

    @Override
    @SneakyThrows
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var newFaultPercent = request.getParameter("faultPercent");
        if (newFaultPercent != null) {
            faultPercent = Integer.parseInt(newFaultPercent);
        }

        var newDelay = request.getParameter("delay");
        if (newDelay != null) {
            delay = Integer.parseInt(newDelay);
        }

        var randomThreshold = random.nextInt(100) + 1;
        if (faultPercent < randomThreshold) {
            log.info("We got lucky, no error occurred, {} < {}",
                    faultPercent, randomThreshold);
        } else {
            log.info("Bad luck, an error occurred, {} >= {}",
                    faultPercent, randomThreshold);
            throw new RuntimeException("Something went wrong...");
        }

        if (delay > 0) {
            log.info("Sleeping, {}", delay);
            Thread.sleep(delay);
        }

        filterChain.doFilter(request, response);
    }
}
