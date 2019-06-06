
## Deploying the greeter-client

After having build your image on OpenShift (see [build.md]), create a new App and then the corresponding Route.

```
$ oc new-app --image-stream=greeter-client:latest --env=QUARKUS_PROFILE=kube
$ oc expose service greeter-client
```

We have to provide the `QUARKUS_PROFILE` environment variable to make Quarkus use a diffrent set of properties from `application.properties` when bootstrapping the application.

You can check everything works fine by calling the Route with some arguments:

```
$ curl "http://$(oc get route | grep greeter-client | awk '{print $2}')/api/greet/Laurent"
Greeting result => Bonjour Laurent from 3290ddbe938a
```

## Activate Istio side-car

As we have created a DeploymentConfig using CLI, we should now mark this Deployment has being intrumented by Istio controller.
On OpenShift Service Mesh, this could be simply done using `sidecar.istio.io/inject` annotation. So just patch the DeploymentConfig like this:

```
$ oc patch dc/greeter-client --type=json -p '[{"op":"add", "path":"/spec/template/metadata/annotations/-", "value": {"sidecar.istio.io/inject": "true"}}'
```

Wait for a few seconds and re-deployment should occur with side-car injected.