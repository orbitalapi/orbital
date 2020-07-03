# Search locations
The search locations can contain placeholders for {application}, {profile}, and {label}. 
In this way, you can segregate the directories in the path and choose a strategy that makes sense 
for you (such as subdirectory per application or subdirectory per profile).
Reference: https://cloud.spring.io/spring-cloud-config/multi/multi__spring_cloud_config_server.html

## How to access configuration
```http://localhost:8888/<application>/<profile>/<label>```

### Examples: 
* http://localhost:8888/cask/default -> config/cask.yml
* http://localhost:8888/cask/local -> config/cask-local.yml
* http://localhost:8888/cask/local/master -> config/cask-local.yml (for git implementation this may refer to specific tag)
...
