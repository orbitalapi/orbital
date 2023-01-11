package org.taxilang.playground

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.OffsetDateTime

enum class SubscriptionType {
   VOYAGER_ONLY,
   GENERAL_MARKETING
}

@RestController
class EmailSubscriptionController(
   val restTemplate: RestTemplate,
   @Value("\${hubspot.api.url}") val hubspotApiUrl: String,
   @Value("\${hubspot.api.auth-token}") val hubspotAuthToken: String,
   @Value("\${hubspot.subscriptions.voyager-updates-id}") val voyagerSubscriptionId: String,
   @Value("\${hubspot.subscriptions.general-marketing}") val generalMarketingSubscriptionId: String
) {

   private val subscriptionIds = mapOf(
      SubscriptionType.VOYAGER_ONLY to voyagerSubscriptionId,
      SubscriptionType.GENERAL_MARKETING to generalMarketingSubscriptionId
   )

   private val hubspotHeaders: HttpHeaders = HttpHeaders()
   val logger: Logger = LoggerFactory.getLogger(EmailSubscriptionController::class.java)

   init {
      hubspotHeaders.setBearerAuth(hubspotAuthToken)
      hubspotHeaders.contentType = MediaType.APPLICATION_JSON
   }

   @PostMapping("/api/subscribe")
   fun subscribeFan(@RequestBody subscribeDetails: SubscribeDetails): ResponseEntity<UiSubscriptionResponse> {
      createContact(subscribeDetails.email)

      val subscriptionsRequested = if (subscribeDetails.otherCommsConsent) {
         listOf(SubscriptionType.VOYAGER_ONLY, SubscriptionType.GENERAL_MARKETING)
      } else {
         listOf(SubscriptionType.VOYAGER_ONLY)
      }

      val results = subscriptionsRequested
         .map { subscriptionType ->
            requestSubscriptionForContact(subscriptionIds[subscriptionType]!!, subscribeDetails).result
         }

      return if (results.any { it == SubscriptionResult.FAILED } ) {
         logger.error("An error occured for one or more subscriptions")
         ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(UiSubscriptionResponse(SubscriptionResult.FAILED))
      } else if (results.all { it == SubscriptionResult.ALREADY_SUBSCRIBED }) {
         logger.info("Contact was already subscribed to requested subscriptions")
         ResponseEntity.ok(UiSubscriptionResponse(SubscriptionResult.ALREADY_SUBSCRIBED))
      } else if (results.all { it == SubscriptionResult.SUCCESS || it == SubscriptionResult.ALREADY_SUBSCRIBED} ) {
         logger.info("All requested subscriptions successfully processed")
         // all success or mix of already subscribed, in case they resubscribed with wider options
         ResponseEntity.ok(UiSubscriptionResponse(SubscriptionResult.SUCCESS))
      } else {
         logger.error("Unexpected combination of subscription results")
         ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(UiSubscriptionResponse(SubscriptionResult.FAILED))
      }
   }

   private fun createContact(email: String) {
      val createContactUrl = "$hubspotApiUrl/crm/v3/objects/contacts"

      try {
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
            else -> {
               logger.error("New contact call succeeded but with unexpected status code ${response.statusCode}")
            }
         }
      } catch (e: HttpClientErrorException) {
         when (e.statusCode) {
            HttpStatus.CONFLICT -> {
               logger.info("Contact already exists in hubspot, proceed with subscription")
            }
            else -> {
               logger.info("Hubspot create contract call failed: ${e.message}")
               throw e
            }
         }
      }
   }

   private fun requestSubscriptionForContact(
      subscriptionId: String,
      subscribeDetails: SubscribeDetails
   ): SubscriptionResponse {
      val subscriptionUrl = "$hubspotApiUrl/communication-preferences/v3/subscribe"


      return try {
         val response = this.restTemplate.exchange(
            subscriptionUrl,
            HttpMethod.POST,
            HttpEntity(
               HubspotSubscriptionData(subscribeDetails.email, subscriptionId, "CONSENT_WITH_NOTICE"),
               hubspotHeaders
            ),
            HubspotSubscriptionResponse::class.java
         ).body

         logger.info("Contact was subscribed to subscription $subscriptionId")

         SubscriptionResponse(
            SubscriptionResult.SUCCESS,
            response
         )
      } catch (e: HttpClientErrorException) {
         if (e.statusCode == HttpStatus.BAD_REQUEST && e.message?.contains("already subscribed to subscription") == true) {
            logger.info("Contact was already subscribed to subscription $subscriptionId")
            SubscriptionResponse(
               SubscriptionResult.ALREADY_SUBSCRIBED
            )
         } else {
            logger.error("Subscription for contact failed for $subscriptionId. Error: ${e.message}")
            SubscriptionResponse(
               SubscriptionResult.FAILED
            )
         }
      }
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

data class SubscriptionResponse(
   val result: SubscriptionResult,
   val hubspotData: HubspotSubscriptionResponse? = null
)

enum class SubscriptionResult {
   SUCCESS,
   ALREADY_SUBSCRIBED,
   FAILED
}

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

class UiSubscriptionResponse(val result: SubscriptionResult)
