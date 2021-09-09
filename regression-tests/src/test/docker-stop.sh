#!/bin/bash
docker rm -vf $(docker ps | grep "vyneco" | awk '{ print $1 }') 2>/dev/null || echo "No more vyne containers to remove."
docker rm -vf $(docker ps | grep "testcontainers" | awk '{ print $1 }') 2>/dev/null || echo "No more test containers to remove."
docker rm -vf $(docker ps | grep "kafka" | awk '{ print $1 }') 2>/dev/null || echo "No more kafka containers to remove."
docker rm -vf $(docker ps | grep "prometheus" | awk '{ print $1 }') 2>/dev/null || echo "No more prometheus containers to remove."
docker rm -vf $(docker ps | grep "grafana" | awk '{ print $1 }') 2>/dev/null || echo "No more grafana containers to remove."
