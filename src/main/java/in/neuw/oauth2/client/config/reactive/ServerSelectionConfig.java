package in.neuw.oauth2.client.config.reactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerSelectionConfig {

    private final Logger logger = LoggerFactory.getLogger(ServerSelectionConfig.class);

    /**
     * just for testing the code, single repo - two flavours at a common place!
     * this is needed to prefer netty instead of the tomcat based TomcatReactiveWebServerFactory for reactive context
     * may not be necessary for all, can be removed if one wants.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    public ReactiveWebServerFactory reactiveWebServerFactory() {
        logger.info("netty will be initialized!");
        return new NettyReactiveWebServerFactory();
    }

}
