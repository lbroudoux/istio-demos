## Istio demonstration

`book-info` namespace on OpenShift environment with [Book Info](https://istio.io/docs/examples/bookinfo/) sample app loaded.

![app overview](https://istio.io/docs/examples/bookinfo/noistio.svg)

### 1) Default

`oc get virtualservices -n book-info` should return no resources.
`oc get destinationrules -n book-info` should return no resources.

Load-balancing between 3 versions of reviews is managed by OpenShift (round-robin). Check through Kiali.

### 2) Force everything on v1 of services

`oc apply -f destination-rule-all.yaml -n book-info`
`oc apply -f virtual-service-all-v1.yaml -n book-info`

We only go to review without stars (black or red). Check through Kiali.

### 3) Test a new v2 reviews release, only for jason user

`oc apply -f virtual-service-reviews-test-v2.yaml -n book-info`

Log into `productpage` application using `jason/jason` and check that we only got black stars.

Check through Kiali.

Check that regular un-authenticated user (from another browser session) is still redirected to reviews without ratings.

### 4) Check how v2 reviews release react when something goes wrong

Introduce some extra delays for ratings component only for `jason` user.

`oc apply -f virtual-service-ratings-test-delay.yaml -n book-info`

`productpage` only wait for 6 seconds (2 retries x 3 seconds), so that 7s makes everything fail. Check through Kiali.

Check that regular un-authenticated user (from another browser session) is still redirected to reviews without ratings.

We can also replace the delay by a regular HTTP error so that we can check that it fails fast.

`oc apply -f virtual-service-ratings-test-abort.yaml -n book-info`

### 5) We've fixed everything in reviews v3 and are confident

Split 50% of the traffic between v1 and v3 of reviews.

`oc apply -f virtual-service-reviews-50-v3.yaml -n book-info`

Check through Kiali. We're confident.

We're sending everything to v3 !!

`oc apply -f virtual-service-reviews-v3.yaml -n book-info`


## Troubleshooting

```
$> istioctl proxy-status
PROXY                                                  CDS        LDS        EDS               RDS          PILOT                           VERSION
details-v1-68f6dfb958-jnwj8.book-info                  SYNCED     SYNCED     SYNCED (100%)     SYNCED       istio-pilot-d99689994-2hhkw     1.0.0
istio-egressgateway-5d7f8fcc7b-l5pwg.istio-system      SYNCED     SYNCED     SYNCED (100%)     NOT SENT     istio-pilot-d99689994-2hhkw     1.0.0
istio-ingressgateway-6f58fdc8d7-c485b.istio-system     SYNCED     SYNCED     SYNCED (100%)     NOT SENT     istio-pilot-d99689994-2hhkw     1.0.0
istio-ingressgateway-6f58fdc8d7-hw6lc.istio-system     SYNCED     SYNCED     SYNCED (100%)     NOT SENT     istio-pilot-d99689994-2hhkw     1.0.0
productpage-v1-576cdb5dc8-hq9h6.book-info              SYNCED     SYNCED     SYNCED (100%)     SYNCED       istio-pilot-d99689994-2hhkw     1.0.0
ratings-v1-7c96469dc4-nt2fl.book-info                  SYNCED     SYNCED     SYNCED (100%)     SYNCED       istio-pilot-d99689994-2hhkw     1.0.0
reviews-v1-6bb564699d-pl9x4.book-info                  SYNCED     SYNCED     SYNCED (100%)     SYNCED       istio-pilot-d99689994-2hhkw     1.0.0
reviews-v2-767c798588-hqg99.book-info                  SYNCED     SYNCED     SYNCED (100%)     SYNCED       istio-pilot-d99689994-2hhkw     1.0.0
reviews-v3-786f7b97cd-bwmk5.book-info                  SYNCED     SYNCED     SYNCED (100%)     SYNCED       istio-pilot-d99689994-2hhkw     1.0.0
```

```
$> istioctl proxy-config clusters -n book-info productpage-v1-576cdb5dc8-hq9h6
SERVICE FQDN                                                      PORT      SUBSET          DIRECTION     TYPE
BlackHoleCluster                                                  -         -               -             STATIC
apicast.beer-catalog-prod.svc.cluster.local                       8080      -               outbound      EDS
apicast.beer-catalog-prod.svc.cluster.local                       8090      -               outbound      EDS
apicast.beer-catalog-test.svc.cluster.local                       8080      -               outbound      EDS
apicast.beer-catalog-test.svc.cluster.local                       8090      -               outbound      EDS
apiserver.kube-service-catalog.svc.cluster.local                  443       -               outbound      EDS
apiserver.openshift-template-service-broker.svc.cluster.local     443       -               outbound      EDS
asb-etcd.openshift-ansible-service-broker.svc.cluster.local       2379      -               outbound      EDS
asb.openshift-ansible-service-broker.svc.cluster.local            1338      -               outbound      EDS
awx-rmq-mgmt.fabric.svc.cluster.local                             15672     -               outbound      EDS
awx-web-svc.fabric.svc.cluster.local                              80        -               outbound      EDS
beer-catalog-impl.beer-catalog-dev.svc.cluster.local              8080      -               outbound      EDS
beer-catalog-impl.beer-catalog-prod.svc.cluster.local             8080      -               outbound      EDS
beer-catalog-impl.beer-catalog-test.svc.cluster.local             8080      -               outbound      EDS
broker-amq-tcp.ignite.svc.cluster.local                           61613     -               outbound      EDS
broker-amq-tcp.ignite.svc.cluster.local                           61616     -               outbound      EDS
broker-kafka-headless.strimzi.svc.cluster.local                   9091      -               outbound      ORIGINAL_DST
broker-kafka-headless.strimzi.svc.cluster.local                   9092      -               outbound      ORIGINAL_DST
broker-kafka.strimzi.svc.cluster.local                            9091      -               outbound      EDS
broker-kafka.strimzi.svc.cluster.local                            9092      -               outbound      EDS
broker-zookeeper-headless.strimzi.svc.cluster.local               2181      -               outbound      ORIGINAL_DST
broker-zookeeper-headless.strimzi.svc.cluster.local               2888      -               outbound      ORIGINAL_DST
broker-zookeeper-headless.strimzi.svc.cluster.local               3888      -               outbound      ORIGINAL_DST
broker-zookeeper.strimzi.svc.cluster.local                        2181      -               outbound      EDS
coolstore-ping.rh-forum.svc.cluster.local                         8888      -               outbound      ORIGINAL_DST
coolstore.rh-forum.svc.cluster.local                              8080      -               outbound      EDS
customer.istio-tutorial.svc.cluster.local                         8080      -               outbound      EDS
dbz-tutorial.strimzi.svc.cluster.local                            9090      -               outbound      EDS
debezium-connect.strimzi.svc.cluster.local                        8083      -               outbound      EDS
details.book-info.svc.cluster.local                               9080      -               outbound      EDS
details.book-info.svc.cluster.local                               9080      v1              outbound      EDS
details.book-info.svc.cluster.local                               9080      v2              outbound      EDS
docker-registry.default.svc.cluster.local                         5000      -               outbound      EDS
gogs-postgresql.fabric.svc.cluster.local                          5432      -               outbound      EDS
gogs.fabric.svc.cluster.local                                     3000      -               outbound      EDS
grafana.istio-system.svc.cluster.local                            3000      -               outbound      EDS
hawkular-cassandra-nodes.openshift-infra.svc.cluster.local        7000      -               outbound      ORIGINAL_DST
hawkular-cassandra-nodes.openshift-infra.svc.cluster.local        7001      -               outbound      ORIGINAL_DST
hawkular-cassandra-nodes.openshift-infra.svc.cluster.local        9042      -               outbound      ORIGINAL_DST
hawkular-cassandra-nodes.openshift-infra.svc.cluster.local        9160      -               outbound      ORIGINAL_DST
hawkular-cassandra.openshift-infra.svc.cluster.local              7000      -               outbound      EDS
hawkular-cassandra.openshift-infra.svc.cluster.local              7001      -               outbound      EDS
hawkular-cassandra.openshift-infra.svc.cluster.local              9042      -               outbound      EDS
hawkular-cassandra.openshift-infra.svc.cluster.local              9160      -               outbound      EDS
hawkular-metrics.openshift-infra.svc.cluster.local                443       -               outbound      EDS
heapster.openshift-infra.svc.cluster.local                        80        -               outbound      EDS
istio-citadel.istio-system.svc.cluster.local                      8060      -               outbound      EDS
istio-citadel.istio-system.svc.cluster.local                      9093      -               outbound      EDS
istio-egressgateway.istio-system.svc.cluster.local                80        -               outbound      EDS
istio-egressgateway.istio-system.svc.cluster.local                443       -               outbound      EDS
istio-galley.istio-system.svc.cluster.local                       443       -               outbound      EDS
istio-galley.istio-system.svc.cluster.local                       9093      -               outbound      EDS
istio-ingressgateway.istio-system.svc.cluster.local               80        -               outbound      EDS
istio-ingressgateway.istio-system.svc.cluster.local               443       -               outbound      EDS
istio-ingressgateway.istio-system.svc.cluster.local               8060      -               outbound      EDS
istio-ingressgateway.istio-system.svc.cluster.local               15011     -               outbound      EDS
istio-ingressgateway.istio-system.svc.cluster.local               15030     -               outbound      EDS
istio-ingressgateway.istio-system.svc.cluster.local               15031     -               outbound      EDS
istio-ingressgateway.istio-system.svc.cluster.local               31400     -               outbound      EDS
istio-pilot.istio-system.svc.cluster.local                        8080      -               outbound      EDS
istio-pilot.istio-system.svc.cluster.local                        9093      -               outbound      EDS
istio-pilot.istio-system.svc.cluster.local                        15010     -               outbound      EDS
istio-pilot.istio-system.svc.cluster.local                        15011     -               outbound      EDS
istio-policy.istio-system.svc.cluster.local                       9091      -               outbound      EDS
istio-policy.istio-system.svc.cluster.local                       9093      -               outbound      EDS
istio-policy.istio-system.svc.cluster.local                       15004     -               outbound      EDS
istio-sidecar-injector.istio-system.svc.cluster.local             443       -               outbound      EDS
istio-statsd-prom-bridge.istio-system.svc.cluster.local           9102      -               outbound      EDS
istio-statsd-prom-bridge.istio-system.svc.cluster.local           9125      -               outbound      EDS
istio-telemetry.istio-system.svc.cluster.local                    9091      -               outbound      EDS
istio-telemetry.istio-system.svc.cluster.local                    9093      -               outbound      EDS
istio-telemetry.istio-system.svc.cluster.local                    15004     -               outbound      EDS
istio-telemetry.istio-system.svc.cluster.local                    42422     -               outbound      EDS
jaeger-agent.istio-system.svc.cluster.local                       5775      -               outbound      ORIGINAL_DST
jaeger-agent.istio-system.svc.cluster.local                       6831      -               outbound      ORIGINAL_DST
jaeger-agent.istio-system.svc.cluster.local                       6832      -               outbound      ORIGINAL_DST
jaeger-collector.istio-system.svc.cluster.local                   14267     -               outbound      EDS
jaeger-collector.istio-system.svc.cluster.local                   14268     -               outbound      EDS
jaeger-query.istio-system.svc.cluster.local                       16686     -               outbound      EDS
jenkins-jnlp.fabric.svc.cluster.local                             50000     -               outbound      EDS
jenkins-jnlp.microcks.svc.cluster.local                           50000     -               outbound      EDS
jenkins.fabric.svc.cluster.local                                  80        -               outbound      EDS
jenkins.microcks.svc.cluster.local                                80        -               outbound      EDS
kiali-jaeger.istio-system.svc.cluster.local                       20002     -               outbound      EDS
kiali.istio-system.svc.cluster.local                              20001     -               outbound      EDS
kubernetes.default.svc.cluster.local                              53        -               outbound      EDS
kubernetes.default.svc.cluster.local                              443       -               outbound      EDS
microcks-keycloak-postgresql.microcks.svc.cluster.local           5432      -               outbound      EDS
microcks-keycloak.microcks.svc.cluster.local                      8080      -               outbound      EDS
microcks-mongodb.microcks.svc.cluster.local                       27017     -               outbound      EDS
microcks-postman-runtime.microcks.svc.cluster.local               8080      -               outbound      EDS
microcks.microcks.svc.cluster.local                               8080      -               outbound      EDS
mysql.strimzi.svc.cluster.local                                   3306      -               outbound      EDS
nexus.fabric.svc.cluster.local                                    8081      -               outbound      EDS
postgresql.fabric.svc.cluster.local                               5432      -               outbound      EDS
preference.istio-tutorial.svc.cluster.local                       8080      -               outbound      EDS
productpage.book-info.svc.cluster.local                           9080      -               inbound       STATIC
productpage.book-info.svc.cluster.local                           9080      -               outbound      EDS
productpage.book-info.svc.cluster.local                           9080      v1              outbound      EDS
prometheus.istio-system.svc.cluster.local                         9090      -               outbound      EDS
rabbitmq.fabric.svc.cluster.local                                 5672      -               outbound      EDS
rabbitmq.fabric.svc.cluster.local                                 15672     -               outbound      EDS
ratings.book-info.svc.cluster.local                               9080      -               outbound      EDS
ratings.book-info.svc.cluster.local                               9080      v1              outbound      EDS
ratings.book-info.svc.cluster.local                               9080      v2              outbound      EDS
ratings.book-info.svc.cluster.local                               9080      v2-mysql        outbound      EDS
ratings.book-info.svc.cluster.local                               9080      v2-mysql-vm     outbound      EDS
recommendation.istio-tutorial.svc.cluster.local                   8080      -               outbound      EDS
recommendation.istio-tutorial.svc.cluster.local                   8080      version-v1      outbound      EDS
recommendation.istio-tutorial.svc.cluster.local                   8080      version-v2      outbound      EDS
registry-console.default.svc.cluster.local                        9000      -               outbound      EDS
reviews.book-info.svc.cluster.local                               9080      -               outbound      EDS
reviews.book-info.svc.cluster.local                               9080      v1              outbound      EDS
reviews.book-info.svc.cluster.local                               9080      v2              outbound      EDS
reviews.book-info.svc.cluster.local                               9080      v3              outbound      EDS
router.default.svc.cluster.local                                  80        -               outbound      EDS
router.default.svc.cluster.local                                  443       -               outbound      EDS
router.default.svc.cluster.local                                  1936      -               outbound      EDS
servicegraph.istio-system.svc.cluster.local                       8088      -               outbound      EDS
sqlserver.rh-forum.svc.cluster.local                              1433      -               outbound      EDS
syndesis-db.ignite.svc.cluster.local                              5432      -               outbound      EDS
syndesis-meta.ignite.svc.cluster.local                            80        -               outbound      EDS
syndesis-oauthproxy.ignite.svc.cluster.local                      8443      -               outbound      EDS
syndesis-prometheus.ignite.svc.cluster.local                      80        -               outbound      EDS
syndesis-server.ignite.svc.cluster.local                          80        -               outbound      EDS
syndesis-ui.ignite.svc.cluster.local                              80        -               outbound      EDS
todo.ignite.svc.cluster.local                                     8080      -               outbound      EDS
tracing.istio-system.svc.cluster.local                            80        -               outbound      EDS
webconsole.openshift-web-console.svc.cluster.local                443       -               outbound      EDS
xds-grpc                                                          -         -               -             STRICT_DNS
zipkin                                                            -         -               -             STRICT_DNS
zipkin.istio-system.svc.cluster.local                             9411      -               outbound      EDS
```
