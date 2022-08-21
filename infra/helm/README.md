# Helm charts for Vyne

## Local development

To run locally, run the following commands:

```shell
cd vyne-helm
helm install vyne .
kubectl port-forward service/vyne 9022:80
```

and access the query server in http://localhost:9022.

To update the stack, run:

```shell
helm upgrade vyne .
```

```bash
kubectl cp ./schema-server/projects/ schema-server-8cbd845cd-fpjf2:/var/lib/vyne/schema-server/
kubectl cp ./schema-server/schema-server.conf schema-server-8cbd845cd-fpjf2:/var/lib/vyne/schema-server/schema-server.conf
```

## TODO

- Add pipelines
- License passing
- Add config volume to chart
- Readiness and liveness probe
- Refine labels and selectors
- Resource limits
