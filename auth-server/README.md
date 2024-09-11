# Auth Server

This is a standalone auth server (Keycloak) for locally testing Vyne with authentication.

It's not to be used for production.

## Getting started

Just run:

```bash
docker-compose up -d
```

The admin console is then avaiable on `http://localhost:8080/admin`

Log in with username/password `admin/admin`

## Logging in to Vyne

To run vyne in authenticated mode, you need to add the following params to your startup:

```
--vyne.security.openIdp.jwk-set-uri=http://localhost:8080/realms/Vyne/protocol/openid-connect/certs
--vyne.security.openIdp.enabled=true
--vyne.security.openIdp.issuerUrl=http://localhost:8080/realms/Vyne
--vyne.security.openIdp.clientId=vyne-spa
--vyne.security.openIdp.scope=openid
--vyne.security.openIdp.require-https=false
```

Currently, the following user/passwords are configured:

* `marty / password`

## Exporting users

When the application starts, the realms defined in `realms/` directory are automatically imported.

This includes the above users.

To add a new user (which will be automatically imported on startup), first log into the admin panel,
and create your user through the UI. Be sure to set the password in the Credentials tab too.

Then, do the following:

* Ensure the docker image is running using the above
* `docker exec -it {containedId} /bin/bash`
* `cd /opt/keycloak/bin`
* `./kc.sh export --dir /opt/keycloak/data/import --users realm_file`

This will export the data into `Vyne-realm.json`. Feel free to check this file in.

## Styling Keycloak

We're using [keycloakify-starter](https://github.com/keycloakify/keycloakify-starter) to style the **login page**.

The actual configuration for the theme is done in the KcPage.tsx and the ejected Template.tsx files (along with a main.css file).

To generate a new theme .jar file, run `npx keycloakify start-keycloak`

For further help, see the [documentation on the keycloakify site](https://docs.keycloakify.dev/).
