package in.neuw.oauth2.client.controllers;

import in.neuw.oauth2.client.service.PongClientReactiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/apis/v1")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
class WebFluxApisController {

    private final Logger logger = LoggerFactory.getLogger(WebFluxApisController.class);

    private final PongClientReactiveService pongClientReactiveService;

    WebFluxApisController(final PongClientReactiveService pongClientReactiveService) {
        this.pongClientReactiveService = pongClientReactiveService;
    }

    @GetMapping("/ping")
    public Mono<String> ping() {
        logger.info("Web API PING");
        return pongClientReactiveService.getPongStandard();
    }

    @GetMapping("/ping-custom")
    public Mono<String> pingCustom() {
        logger.info("Web API PING custom");
        return pongClientReactiveService.getPongNonStandard();
    }

}
