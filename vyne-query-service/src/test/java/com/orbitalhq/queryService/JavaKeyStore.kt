package com.orbitalhq.queryService

import org.springframework.core.io.ClassPathResource
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import java.security.KeyStore.ProtectionParameter
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.PrivateKey
import java.security.UnrecoverableEntryException
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.xml.bind.DatatypeConverter

/**
 * This is just a test helper class to encapsulate java KeyClient class.
 * We use this class to create corresponding key and trust stores required in
 *  **[com.orbitalhq.queryService.OperationAuthenticationMtlsTest.setUpOrbitalMtls]**
 * key store contains certificates that identify Orbital
 * trust store contains certificates that identify remote services
 */
class JavaKeyStore(
   private var keyStoreType: String? = null,
   private val keyStorePassword: String,
   private val keyStorePath: String
) {

   companion object {
       fun createPrivateKey(privateKeyPem: ClassPathResource): PrivateKey {
         val r = BufferedReader(privateKeyPem.inputStream.reader())
         var s = r.readLine()
         if (s == null || !s.contains("BEGIN PRIVATE KEY")) {
            r.close()
            throw IllegalArgumentException("No PRIVATE KEY found")
         }
         val b = StringBuilder()
         s = ""
         while (s != null) {
            if (s.contains("END PRIVATE KEY")) {
               break
            }
            b.append(s)
            s = r.readLine()
         }
         r.close()
         val hexString = b.toString()
         val bytes: ByteArray = DatatypeConverter.parseBase64Binary(hexString)
         return generatePrivateKeyFromDER(bytes)
      }


       fun createCertificates(certificatePem: ClassPathResource): Array<Certificate?>? {
         val result: MutableList<X509Certificate> = ArrayList()
         val r = BufferedReader(certificatePem.inputStream.reader())
         var s = r.readLine()
         if (s == null || !s.contains("BEGIN CERTIFICATE")) {
            r.close()
            throw java.lang.IllegalArgumentException("No CERTIFICATE found")
         }
         var b = java.lang.StringBuilder()
         while (s != null) {
            if (s.contains("END CERTIFICATE")) {
               val hexString = b.toString()
               val bytes = DatatypeConverter.parseBase64Binary(hexString)
               val cert = generateCertificateFromDER(bytes)
               result.add(cert)
               b = java.lang.StringBuilder()
            } else {
               if (!s.startsWith("----")) {
                  b.append(s)
               }
            }
            s = r.readLine()
         }
         r.close()
         return result.toTypedArray<Certificate?>()
      }


       private fun generatePrivateKeyFromDER(keyBytes: ByteArray): RSAPrivateKey {
         val spec = PKCS8EncodedKeySpec(keyBytes)
         val factory: KeyFactory = KeyFactory.getInstance("RSA")
         return factory.generatePrivate(spec) as RSAPrivateKey
      }

       private fun generateCertificateFromDER(certBytes: ByteArray): X509Certificate {
         val factory: CertificateFactory = CertificateFactory.getInstance("X.509")
         return factory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate
      }

   }
   private lateinit var  keyStore: KeyStore

   @Throws(
      KeyStoreException::class,
      CertificateException::class,
      NoSuchAlgorithmException::class,
      IOException::class
   )
   fun createEmptyKeyStore() {
      if (keyStoreType.isNullOrEmpty()) {
         keyStoreType = KeyStore.getDefaultType()
         keyStore = KeyStore.getInstance(keyStoreType)
      } else {
         keyStore = KeyStore.getInstance(keyStoreType)
      }
      //load
      val pwdArray = keyStorePassword.toCharArray()
      this.keyStore.load(null, pwdArray)

      // Save the keyStore
      val fos = FileOutputStream(keyStorePath)
      keyStore.store(fos, pwdArray)
      fos.close()
   }

   @Throws(
      IOException::class,
      KeyStoreException::class,
      CertificateException::class,
      NoSuchAlgorithmException::class
   )
   fun loadKeyStore() {
      val pwdArray = keyStorePassword.toCharArray()
      val fis = FileInputStream(keyStorePath)
      keyStore.load(fis, pwdArray)
      fis.close()
   }


   fun persist() {
      val out = FileOutputStream(keyStorePath)
      out.use {
         BufferedOutputStream(it).use { bos ->
            keyStore.store(bos, keyStorePassword.toCharArray())
         }
      }
   }

   @Throws(KeyStoreException::class)
   fun setEntry(alias: String?, secretKeyEntry: KeyStore.SecretKeyEntry?, protectionParameter: ProtectionParameter?) {
      keyStore.setEntry(alias, secretKeyEntry, protectionParameter)
   }

   @Throws(UnrecoverableEntryException::class, NoSuchAlgorithmException::class, KeyStoreException::class)
   fun getEntry(alias: String?): KeyStore.Entry {
      val protParam: ProtectionParameter = PasswordProtection(keyStorePassword.toCharArray())
      return keyStore.getEntry(alias, protParam)
   }

   @Throws(KeyStoreException::class)
   fun setKeyEntry(
      alias: String?,
      privateKey: PrivateKey?,
      keyPassword: String,
      certificateChain: Array<Certificate?>?
   ) {
      keyStore.setKeyEntry(alias, privateKey, keyPassword.toCharArray(), certificateChain)
   }

   @Throws(KeyStoreException::class)
   fun setCertificateEntry(alias: String?, certificate: Certificate?) {
      keyStore.setCertificateEntry(alias, certificate)
   }

   @Throws(KeyStoreException::class)
   fun getCertificate(alias: String?): Certificate {
      return keyStore.getCertificate(alias)
   }

   @Throws(KeyStoreException::class)
   fun deleteEntry(alias: String?) {
      keyStore.deleteEntry(alias)
   }

   @Throws(KeyStoreException::class, IOException::class)
   fun deleteKeyStore() {
      val aliases = keyStore.aliases()
      while (aliases.hasMoreElements()) {
         val alias = aliases.nextElement()
         keyStore.deleteEntry(alias)
      }

      val keyStoreFile = Paths.get(keyStorePath)
      Files.delete(keyStoreFile)
   }
}
