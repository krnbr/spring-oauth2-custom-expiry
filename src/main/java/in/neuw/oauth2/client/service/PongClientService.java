package in.neuw.oauth2.client.service;


import com.fasterxml.uuid.Generators;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class PongClientService {

    private final WebClient webClientStandard;
    private final WebClient webClientNotStandard;

    public PongClientService(final WebClient webClientStandard,
                             final WebClient webClientNotStandard) {
        this.webClientStandard = webClientStandard;
        this.webClientNotStandard = webClientNotStandard;
    }

    public String getPongStandard() {
        return webClientStandard
                .get().uri("/pong")
                .header("x-txn-id", Generators.timeBasedGenerator().generate().toString())
                .retrieve().bodyToMono(String.class).block();
    }

    public String getPongNonStandard() {
        return webClientNotStandard
                .get().uri("/pong")
                .header("x-txn-id", Generators.timeBasedGenerator().generate().toString())
                .retrieve().bodyToMono(String.class).block();
    }
}
