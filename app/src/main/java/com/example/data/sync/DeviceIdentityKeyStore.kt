package com.example.data.sync

import android.os.Build
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

object DeviceIdentityKeyStore {
  private const val KEY_ALIAS = "tribe_device_identity_signing_v1"

  @Synchronized
  fun getOrCreateKeyPair(): KeyPair {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    if (keyStore.containsAlias(KEY_ALIAS)) {
      val privateKey = keyStore.getKey(KEY_ALIAS, null) as PrivateKey
      val publicKey = keyStore.getCertificate(KEY_ALIAS).publicKey
      return KeyPair(publicKey, privateKey)
    }

    val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
    generator.initialize(
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
      )
        .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
        .setDigests(KeyProperties.DIGEST_SHA256)
        .build()
    )
    return generator.generateKeyPair()
  }

  fun publicKeyBase64(): String =
    Base64.encodeToString(getOrCreateKeyPair().public.encoded, Base64.NO_WRAP)

  fun sign(payload: ByteArray): ByteArray {
    val signature = Signature.getInstance("SHA256withECDSA")
    signature.initSign(getOrCreateKeyPair().private)
    signature.update(payload)
    return signature.sign()
  }

  fun deviceLabel(): String =
    (Build.MANUFACTURER + " " + Build.MODEL).trim().take(80).ifBlank { "Android device" }
}
