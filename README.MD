The project repository works in hand with the blog [here](https://medium.com/@neuw/spring-boot-oauth2-client-override-expiry-53d62bf1f15d
) ▸ https://medium.com/@neuw/spring-boot-oauth2-client-override-expiry-53d62bf1f15d


There are two main profiles

**▸ servlets**

This will run all the config, controllers, services, etc. in servlets context.

**▸ reactive**

For all the config, controllers, services, etc. based on reactive/ webflux setup

In short this project includes two projects in a single one, to compare & understand two different paradigms easily.

Used 

`@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)` 
                                     & 
`@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)`

to load almost all the classes based on different paradigms of servlets & reactive webflux worlds

The Mocked downstream endpoints are included in `the in.neuw.oauth2.mock` package, presently hard coded.

▸ Why mock within the main resource?
─ Since this is a POC(**P**roof Of **C**oncept), too many separate modules(microservice) would need extra time and mind to understand the situation. 

The mocks have three endpoints :-

─ Resource Endpoint - /mock/apis/pong

─ Standard Token Endpoint - /mock/apis/token

─ Non-Standard Token Endpoint - /mock/apis/wrong_token