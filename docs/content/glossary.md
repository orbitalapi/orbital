---
title: Glossary
description: Vyne Glossary
---

<table>
  <thead>
    <tr>
      <th style="text-align:left">Term</th>
      <th style="text-align:left">Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style="text-align:left"><b>Taxi Type</b>
      </td>
      <td style="text-align:left">Core Vyne Entity, (e.g. Customer, Order, Trade, etc.)
        <br />Refer to <a href="https://docs.taxilang.org/taxi-language">Taxi </a>for
        more information.</td>
    </tr>
    <tr>
      <td style="text-align:left">
        <p><b>Taxi File</b>
        </p>
        <p><b>Taxi Schema </b>
        </p>
      </td>
      <td style="text-align:left">Single or multiple files defining types and models used in your company
        (e.g. Orders, Trades, etc.)
        <br />Refer to <a href="https://docs.taxilang.org/taxi-language">Taxi </a>for
        more information.</td>
    </tr>
    <tr>
      <td style="text-align:left"><b>Vyne, <br />Query_Server</b> 
      </td>
      <td style="text-align:left">
        <p>Heart of the system.</p>
        <p>Provides data query API. e.g. <b>findAll { demo.Broker1Order[] } as demo.CommonOrder</b>
        </p>
        <p>If data for requested type is reachable it will automatically perform
          all the requests, aggregation and projection.</p>
      </td>
    </tr>
    <tr>
      <td style="text-align:left"><b>Schema_Server</b>
      </td>
      <td style="text-align:left">Maintains copy of Taxi files and distributes any changes to all the Vyne
        components.</td>
    </tr>
    <tr>
      <td style="text-align:left"><b>Cask</b>
      </td>
      <td style="text-align:left">Read Cache. Exposes API for data ingestion and automatically exposes it
        as as service via Vyne.</td>
    </tr>
    <tr>
      <td style="text-align:left"><b>Pipeline</b>
      </td>
      <td style="text-align:left">Ingestion pipeline - it automatically reads your data (e.g. from Kafka
        topic) and pushes it to Cask for ingestion.</td>
    </tr>
    <tr>
      <td style="text-align:left"><b>Pipeline Definition</b>
      </td>
      <td style="text-align:left">Description of the pipeline (e.g. source definition, topic names, target
        type, cask instance, etc.)</td>
    </tr>
    <tr>
      <td style="text-align:left"><b>Pipeline Orchestrator</b>
      </td>
      <td style="text-align:left">Service holding all the pipeline definitions. Exposes admin UI for managing
        pipelines and their state.</td>
    </tr>
    <tr>
      <td style="text-align:left"><b>Eureka</b>
      </td>
      <td style="text-align:left">Service discovery. Refer <a href="https://www.tutorialspoint.com/spring_boot/spring_boot_eureka_server.htm">here </a>for
        more info.</td>
    </tr>
    <tr>
      <td style="text-align:left"><b>Hazelcast</b>
      </td>
      <td style="text-align:left">In memory data grid <a href="https://hazelcast.org/">https://hazelcast.org/</a> 
        <br
        />Used by Vyne to replicate Taxi Schema among all the components.</td>
    </tr>
  </tbody>
</table>
