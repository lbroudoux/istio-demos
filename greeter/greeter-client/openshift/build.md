## Produce a Quarkus native image suitable for containers

By default, the native executable is tailored for your operating system (Linux, macOS, Windows etc). Because the container may not use the same executable format as the one produced by your operating system, we will instruct the Maven build to produce an executable from inside a container:

```
./mvnw package -Pnative -Dnative-image.docker-build=true
```

The produced executable will be a 64 bit Linux executable, so depending on your operating system it may no longer be runnable. However, it’s not an issue as we are going to copy it to a container. The project generation has provided a `Dockerfile.native` in the `src/main/docker` directory.

Optionnaly, if you want to test locally using your docker daemon and if you didn’t delete the generated native executable, you can build the docker image with:

```
docker build -f src/main/docker/Dockerfile.native -t istio-demos/greeter-client .
```

## Building container images on OpenShift

On the OpenShift side, you may want to create a generic Source-to-image binary build configuration. This build will used the local `Dockerfile.native` that will be injected during the build. We do that by patching the BuildConfig once created:

```
$ oc new-build --binary --name=greeter-client -l app=greeter-client
$ oc patch bc/greeter-client -p '{"spec":{"strategy":{"dockerStrategy":{"dockerfilePath":"src/main/docker/Dockerfile.native"}}}}'
```

If you didn’t delete the generated native executable in `/target`directory, you can start the OpenShift build:

```
$ oc start-build greeter-client --from-dir=. --follow 
```