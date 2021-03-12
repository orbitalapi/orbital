---
title: Pipelines Orchestrator API
description: >-
  Pipelines Orchestrator API allows for an easy way of scheduling your pipelines
  and manage your runners.
---

A Swagger UI is available at `http://pipelies-orchestrator:9600/swagger-ui.html`

<!--{% api-method method="get" host="http://pipelines-orchestrator:9600" path="/api/pipelines" %}
{% api-method-summary %}
Get Pipelines
{% endapi-method-summary %}

{% api-method-description %}
Retrieve all the scheduled/running pipelines
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
The list of current Pipeline states
{% endapi-method-response-example-description %}

```
[
  {
    "info": "string",
    "instance": {
      "instanceId": "string",
      "uri": "string"
    },
    "name": "string",
    "pipelineDescription": "string",
    "state": "SCHEDULED"
  }
]
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

{% api-method method="post" host="http://pipelines-orchestrator:9600" path="/api/pipelines" %}
{% api-method-summary %}
Submit a pipeline
{% endapi-method-summary %}

{% api-method-description %}
Submit a pipeline to the Orchestrator
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}
{% api-method-body-parameters %}
{% api-method-parameter name="pipelineDescription" type="string" required=true %}
The pipeline description
{% endapi-method-parameter %}
{% endapi-method-body-parameters %}
{% endapi-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
The scheduled pipeline state
{% endapi-method-response-example-description %}

```
{
  "info": "string",
  "instance": {
    "instanceId": "string",
    "uri": "string"
  },
  "name": "string",
  "pipelineDescription": "string",
  "state": "SCHEDULED"
}
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}

###

{% api-method method="get" host="http://pipelines-orchestrator:9600" path="/api/runners" %}
{% api-method-summary %}
Get the runners
{% endapi-method-summary %}

{% api-method-description %}
Get the runner Pipeline Runner instances
{% endapi-method-description %}

{% api-method-spec %}
{% api-method-request %}

{% api-method-response %}
{% api-method-response-example httpCode=200 %}
{% api-method-response-example-description %}
The running Pipeline Runners
{% endapi-method-response-example-description %}

```
[
  {
    "instanceId": "string",
    "uri": "string"
  }
]
```
{% endapi-method-response-example %}
{% endapi-method-response %}
{% endapi-method-spec %}
{% endapi-method %}
-->
