package org.taxilang.playground

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate
import java.time.OffsetDateTime

@RestController
class EmailSubscriptionController(
   val restTemplate: RestTemplate,
   @Value("\${hubspot.api.url}") val hubspotApiUrl: String,
   @Value("\${hubspot.api.auth-token}") val hubspotAuthToken: String,
   @Value("\${hubspot.subscriptions.voyager-updates-id}") val voyagerSubscriptionId: String,
   @Value("\${hubspot.subscriptions.general-marketing}") val generalMarketingSubscriptionId: String
) {

   private val hubspotHeaders: HttpHeaders = HttpHeaders()
   val logger: Logger = LoggerFactory.getLogger(EmailSubscriptionController::class.java)

   init {
      hubspotHeaders.setBearerAuth(hubspotAuthToken)
      hubspotHeaders.contentType = MediaType.APPLICATION_JSON
   }

   @PostMapping("/api/subscribe")
   fun subscribeFan(@RequestBody subscribeDetails: SubscribeDetails): ResponseEntity<EmptyResponse> {
      createContact(subscribeDetails.email)

      val voyagerSubscriptionResponse = requestSubscriptionForContact(voyagerSubscriptionId, subscribeDetails)
      if (voyagerSubscriptionResponse.statusCode == HttpStatus.OK) {
         logger.info("Contact has been subscribed to the voyager mailing list")
      } else {
         logger.error("Failed to subscribe contact to the voyager mailing list")
      }

      var generalMarketingSubscriptionResponse: ResponseEntity<HubspotSubscriptionResponse>? = null
      if (subscribeDetails.otherCommsConsent) {
         generalMarketingSubscriptionResponse = requestSubscriptionForContact(generalMarketingSubscriptionId, subscribeDetails)
         if (generalMarketingSubscriptionResponse.statusCode == HttpStatus.OK) {
            logger.info("Contact has been subscribed to the general marketing mailing list")
         } else {
            logger.error("Failed to subscribe contact to the general marketing list")
         }
      }

      return if (voyagerSubscriptionResponse.statusCode == HttpStatus.OK && (generalMarketingSubscriptionResponse == null || (generalMarketingSubscriptionResponse.statusCode == HttpStatus.OK))) {
         ResponseEntity.ok(EmptyResponse())
      } else {
         ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(EmptyResponse())
      }
   }

   private fun createContact(email: String) {
      val createContactUrl = "$hubspotApiUrl/crm/v3/objects/contacts"

      val response = this.restTemplate.exchange(
         createContactUrl,
         HttpMethod.POST,
         HttpEntity(HubspotCreateContactData(HubspotCreateContactProperties(email)), hubspotHeaders),
         HubspotCreateContactResponse::class.java
      )

      when (response.statusCode) {
          HttpStatus.CREATED -> {
             logger.info("New contact successfully created")
          }
          HttpStatus.CONFLICT -> {
             logger.info("Contact already exists in hubspot")
          }
          else -> {
             logger.error("Error occurred creating contact in Hubspot for email $email")
          }
      }
   }

   private fun requestSubscriptionForContact(subscriptionId: String, subscribeDetails: SubscribeDetails): ResponseEntity<HubspotSubscriptionResponse> {
      val subscriptionUrl = "$hubspotApiUrl/communication-preferences/v3/subscribe"

      return this.restTemplate.exchange(
         subscriptionUrl,
         HttpMethod.POST,
         HttpEntity(HubspotSubscriptionData(subscribeDetails.email, subscriptionId, "CONSENT_WITH_NOTICE"), hubspotHeaders),
         HubspotSubscriptionResponse::class.java)
   }
}

data class SubscribeDetails(var email: String, var otherCommsConsent: Boolean)

/*
{
  "company": "Biglytics",
  "email": "bcooper@biglytics.net",
  "firstname": "Bryan",
  "lastname": "Cooper",
  "phone": "(877) 929-0687",
  "website": "biglytics.net"
}
 */
data class HubspotCreateContactData(
   val properties: HubspotCreateContactProperties
)

data class HubspotCreateContactProperties(
   val email: String
)

/*
{
  "id": "512",
  "properties": {
    "company": "Biglytics",
    "createdate": "2019-10-30T03:30:17.883Z",
    "email": "bcooper@biglytics.net",
    "firstname": "Bryan",
    "lastmodifieddate": "2019-12-07T16:50:06.678Z",
    "lastname": "Cooper",
    "phone": "(877) 929-0687",
    "website": "biglytics.net"
  },
  "createdAt": "2019-10-30T03:30:17.883Z",
  "updatedAt": "2019-12-07T16:50:06.678Z",
  "archived": false
}
 */
data class HubspotCreateContactResponse(
   val id: String,
   val properties: HubspotContact,
   val createdAt: OffsetDateTime,
   val updatedAt: OffsetDateTime,
   val archived: Boolean
)

data class HubspotContact(
   val company: String?,
   val createdate: OffsetDateTime?,
   val email: String?,
   val firstname: String?,
   val lastmodifieddate: OffsetDateTime?,
   val lastname: String?,
   val phone: String?,
   val website: String?
)

data class HubspotSubscriptionData(
   val emailAddress: String,
   val subscriptionId: String,
   val legalBasis: String?,
   val legalBasisExplanation: String? = null
)

data class HubspotSubscriptionResponse(
   val id: String,
   val name: String?,
   val status: String?,
   val sourceOfStatus: String?,
   val brandId: Int?,
   val preferenceGroupName: String?,
   val legalBasis: String?,
   val legalBasisExplanation: String?
)

class EmptyResponse
