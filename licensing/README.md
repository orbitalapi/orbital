# Generating a key for licensing

```bash
$ openssl genrsa -out vyne-license.pem 2048

# convert private Key to PKCS#8 format (so Java can read it)
$ openssl pkcs8 -topk8 -inform PEM -outform DER -in vyne-license.pem \
-out vyne-license.der -nocrypt

# output public key portion in DER format (so Java can read it)
$ openssl rsa -in vyne-license.pem -pubout -outform DER -out vyne-license-pub.der
```

# Reading license keys:

Useful post: https://blog.jonm.dev/posts/rsa-public-key-cryptography-in-java/

Relevant section:

> That’s about it. The hard part was figuring out a compatible set of:
> openssl DER output options (particularly the PKCS#8 encoding)
> which type of KeySpec Java needed to use (strangely enough, the public key needs the “X509” keyspec, even though 
> you would normally handle X.509 certificates with the openssl x509 command, not the openssl rsa command. Real intuitive.)
>
> From here, signing and verifying work as described in the JCE documentation; the only other thing you need to know is that you 
> can use the “SHA1withRSA” algorithm when you get your java.security.Signature instance for signing/verifying, and that you want the “RSA” 
> algorithm when you get your `javax.crypto.Cipher` instance for encrypting/decrypting.

Follow-up StackOverflow entry: https://stackoverflow.com/a/17515382/59015
