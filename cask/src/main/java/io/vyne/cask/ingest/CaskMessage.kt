package io.vyne.cask.ingest

import io.vyne.cask.api.ContentType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import javax.persistence.*

@Entity
data class CaskMessage(
   @Id
   val id: String,
   @Column(name = "qualifiedtypename")
   val qualifiedTypeName: String,
   @Column(name = "messageid")
   val messageContentId: Long?,
   @Column(name = "insertedat")
   val insertedAt: Instant,
   @Enumerated(EnumType.STRING)
   // Nullable, as we're migrating existing data and there's
   // no reasonable default
   @Column(name = "contenttype")
   val messageContentType: ContentType?,
   @Column(name = "ingestionparams")
   val ingestionParams: String?

)

interface CaskMessageRepository : JpaRepository<CaskMessage,String>
