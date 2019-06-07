# Quarkus Greeter sample with Istio

A simple distributed application with 2 components (`greeter-service` and `greeter-client`).
We'll deploy `greeter-service` with 3 different versions to illustrate Istio traffic management and policy enforcing capabalities.

Tested configuration:
* Quarkus: 0.16.0
* OpenShift Container Platform: 3.11.88
* OpenShift Service Mesh (Istio): 0.10.0 installed through Maistra Operator

## Prepare a project for building and deploying the components

Into your OpenShift cluster, create a new project to host your builds and components. As per OpenShift Service Mesh requirements, this project should provide extended capabilities for default service account.

```
$ oc new-project istio-greeter
$ oc adm policy add-scc-to-user anyuid -z default -n istio-greeter
$ oc adm policy add-scc-to-user privileged -z default -n istio-greeter
```

## Build the components

Follow this 2 documents for building:
* Build `greeter-service` with [build.md](./greeter-service/build.md),
* Build `greeter-client` with [build.md](./greeter-client/build.md).

## Deploy the components

Follow this 2 documents for deploying:
* Deploy `greeter-service` with [deploy.md](./greeter-service/deploy.md),
* Deploy `greeter-client` with [deploy.md](./greeter-client/deploy.m)].

## Demonstrate

### Initial state

* No `destinationrules` nor `virtualservices` present into your `istio-greeter` project.
* You may want to launch `./greeter-client/poll.sh` script to show that default round-robin Kubernetes service distribution applies.
* You may want to open Kiali from the Route into `istio-system` to show the graph.

### Enable traffic management

We'll focus on `greeter-service` and we're gonna route all traffic to `v1` :

```
$ oc apply -f ./istiofiles/dr-greeter-service.yml
$ oc apply -f ./istiofiles/vs-greeter-service-v1.yml
```

* Check routing through Kiali console
* Check distributed tracing enablement through Kiali / Jaeger integration
* Check Istio Grafana dashboards

### Enable canary release and test of version v2

Leave general traffic on v1, allow specified users with header `x-channel: canary` to acces v2 in primer.

```
$ oc replace -f istiofiles/vs-greeter-service-v1-v2-canary.yml
```

Now test with this URL, that shoul reach `v2` service : 

```
$ curl "http://$(oc get route | grep greeter-client | awk '{print $2}')/api/greet/Laurent" -H 'x-channel: canary'
```

* Talk about some other matching rules availbalble (here)[https://istio.io/docs/reference/config/networking/v1alpha3/virtual-service/#HTTPMatchRequest]
* Optionnaly discuss header propagation in Quarkus and Micropofile application

### Split traffic between 2 versions

Now apply a new virtual service definition to allow splitted traffic :

```
$ oc replace -f ./istiofile/vs-greeter-service-v1-70-v2-30.yml
```

* Check routing through Kiali console

### What if my service is faulty ?

Scale the number of `v2` pods to at least 2.
Connect into a `v2` container (from `oc get pods`) and invoke a special endpoint that makes the service return `503`responses:

```
$ oc rsh greeter-service-v2-6bbd455789-4jv29
Defaulting container name to greeter-service.
Use 'oc describe pod/greeter-service-v2-6bbd455789-4jv29 -n istio-greeter' to see all of the containers in this pod.
sh-4.4# curl localhost:8080/api/greet/flag/misbehave
Following requests to / will return a 503
exit
```

* See the results when polling the service

Now apply some new `destinationrule` and `virtualservice` definitions:

```
$ oc replace -f ./istiofiles/dr-greeter-service-cb.yml
$ oc replace -f ./istiofiles/vs-greeter-service-v1-50-v2-50-retry.yml
```

* See the results when polling the service
* Details Circuit Breaker configuration 
* Details Retris configuration and explain why you need both.

Reset pod so that it responds 200.

```
$ oc rsh greeter-service-v2-6bbd455789-4jv29
Defaulting container name to greeter-service.
Use 'oc describe pod/greeter-service-v2-6bbd455789-4jv29 -n istio-greeter' to see all of the containers in this pod.
sh-4.4# curl localhost:8080/api/greet/flag/behave
Following requests to / will return 200
sh-4.4# exit
```

### What if my service is slow ?

Apply some new routing roules, so that we reach `v3` service:

```
$ oc replace -f ./istiofiles/dr-greeter-service.yml
$ oc replace -f ./istiofiles/vs-greeter-service-v1-70-v3-30.yml
```

Scale the number of `v3` pods to at least 2.
Connect into a `v3` container (from `oc get pods`) and invoke a special endpoint that makes the service return add a `3s` timeout:

```
$ oc rsh greeter-service-v3-99b7457cf-rw5lv
Defaulting container name to greeter-service.
Use 'oc describe pod/greeter-service-v3-99b7457cf-rw5lv -n istio-greeter' to see all of the containers in this pod.
sh-4.4# curl localhost:8080/api/greet/flag/timeout
Following requests to / will wait 3s
sh-4.4# exit
```

Now apply a new definition for VirtualService, allowing to check for a defined timeout before returning a response (thus allowing the invoking service to wait too long and thus making the whole system collape).

```
$ oc replace -f ./istiofiles/vs-greeter-service-v1-50-v3-50-timeout.yml
```

Restart the polling script and see the invocations traces:

```
Greeting result => Hello Laurent from 5947f55fcf-48mqn
Greeting result => Namaste Laurent from 99b7457cf-flj4b
Greeting result => Hello Laurent from 5947f55fcf-48mqn
Greeting result => Error: 504 - upstream request timeout
Greeting result => Hello Laurent from 5947f55fcf-48mqn
Greeting result => Namaste Laurent from 99b7457cf-flj4b
Greeting result => Hello Laurent from 5947f55fcf-48mqn
Greeting result => Hello Laurent from 5947f55fcf-48mqn
Greeting result => Error: 504 - upstream request timeout
```

* `Error: 504 responses` are produced before waiting 3s, just after 1s
* Timedout pod is still called in a round-robin fashion, it is not removed from pool

You can now apply a new DestinationRule definition to allow outlier detection and temporary removing from pool:

```
$ oc replace -f ./istiofiles/dr-greeter-service-cb-v1-v3.yml
```

* Details Circuit Breaker configuration 

Now, `504` errors may occur just once or twice before being detected as outlier and removed from pool for `30s`. If letting the polling script a long time, you should see this errors frequence droppped as the eviction algorithm takes care of the number of time is has been detected as an outlier to weight the evistion time.

Reset pod so that it responds immediatly.

```
$ oc rsh greeter-service-v3-99b7457cf-rw5lv
Defaulting container name to greeter-service.
Use 'oc describe pod/greeter-service-v3-99b7457cf-rw5lv -n istio-greeter' to see all of the containers in this pod.
sh-4.4# curl localhost:8080/api/greet/flag/timein
Following requests to / will not wait
sh-4.4# exit
```

### Super resilient invokations with retries, timeout and circuit-breaker

Finally, combine all what we learned for super resilient invokations!!

Scale the number of `v2` pods to at least 2 and apply misbehaviour on 1 of them.
Scale the number of `v3` pods to at least 2 and apply timeout on 1 of them.

```
$ oc replace -f ./istiofiles/dr-greeter-service-cb-all.yml
$ oc replace -f ./istiofiles/vs-greeter-service-all-retry-timeout.yml
```

* Review configurations
* See that we lower the `perTryTimeout` value to fit with the global `timeout`
* Take some time to introspect the graph and metrices in Kiali (checking for example for response code on `v2` and `v3` services)

### Enable MTLS

Now just pursuing previous sequence, use Kiali to visualise and edit `greeter-service` destinationrule. Add the `tls` node under `trafficPolicy` like this:

```
spec:
  host: greeter-service
  trafficPolicy:
    tls:
      mode: ISTIO_MUTUAL
  ...
```

Save the definition and get back to the graphic in Kiali, making sure to display the secured badge. Wait for a few seconds and refresh, now you've got mutual TLS enabled between all the pods!
