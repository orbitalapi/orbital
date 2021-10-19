# Vyne Graph debugger

## Start this:
```
npm i
ng serve
```

## Grab the query graph.  

Update the log levels in QueryServer `application.yml`, setting the log settings for `io.vyne.query.GraphSearcher` to `TRACE`, and run a query.

You should get an output like:
```text
===================Query graph:========================
{
  "nodes" : { ...snip... }
}
========================================================
```

Paste into the text box, and the graph will render.
