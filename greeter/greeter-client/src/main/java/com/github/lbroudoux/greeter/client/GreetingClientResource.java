package com.github.lbroudoux.greeter.client;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@Path("/api/greet")
public class GreetingClientResource {

    private final Logger logger = Logger.getLogger(getClass());

    private static final String RESPONSE_STRING_FORMAT = "Greeting result => %s\n";

    @Inject
    @RestClient
    GreetingServerService greetingService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}")
    public Response greeting(@PathParam("name") String name) {
        try {
            String response = greetingService.greetByByName(name);
            return Response.ok(String.format(RESPONSE_STRING_FORMAT, response)).build();
        } catch (WebApplicationException wae) {
            Response response = wae.getResponse();
            logger.warn("Non HTTP 20x trying to get the response from greeting service: " + response.getStatus());
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(String.format(RESPONSE_STRING_FORMAT,
                            String.format("Error: %d - %s", response.getStatus(), response.readEntity(String.class)))
                    )
                    .build();
        } catch (ProcessingException pe) {
            logger.warn("Exception trying to get the response from greeting service.", pe);
            return Response
                    .status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(String.format(RESPONSE_STRING_FORMAT, pe.getCause().getClass().getSimpleName() + ": " + pe.getCause().getMessage()))
                    .build();

        }
    }
}