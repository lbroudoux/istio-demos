## Istio demonstration

`book-info` namespace on OpenShift environment with [Book Info](https://istio.io/docs/examples/bookinfo/) sample app loaded.

### 1) Initialisation

```
$ kubectl apply -f <(istioctl kube-inject -f httpbin.yaml)
$ kubectl apply -f <(istioctl kube-inject -f fortio-deploy.yaml)
```

or just apply directly the file as sidecar injection annotation has no been added ;-)

Now test it is working;

```
$ FORTIO_POD=$(oc get pod | grep fortio | awk '{ print $1 }')
$ oc exec -it $FORTIO_POD -c fortio /usr/local/bin/fortio -- load -curl  http://httpbin:8000/get
HTTP/1.1 200 OK
server: envoy
date: Wed, 06 Mar 2019 16:39:58 GMT
content-type: application/json
access-control-allow-origin: *
access-control-allow-credentials: true
content-length: 365
x-envoy-upstream-service-time: 74

{
  "args": {},
  "headers": {
    "Content-Length": "0",
    "Host": "httpbin:8000",
    "User-Agent": "istio/fortio-1.0.1",
    "X-B3-Sampled": "1",
    "X-B3-Spanid": "23a6e22a94616006",
    "X-B3-Traceid": "23a6e22a94616006",
    "X-Request-Id": "9338497a-1871-97b5-a6ba-a35f8eaf2e74"
  },
  "origin": "127.0.0.1",
  "url": "http://httpbin:8000/get"
}
```

### 2) Start playing !

Call the service with two concurrent connections (-c 2) and send 20 requests (-n 20):

```
$ oc exec -it $FORTIO_POD  -c fortio /usr/local/bin/fortio -- load -c 2 -qps 0 -n 20 -loglevel Warning http://httpbin:8000/get
[...]
Code 200 : 20 (100.0 %)
[...]
```

It’s interesting to see that almost all requests made it through! The istio-proxy does allow for some leeway.

```
$ oc exec -it $FORTIO_POD  -c fortio /usr/local/bin/fortio -- load -c 3 -qps 0 -n 20 -loglevel Warning http://httpbin:8000/get
[...]
Code 200 : 14 (70.0 %)
Code 503 : 6 (30.0 %)
[...]
```

Now you start to see the expected circuit breaking behavior. Only 70% of the requests succeeded and the rest were trapped by circuit breaking.

```
$ oc exec -it $FORTIO_POD  -c istio-proxy  -- sh -c 'curl localhost:15000/stats' | grep httpbin | grep pending
cluster.outbound|8000||httpbin.book-info.svc.cluster.local.upstream_rq_pending_active: 0
cluster.outbound|8000||httpbin.book-info.svc.cluster.local.upstream_rq_pending_failure_eject: 0
cluster.outbound|8000||httpbin.book-info.svc.cluster.local.upstream_rq_pending_overflow: 5
cluster.outbound|8000||httpbin.book-info.svc.cluster.local.upstream_rq_pending_total: 38
```
