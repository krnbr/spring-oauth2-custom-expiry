package in.neuw.oauth2.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan("in.neuw.oauth2.mock")
@ConditionalOnProperty(name = "mock.enabled", havingValue = "true")
public class MockConfig implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(MockConfig.class);

    @Override
    public void afterPropertiesSet() {
        logger.info("MockConfig initialized");
    }
}
