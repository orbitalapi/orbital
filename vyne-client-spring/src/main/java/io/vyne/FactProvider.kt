package io.vyne

import io.vyne.query.Fact

interface FactProvider {
   // TODO : We should allow factProviders to publish their own schemas, and this seems like a natural place to do it.

   fun provideFacts(currentFacts: List<Fact>): List<Fact>
}
