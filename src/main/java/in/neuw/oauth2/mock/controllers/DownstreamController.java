package in.neuw.oauth2.mock.controllers;

import com.fasterxml.uuid.Generators;
import in.neuw.oauth2.mock.models.NotSoStandardTokenResponse;
import in.neuw.oauth2.mock.models.StandardTokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RestController
@RequestMapping("/mock/apis/")
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
class DownstreamController {

    private final Logger logger = LoggerFactory.getLogger(DownstreamController.class);

    @GetMapping("pong")
    String getPong(final @RequestHeader("x-txn-id") String txnId,
                   final @RequestHeader(AUTHORIZATION) String token) {
        logger.info(
            "the received token value is - {} at the mocked downstream resource endpoint",
            token.replace("Bearer ", "")
        );
        return "pong with a random id -> "+txnId + " -> received at -> "+ Instant.now();
    }

    @PostMapping("token")
    StandardTokenResponse standardToken() {
        // not relying on the input, not required, since this is a mock.
        return new StandardTokenResponse()
                .setAccessToken(Generators.timeBasedGenerator().generate().toString())
                .setExpiresIn(3600)
                .setTokenType("Bearer");
    }

    @PostMapping("wrong_token")
    NotSoStandardTokenResponse wrongToken() {
        // not relying on the input, not required, since this is a mock.
        return new NotSoStandardTokenResponse()
                .setAccessToken(Generators.timeBasedGenerator().generate().toString())
                .setExpiresIn(3600)
                .setTokenType("Bearer");
    }

}
