package io.vyne.licensing.vendor

import io.vyne.licensing.License
import io.vyne.licensing.Signing
import java.nio.file.Path
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64


class LicenseVendor(
   private val privateKey: PrivateKey
) {
   companion object {
      fun forPrivateKeyAtPath(path: Path): LicenseVendor {
         val bytes = path.toFile().readBytes()
         return forPrivateKey(bytes)
      }
      fun forPrivateKey(bytes:ByteArray): LicenseVendor {
         val privateKey = Signing.keyFactory
            .generatePrivate(PKCS8EncodedKeySpec(bytes))
         return LicenseVendor(privateKey)
      }
   }

   fun generateSignedLicense(license: License): License {
      require(license.signature == null) { "This license has already been signed" }
      val claimToVerify = license.verificationClaim()
      val signer = Signing.signer
      signer.initSign(privateKey)
      signer.update(claimToVerify)
      val signatureBytes = signer.sign()
      val signatureText = Base64.getEncoder().encodeToString(signatureBytes)
      return license.signed(signatureText)
   }
}
