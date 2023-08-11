package io.vyne.cockpit.core.security

import io.vyne.auth.authentication.UserOrbitalId
import io.vyne.auth.authentication.VyneUser
import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

// DB persistence for VyneUser.
// Likely the successor to file based persistence implemented through VyneUserRepository
interface VyneUserJpaRepository : JpaRepository<VyneUser,UserOrbitalId>{

   @Transactional
   @Modifying
   @Query(
      value = """
            INSERT INTO USERS(id, issuer, username, email, profile_url, name)
            VALUES (:id, :issuer, :username, :email, :profileUrl, :name)
            ON CONFLICT (id)
            DO UPDATE SET
                issuer = :issuer,
                username = :username,
                email = :email,
                profile_url = :profileUrl,
                name = :name
        """,
      nativeQuery = true
   )
   fun upsert(
      id: String,
      issuer: String,
      username: String,
      email: String,
      profileUrl: String?,
      name: String?
   ): Int

}
