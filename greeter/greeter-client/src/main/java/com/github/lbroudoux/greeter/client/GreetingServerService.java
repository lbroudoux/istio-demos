package com.github.lbroudoux.greeter.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

@Path("/api/greet")
@RegisterRestClient
public interface GreetingServerService {

    @GET
    @Path("/{name}")
    @Produces("text/plain")
    String greetByByName(@PathParam("name") String name);
}