package com.example.data.transfer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.os.ParcelUuid
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class NearbyTransferPeer(val device: BluetoothDevice) {
  val address: String get() = device.address
  val displayName: String get() = address
}

data class ReceivedTransfer(
  val fileName: String,
  val mimeType: String,
  val encryptedFile: File,
  val sizeBytes: Long
)

class BluetoothTransferManager(private val context: Context) {
  companion object {
    val SERVICE_UUID: UUID = UUID.fromString("a9b4d4e2-8e6f-4c52-9d7e-5b7a4c1f0a11")
    private const val SERVICE_NAME = "TRIBE Secure Share"
    private const val CHUNK_SIZE = 48 * 1024
    private const val MAX_TRANSFER_BYTES = 20L * 1024L * 1024L
    private const val TAG = "TribeBluetooth"
  }

  private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
  private var scanner: BluetoothLeScanner? = null
  private var advertiser: BluetoothLeAdvertiser? = null
  private var scanCallback: ScanCallback? = null
  private var advertiseCallback: AdvertiseCallback? = null
  @Volatile private var serverSocket: BluetoothServerSocket? = null
  @Volatile private var activeSocket: BluetoothSocket? = null
  private val scanning = AtomicBoolean(false)

  fun isAvailable(): Boolean = adapter != null

  fun isEnabled(): Boolean = try {
    hasConnectPermission() && adapter?.isEnabled == true
  } catch (e: SecurityException) {
    Log.w(TAG, "Bluetooth state permission unavailable", e)
    false
  }

  private fun hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

  private fun hasConnectPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || hasPermission(Manifest.permission.BLUETOOTH_CONNECT)

  private fun hasScanPermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      hasPermission(Manifest.permission.BLUETOOTH_SCAN)
    } else {
      hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

  private fun hasAdvertisePermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S || hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)

  fun startAdvertising(): Boolean {
    if (!hasConnectPermission() || !hasAdvertisePermission()) return false
    val bluetoothAdapter = adapter ?: return false
    if (!bluetoothAdapter.isEnabled) return false
    val bleAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser ?: return false
    stopAdvertising()
    val callback = object : AdvertiseCallback() {
      override fun onStartFailure(errorCode: Int) {
        Log.w(TAG, "BLE advertising failed: " + errorCode)
      }
    }
    val settings = AdvertiseSettings.Builder()
      .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
      .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
      .setConnectable(true)
      .build()
    val data = AdvertiseData.Builder()
      .addServiceUuid(ParcelUuid(SERVICE_UUID))
      .setIncludeDeviceName(false)
      .build()
    return try {
      advertiser = bleAdvertiser
      advertiseCallback = callback
      bleAdvertiser.startAdvertising(settings, data, callback)
      true
    } catch (e: SecurityException) {
      Log.w(TAG, "Missing Bluetooth advertise permission", e)
      false
    }
  }

  fun stopAdvertising() {
    val currentAdvertiser = advertiser
    val currentCallback = advertiseCallback
    if (currentAdvertiser != null && currentCallback != null) {
      if (hasAdvertisePermission()) {
        try {
          currentAdvertiser.stopAdvertising(currentCallback)
        } catch (e: SecurityException) {
          Log.w(TAG, "Bluetooth advertise permission was revoked", e)
        }
      }
    }
    advertiser = null
    advertiseCallback = null
  }

  fun startScan(onPeerFound: (NearbyTransferPeer) -> Unit): Boolean {
    if (!hasConnectPermission() || !hasScanPermission()) return false
    val bluetoothAdapter = adapter ?: return false
    if (!bluetoothAdapter.isEnabled) return false
    val bleScanner = bluetoothAdapter.bluetoothLeScanner ?: return false
    stopScan()
    val callback = object : ScanCallback() {
      override fun onScanResult(callbackType: Int, result: ScanResult) {
        onPeerFound(NearbyTransferPeer(result.device))
      }
      override fun onBatchScanResults(results: MutableList<ScanResult>) {
        results.forEach { onPeerFound(NearbyTransferPeer(it.device)) }
      }
      override fun onScanFailed(errorCode: Int) {
        Log.w(TAG, "BLE scan failed: " + errorCode)
      }
    }
    return try {
      scanner = bleScanner
      scanCallback = callback
      scanning.set(true)
      bleScanner.startScan(
        listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()),
        ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(),
        callback
      )
      true
    } catch (e: SecurityException) {
      scanning.set(false)
      Log.w(TAG, "Missing Bluetooth scan permission", e)
      false
    }
  }

  fun stopScan() {
    val currentScanner = scanner
    val currentCallback = scanCallback
    if (currentScanner != null && currentCallback != null && scanning.getAndSet(false)) {
      if (hasScanPermission()) {
        try {
          currentScanner.stopScan(currentCallback)
        } catch (e: SecurityException) {
          Log.w(TAG, "Bluetooth scan permission was revoked", e)
        }
      }
    }
    scanner = null
    scanCallback = null
  }

  suspend fun sendFile(
    device: BluetoothDevice,
    fileName: String,
    bytes: ByteArray,
    mimeType: String = "application/octet-stream",
    onProgress: (Long, Long) -> Unit = { _, _ -> }
  ): Result<Unit> = withContext(Dispatchers.IO) {
    if (bytes.isEmpty() || bytes.size.toLong() > MAX_TRANSFER_BYTES) {
      return@withContext Result.failure(IllegalArgumentException("Transfer file must be 1 byte to 20 MB"))
    }
    if (!hasConnectPermission()) return@withContext Result.failure(SecurityException("Bluetooth connect permission is required"))
    if (adapter?.isEnabled != true) return@withContext Result.failure(IOException("Bluetooth is turned off"))
    val isBonded = try {
      device.bondState == BluetoothDevice.BOND_BONDED
    } catch (e: SecurityException) {
      return@withContext Result.failure(e)
    }
    if (!isBonded) {
      return@withContext Result.failure(IOException("Pair both phones in Android Bluetooth settings before sending"))
    }

    var socket: BluetoothSocket? = null
    try {
      val keyPair = TransferCrypto.createEphemeralKeyPair()
      socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
      socket.connect()
      DataInputStream(socket.inputStream).use { input ->
        DataOutputStream(socket.outputStream).use { output ->
          writeBytes(output, keyPair.public.encoded)
          val peerPublicKey = readBytes(input, 2048)
          val aesKey = TransferCrypto.deriveAesKey(keyPair, peerPublicKey)
          val transferId = TransferCrypto.sha256(bytes)
          val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "shared-file" }

          output.writeUTF(safeName)
          output.writeUTF(mimeType.take(120))
          output.writeUTF(transferId)
          output.writeLong(bytes.size.toLong())
          output.writeInt(CHUNK_SIZE)
          output.flush()

          val resumeOffset = input.readLong().coerceIn(0L, bytes.size.toLong())
          var offset = (resumeOffset / CHUNK_SIZE) * CHUNK_SIZE
          if (resumeOffset == bytes.size.toLong()) offset = resumeOffset
          var index = (offset / CHUNK_SIZE).toInt()

          while (offset < bytes.size) {
            val end = minOf(bytes.size.toLong(), offset + CHUNK_SIZE).toInt()
            val chunk = bytes.copyOfRange(offset.toInt(), end)
            val encrypted = TransferCrypto.encryptChunk(aesKey, transferId, index, chunk)
            output.writeInt(index)
            writeBytes(output, encrypted.first)
            writeBytes(output, encrypted.second)
            output.flush()

            val acknowledged = input.readInt()
            if (acknowledged != index) throw IOException("Receiver acknowledgement mismatch")
            offset += chunk.size
            index += 1
            onProgress(offset, bytes.size.toLong())
          }
          output.writeInt(-1)
          output.flush()
          if (input.readInt() != 1) throw IOException("Receiver could not finalize the file")
        }
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(TAG, "Bluetooth file send failed", e)
      Result.failure(e)
    } finally {
      runCatching { socket?.close() }
    }
  }

  suspend fun receiveFile(
    onProgress: (Long, Long) -> Unit = { _, _ -> }
  ): Result<ReceivedTransfer> = withContext(Dispatchers.IO) {
    val bluetoothAdapter = adapter
      ?: return@withContext Result.failure(IOException("This device has no Bluetooth adapter"))
    if (!hasConnectPermission()) return@withContext Result.failure(SecurityException("Bluetooth connect permission is required"))
    if (!bluetoothAdapter.isEnabled) return@withContext Result.failure(IOException("Bluetooth is turned off"))

    var server: BluetoothServerSocket? = null
    var socket: BluetoothSocket? = null
    try {
      server = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
      serverSocket = server
      socket = server.accept()
      serverSocket = null

      DataInputStream(socket.inputStream).use { input ->
        DataOutputStream(socket.outputStream).use { output ->
          val peerPublicKey = readBytes(input, 2048)
          val keyPair = TransferCrypto.createEphemeralKeyPair()
          writeBytes(output, keyPair.public.encoded)
          val aesKey = TransferCrypto.deriveAesKey(keyPair, peerPublicKey)

          val fileName = input.readUTF().take(120)
          val mimeType = input.readUTF().take(120)
          val transferId = input.readUTF()
          val totalBytes = input.readLong()
          val chunkSize = input.readInt()
          if (!transferId.matches(Regex("^[0-9a-f]{64}$")) ||
            totalBytes !in 1..MAX_TRANSFER_BYTES || chunkSize != CHUNK_SIZE
          ) throw IOException("Invalid transfer metadata")

          val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "received-file" }
          val partialFile = File(context.cacheDir, "tribe-" + transferId + ".partial")
          if (partialFile.exists() && partialFile.length() > totalBytes) partialFile.delete()
          val alignedOffset = minOf(partialFile.length(), totalBytes) / CHUNK_SIZE * CHUNK_SIZE
          if (partialFile.length() != alignedOffset && alignedOffset < totalBytes) {
            RandomAccessFile(partialFile, "rw").use { it.setLength(alignedOffset) }
          }
          var offset = if (partialFile.length() == totalBytes) totalBytes else alignedOffset
          output.writeLong(offset)
          output.flush()
          var expectedIndex = (offset / CHUNK_SIZE).toInt()

          while (true) {
            val index = input.readInt()
            if (index == -1) break
            if (index != expectedIndex) throw IOException("Unexpected chunk index")
            val nonce = readBytes(input, 32)
            val encryptedChunk = readBytes(input, CHUNK_SIZE + 64)
            val plainChunk = TransferCrypto.decryptChunk(aesKey, transferId, index, nonce, encryptedChunk)
            RandomAccessFile(partialFile, "rw").use { file ->
              file.seek(offset)
              file.write(plainChunk)
            }
            offset += plainChunk.size
            expectedIndex += 1
            output.writeInt(index)
            output.flush()
            onProgress(offset, totalBytes)
          }

          if (!partialFile.exists() || partialFile.length() != totalBytes ||
            TransferCrypto.sha256(partialFile.readBytes()) != transferId
          ) throw IOException("Transfer checksum mismatch; partial file retained for resume")

          val receivedDir = File(context.filesDir, "received").apply { mkdirs() }
          val encryptedFile = File(receivedDir, transferId + ".tribe")
          EncryptedReceivedFileStore.encrypt(partialFile, encryptedFile)
          partialFile.delete()
          output.writeInt(1)
          output.flush()
          Result.success(ReceivedTransfer(safeName, mimeType, encryptedFile, totalBytes))
        }
      }
    } catch (e: SecurityException) {
      Log.w(TAG, "Bluetooth connect permission was revoked during receive", e)
      Result.failure(e)
    } catch (e: Exception) {
      Log.e(TAG, "Bluetooth file receive failed", e)
      Result.failure(e)
    } finally {
      serverSocket = null
      activeSocket = null
      runCatching { socket?.close() }
      runCatching { server?.close() }
    }
  }

  fun stopReceiving() {
    runCatching { serverSocket?.close() }
    runCatching { activeSocket?.close() }
    serverSocket = null
    activeSocket = null
  }

  fun exportReceivedFile(transfer: ReceivedTransfer): Uri? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
    return try {
      val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, transfer.fileName)
        put(MediaStore.Downloads.MIME_TYPE, transfer.mimeType)
        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TRIBE")
        put(MediaStore.Downloads.IS_PENDING, 1)
      }
      val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: return null
      val plainBytes = EncryptedReceivedFileStore.decrypt(transfer.encryptedFile)
      context.contentResolver.openOutputStream(uri)?.use { it.write(plainBytes) }
        ?: return null
      val complete = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
      context.contentResolver.update(uri, complete, null, null)
      uri
    } catch (e: Exception) {
      Log.e(TAG, "Could not export received file", e)
      null
    }
  }

  private fun writeBytes(output: DataOutputStream, bytes: ByteArray) {
    output.writeInt(bytes.size)
    output.write(bytes)
  }

  private fun readBytes(input: DataInputStream, maxLength: Int): ByteArray {
    val length = input.readInt()
    if (length !in 1..maxLength) throw IOException("Invalid frame length")
    return ByteArray(length).also { input.readFully(it) }
  }
}

private object TransferCrypto {
  private val random = SecureRandom()

  fun createEphemeralKeyPair(): KeyPair {
    return KeyPairGenerator.getInstance("EC").apply {
      initialize(ECGenParameterSpec("secp256r1"), random)
    }.generateKeyPair()
  }

  fun deriveAesKey(localPair: KeyPair, remotePublicKeyBytes: ByteArray): javax.crypto.SecretKey {
    val remotePublicKey = KeyFactory.getInstance("EC")
      .generatePublic(X509EncodedKeySpec(remotePublicKeyBytes))
    val agreement = KeyAgreement.getInstance("ECDH")
    agreement.init(localPair.private)
    agreement.doPhase(remotePublicKey, true)
    val sharedSecret = agreement.generateSecret()

    val extract = Mac.getInstance("HmacSHA256")
    extract.init(SecretKeySpec(ByteArray(32), "HmacSHA256"))
    val pseudorandomKey = extract.doFinal(sharedSecret)
    val expand = Mac.getInstance("HmacSHA256")
    expand.init(SecretKeySpec(pseudorandomKey, "HmacSHA256"))
    val info = "TRIBE-RFCOMM-AES-GCM-v1".toByteArray(Charsets.UTF_8) + byteArrayOf(1)
    return SecretKeySpec(expand.doFinal(info).copyOf(32), "AES")
  }

  fun encryptChunk(key: javax.crypto.SecretKey, transferId: String, index: Int, plain: ByteArray): Pair<ByteArray, ByteArray> {
    val nonce = ByteArray(12).also { random.nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
    cipher.updateAAD((transferId + ":" + index).toByteArray(Charsets.UTF_8))
    return nonce to cipher.doFinal(plain)
  }

  fun decryptChunk(
    key: javax.crypto.SecretKey,
    transferId: String,
    index: Int,
    nonce: ByteArray,
    encrypted: ByteArray
  ): ByteArray {
    if (nonce.size != 12) throw IOException("Invalid encryption nonce")
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, nonce))
    cipher.updateAAD((transferId + ":" + index).toByteArray(Charsets.UTF_8))
    return cipher.doFinal(encrypted)
  }

  fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 0xff) }

}

private object EncryptedReceivedFileStore {
  private const val KEY_ALIAS = "tribe_received_file_aes_v1"

  private fun getOrCreateKey(): javax.crypto.SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val existing = keyStore.getKey(KEY_ALIAS, null)
    if (existing is javax.crypto.SecretKey) return existing

    val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
    generator.init(
      KeyGenParameterSpec.Builder(
        KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()
    )
    return generator.generateKey()
  }


  fun decrypt(file: File): ByteArray {
    DataInputStream(file.inputStream()).use { input ->
      val ivLength = input.readInt()
      if (ivLength != 12) throw IOException("Invalid encrypted file header")
      val iv = ByteArray(ivLength).also { input.readFully(it) }
      val encryptedLength = input.readInt()
      if (encryptedLength !in 1..(20 * 1024 * 1024 + 64)) throw IOException("Invalid encrypted file size")
      val encrypted = ByteArray(encryptedLength).also { input.readFully(it) }
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
      return cipher.doFinal(encrypted)
    }
  }

  fun encrypt(source: File, destination: File) {
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
    val encrypted = cipher.doFinal(source.readBytes())
    destination.outputStream().use { stream ->
      DataOutputStream(stream).use { output ->
        output.writeInt(cipher.iv.size)
        output.write(cipher.iv)
        output.writeInt(encrypted.size)
        output.write(encrypted)
      }
    }
  }
}
