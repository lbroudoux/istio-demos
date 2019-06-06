
## Deploying the version 1

After having build your image on OpenShift (see [build.md]), create a new Deployment and then the corresponding service. You can also create a Route to heck everything is ok.

```
$ oc apply -f openshift/Deployment-v1.yml
$ oc apply -f openshift/Service.yml
$ oc expose service/greeter-service
```

## Change something and deploy the version 2

Open `src/main/com/github/lbroudoux/greeter/service/GreetingService.java` into your favorite IDE and make some change to the greeting returned. For example, for this version 2, you can replace `Hello` with `Bonjour`.

Rebuild the Quarkus, native image, relaunch the OpenShift build and tag your image as version `v2`.

```
$ ./mvnw clean package -Pnative -Dnative-image.docker-build=true
$ oc start-build greeter-service --from-dir=. --follow
$ oc tag $(oc project -q)/greeter-service:latest $(oc project -q)/greeter-service:v2
```

Finally, create a new deployment on OpenShift:

```
$ oc apply -f openshift/Deployment-v2.yml
```

## Change something and deploy the version 3 (again)

Open `src/main/com/github/lbroudoux/greeter/service/GreetingService.java` into your favorite IDE and make some change to the greeting returned. For example, for this version 2, you can replace `Bonjour` with `Namaste`.

Rebuild the Quarkus, native image, relaunch the OpenShift build and tag your image as version `v3`.

```
$ ./mvnw clean package -Pnative -Dnative-image.docker-build=true
$ oc start-build greeter-service --from-dir=. --follow
$ oc tag $(oc project -q)/greeter-service:latest $(oc project -q)/greeter-service:v3
```

Finally, create a new deployment on OpenShift:

```
$ oc apply -f openshift/Deployment-v3.yml
```