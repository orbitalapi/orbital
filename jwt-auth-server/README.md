# JWT Authorisation Server

This project contains an spring boot wrapper application for a keyclock server, the admin UI can be reached at:

```
http://localhost:8080/auth/admin
```

see application.yml for admin user name and password.

When the application run with its default settings:

  * It instantiates a KeyCloack server according to the settings under META-INF/keycloack-server.json
    * H2 is defined as the KeyCloack Database, so modifications done through Administration Console not persisted across restarts.
   * A new Realm Called 'Vyne' is created by settings defined in /resources/vyne-realm.json
        * A new client called "vyne-spa" is created in Vyne realm
  * To see Vyne Realm settings, goto localhost:8080/auth and click on "Administration Console"
  * Login details for Administration Console can be found in application.yml (keycloack.server.adminUser.username and keycloack.server.adminUser.password)
  * As part of the start up sequence application creates two users, user1 and user2 in 'Vyne' Realm, see application.yml for the definitions of these users.
