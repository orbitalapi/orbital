# Deploying a query node as a serverless function

## Login to ECS

Requires that there's an `orbital` profile defined in `~/.aws/credentials`:

```
[orbital]
aws_access_key_id=xxx
aws_secret_access_key=xxxx
```

Then log into ECR.  Customer specific examples are in internal wiki


## Build native image

The below assumes we're pushing to the Orbital ECS store.
If using a customer specific private ECS store, update accordingly

* Generally, perform a `mvn clean install` (or `mvnd ...`) on the full project before doing this.


#### MOVED
Customer specific details have been moved into the internal wiki.
