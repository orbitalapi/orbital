package com.orbitalhq.query.planner

import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldStartWith

class QueryPlannerTest : DescribeSpec({
   describe("rewriting streaming queries") {
      val queryPlanner = QueryPlanner()
      val schema = TaxiSchema.from(
         """
            model Tweet {
               tweetId : TweetId inherits String
               message : TweetMessage inherits String
            }
            model TweetAnalytics {
               tweetId : TweetId inherits String
               viewCount : ViewCount inherits Int
            }
            type UserCountry inherits String
            service Tweets {
               stream tweets:Stream<Tweet>
               stream analytics: Stream<TweetAnalytics>
            }
         """.trimIndent()
      )
      it("should append missing streaming sources if data is only available there") {
         val (query, _, querySchema) = schema.parseQuery(
            """stream { Tweet } as {
            | tweetId : TweetId
            | viewCount : ViewCount // Comes from TweetAnalytics
            |}[]
         """.trimMargin()
         )
         val (_, rewrittenQuery) = queryPlanner.buildQueryExpression(query, querySchema)
         rewrittenQuery.source.shouldStartWith("""stream { Tweet | TweetAnalytics }""")
      }

      // ORB-103
      it("should not modify streaming sources for data accessible in provided stream source") {
         val (query, _, querySchema) = schema.parseQuery(
            """stream { Tweet } as {
            | tweetId : TweetId // Available in both Tweet and TweetAnalytics, but the query should not be rewritten
            | message : TweetMessage
            |}[]
         """.trimMargin()
         )
         val (_, rewrittenQuery) = queryPlanner.buildQueryExpression(query, querySchema)
         rewrittenQuery.source.shouldStartWith("""stream { Tweet }""")
      }

      it("should not modify streaming sources where the query already specifies existing sources") {
         val (query, _, querySchema) = schema.parseQuery(
            """stream { Tweet | TweetAnalytics } as {
            | tweetId : TweetId // Available in both Tweet and TweetAnalytics, but the query should not be rewritten
            | viewCount : ViewCount
            |}[]
         """.trimMargin()
         )
         val (_, rewrittenQuery) = queryPlanner.buildQueryExpression(query, querySchema)
         rewrittenQuery.source.shouldStartWith("""stream { Tweet | TweetAnalytics }""")
      }

      it("should not modify streaming data sources for data that is not available in any data source") {
         val (query, _, querySchema) = schema.parseQuery(
            """stream { Tweet } as {
            | tweetId : TweetId
            | country : UserCountry // not available anywhere
            |}[]
         """.trimMargin()
         )
         val (_, rewrittenQuery) = queryPlanner.buildQueryExpression(query, querySchema)
         rewrittenQuery.source.shouldStartWith("""stream { Tweet }""")
      }
   }
}) {
}
