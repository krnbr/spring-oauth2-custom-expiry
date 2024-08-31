package in.neuw.oauth2.client.service;

import com.fasterxml.uuid.Generators;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class PongClientReactiveService {

    private final WebClient webClientStandard;
    private final WebClient webClientNotStandard;

    public PongClientReactiveService(final WebClient webClientStandard,
                             final WebClient webClientNotStandard) {
        this.webClientStandard = webClientStandard;
        this.webClientNotStandard = webClientNotStandard;
    }

    public Mono<String> getPongStandard() {
        return webClientStandard
                .get().uri("/pong")
                .header("x-txn-id", Generators.timeBasedGenerator().generate().toString())
                .retrieve().bodyToMono(String.class);
    }

    public Mono<String> getPongNonStandard() {
        return webClientNotStandard
                .get().uri("/pong")
                .header("x-txn-id", Generators.timeBasedGenerator().generate().toString())
                .retrieve().bodyToMono(String.class);
    }

}
