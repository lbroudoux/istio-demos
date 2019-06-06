package com.github.lbroudoux.greeter.service;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.jboss.logging.Logger;


@Path("/api/greet")
public class GreetingResource {

    private final Logger logger = Logger.getLogger(getClass());

    @Inject
    GreetingService service;

    /** Counter to help us see the lifecycle */
    private int count = 0;
    /** Flag for waiting when enabled */
    private boolean timeout = false;
    /** Flag for throwing a 503 when enabled */
    private boolean misbehave = false;

    private static String HOSTNAME = null;

    private static String parseContainerIdFromHostname(String hostname) {
        System.err.println("hostname is " + hostname);
        System.err.println("  after subst: " + hostname.replaceAll("greeter-service-v\\d+-", ""));
        return hostname.replaceAll("greeter-service-v\\d+-", "");
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{name}")
    public Response greeting(@PathParam("name") String name) {
        if (HOSTNAME == null) {
            HOSTNAME = parseContainerIdFromHostname(System.getenv().getOrDefault("HOSTNAME", "unknown"));
        }
        if (timeout) {
            timeout();
        }
        if (misbehave) {
            return doMisbehavior();
        }
        return Response.ok(service.greeting(name) + " from " + HOSTNAME).build();
    }

    @GET
    @Path("/flag/timeout")
    public Response flagTimeout() {
        this.timeout = true;
        logger.debug("'timeout' has been set to 'true'");
        return Response.ok("Following requests to / will wait 3s\n").build();
    }

    @GET
    @Path("/flag/timein")
    public Response flagTimein() {
        this.timeout = false;
        logger.debug("'timeout' has been set to 'false'");
        return Response.ok("Following requests to / will not wait\n").build();
    }

    @GET
    @Path("/flag/misbehave")
    public Response flagMisbehave() {
        this.misbehave = true;
        logger.debug("'misbehave' has been set to 'true'");
        return Response.ok("Following requests to / will return a 503\n").build();
    }

    @GET
    @Path("/flag/behave")
    public Response flagBehave() {
        this.misbehave = false;
        logger.debug("'misbehave' has been set to 'false'");
        return Response.ok("Following requests to / will return 200\n").build();
    }

    private void timeout() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.info("Thread interrupted");
        }
    }

    private Response doMisbehavior() {
        logger.debug(String.format("Misbehaving %d", count));
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(String.format("greeter-service misbehavior from '%s'\n", HOSTNAME)).build();
    }

}