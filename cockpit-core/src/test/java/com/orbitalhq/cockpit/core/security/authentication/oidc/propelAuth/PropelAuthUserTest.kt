package com.orbitalhq.cockpit.core.security.authentication.oidc.propelAuth

import com.fasterxml.jackson.module.kotlin.readValue
import com.orbitalhq.cockpit.core.security.authentication.oidc.PropelAuthApiKeyValidator
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PropelAuthUserTest {
    @Test
    fun `can deserialize`() {
val json = """{
  "metadata": null,
  "user": {
    "user_id": "3d63355d-70dd-4cea-aa1f-7ed63a312385",
    "email": "marty.pitt@gmail.com",
    "email_confirmed": true,
    "has_password": true,
    "first_name": "Marty",
    "last_name": "Pitt",
    "picture_url": "https://img.propelauth.com/2a27d237-db8c-4f82-84fb-5824dfaedc87.png",
    "properties": {},
    "metadata": null,
    "locked": false,
    "enabled": true,
    "mfa_enabled": false,
    "can_create_orgs": false,
    "created_at": 1723278254,
    "last_active_at": 1740998990,
    "org_id_to_org_info": {
      "abbc85e9-068b-426c-8da8-56f410f82ef8": {
        "org_id": "abbc85e9-068b-426c-8da8-56f410f82ef8",
        "org_name": "test",
        "org_metadata": {},
        "url_safe_org_name": "test",
        "user_role": "Admin",
        "inherited_user_roles_plus_current_role": [
          "Admin"
        ],
        "org_role_structure": "multi_role",
        "additional_roles": [
          "License Admin"
        ],
        "user_permissions": [
          "propelauth::can_invite",
          "propelauth::can_change_roles",
          "propelauth::can_remove_users",
          "propelauth::can_setup_saml",
          "propelauth::can_manage_api_keys",
          "propelauth::can_view_other_members",
          "propelauth::can_delete_org",
          "manage-customer-licenses"
        ]
      }
    },
    "update_password_required": false
  },
  "org": null,
  "user_in_org": null,
  "user_id": "3d63355d-70dd-4cea-aa1f-7ed63a312385",
  "org_id": null
}"""

       val response = PropelAuthApiKeyValidator.objectMapper()
          .readValue<PropelAuthApiKeyValidationResponse>(json)
       response.shouldNotBeNull()
//       response.user!!.email.shouldBe("marty.pitt@gmail.com")
//       response.user!!.emailConfirmed.shouldBeTrue()
    }
 }
