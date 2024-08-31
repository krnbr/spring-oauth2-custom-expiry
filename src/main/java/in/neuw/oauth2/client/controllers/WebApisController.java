package in.neuw.oauth2.client.controllers;

import com.fasterxml.uuid.Generators;
import in.neuw.oauth2.client.service.PongClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("/apis/v1")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class WebApisController {

    private final Logger logger = LoggerFactory.getLogger(WebApisController.class);
    private final PongClientService pongClientService;

    WebApisController(final PongClientService pongClientService) {
        this.pongClientService = pongClientService;
    }

    @GetMapping("/ping")
    public String ping() {
        logger.info("Web API PING");
        return pongClientService.getPongStandard();
    }

    @GetMapping("/ping-custom")
    public String pingCustom() {
        logger.info("Web API PING custom");
        return pongClientService.getPongNonStandard();
    }

}
