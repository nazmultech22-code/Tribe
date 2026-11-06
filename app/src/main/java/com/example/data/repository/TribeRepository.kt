package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.SessionState
import com.example.data.local.UserSessionManager
import com.example.data.model.*
import com.example.data.remote.SupabaseClient
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.StateFlow

class TribeRepository(
  private val sessionManager: UserSessionManager,
  val supabaseClient: SupabaseClient
) {

  val sessionState: StateFlow<SessionState> = sessionManager.sessionState

  fun setLanguage(language: AppLanguage) {
    sessionManager.setLanguage(language)
  }

  suspend fun getDestinations(): List<Destination> {
    val remote = supabaseClient.getDestinations()
    return if (remote.isNotEmpty()) remote else DEFAULT_POPULAR_DESTINATIONS
  }

  suspend fun getDestinationById(id: String): Destination? {
    if (id.isBlank()) return null
    val remote = supabaseClient.getDestinationById(id)
    if (remote != null) return remote
    return DEFAULT_POPULAR_DESTINATIONS.firstOrNull { it.id == id }
  }

  suspend fun searchDestinations(query: String): List<Destination> {
    val q = query.trim()
    val remote = supabaseClient.searchDestinations(q)
    if (remote.isNotEmpty()) return remote

    if (q.isEmpty()) return DEFAULT_POPULAR_DESTINATIONS

    val normalized = q.replace(Regex("[^A-Za-z0-9\\u0980-\\u09FF]"), "").lowercase()
    return DEFAULT_POPULAR_DESTINATIONS.filter { d ->
      val nameNorm = d.name.replace(Regex("[^A-Za-z0-9\\u0980-\\u09FF]"), "").lowercase()
      val descNorm = (d.description ?: "").replace(Regex("[^A-Za-z0-9\\u0980-\\u09FF]"), "").lowercase()
      val regNorm = (d.region ?: "").replace(Regex("[^A-Za-z0-9\\u0980-\\u09FF]"), "").lowercase()
      nameNorm.contains(normalized) || descNorm.contains(normalized) || regNorm.contains(normalized) ||
        (normalized.contains("cox") && d.name.contains("Cox", ignoreCase = true)) ||
        (normalized.contains("sajek") && d.name.contains("Sajek", ignoreCase = true)) ||
        (normalized.contains("সাজেক") && d.name.contains("সাজেক")) ||
        (normalized.contains("কক্স") && d.name.contains("কক্স"))
    }
  }

  suspend fun initializeAndSeedIfNeeded() {
    try {
      val existing = supabaseClient.getDestinations()
      if (existing.isEmpty() && sessionState.value.isLoggedIn) {
        // Seed default popular destinations
        seedPopularDestinations()
      }
    } catch (e: Exception) {
      Log.w("TribeRepository", "Seed check skipped: ${e.message}")
    }
  }

  suspend fun seedPopularDestinations() {
    for (d in SEED_DATA) {
      supabaseClient.createDestination(
        name = d.name,
        country = d.country,
        region = d.region,
        description = d.description,
        bestSeason = d.bestSeason,
        transportInfo = d.transportInfo,
        accommodationInfo = d.accommodationInfo,
        foodInfo = d.foodInfo,
        guideInfo = d.guideInfo,
        safetyTips = d.safetyTips,
        bnAliases = d.bnAliases,
        enAliases = d.enAliases
      )
    }
  }

  companion object {
    @Volatile
    private var INSTANCE: TribeRepository? = null

    val DEFAULT_POPULAR_DESTINATIONS = listOf(
      Destination(
        id = "d1000000-0000-0000-0000-000000000001",
        name = "সাজেক ভ্যালি (Sajek Valley)",
        country = "Bangladesh",
        region = "Rangamati Hill District",
        description = "মেঘের রাজ্য সাজেক ভ্যালি বাংলাদেশের সবচেয়ে আকর্ষণীয় পাহাড়ি উপত্যকা।",
        bestSeason = "অক্টোবর - মার্চ (শীত ও হেমন্ত)",
        transportInfo = "ঢাকা থেকে খাগড়াছড়ি বাস, এরপর চাঁদের গাড়িতে সাজেক ভ্যালি।",
        accommodationInfo = "রিসোর্ট রুন্ময়, মেঘ মাচাং, সাজেক হিল ভিউ ও ঐতিহ্যবাহী কটেজ।",
        foodInfo = "স্থানীয় ব্যাম্বু চিকেন, পাহাড়ি কলা ও ট্রেডিশনাল বাংলা খাবার।",
        guideInfo = "স্থানীয় অনুমোদিত চাঁদের গাড়ি ড্রাইভার ও রেজিস্টার্ড ট্রাভেল গাইড।",
        safetyTips = "সেনাবাহিনীর এসকর্ট মেনে চলুন। আইডি কার্ড ও প্রয়োজনীয় ওষুধ সাথে রাখুন।"
      ),
      Destination(
        id = "d1000000-0000-0000-0000-000000000002",
        name = "কক্সবাজার (Cox's Bazar)",
        country = "Bangladesh",
        region = "Chittagong Division",
        description = "বিশ্বের দীর্ঘতম ১২০ কিমি প্রাকৃতিক অবিচ্ছিন্ন বালুকাময় সমুদ্র সৈকত।",
        bestSeason = "নভেম্বর - এপ্রিল (শীতকালীন রৌদ্রোজ্জ্বল আবহাওয়া)",
        transportInfo = "ঢাকা থেকে সরাসরি বিমান, এসি বাস অথবা নতুন কক্সবাজার এক্সপ্রেস ট্রেন।",
        accommodationInfo = "কলাতলী, সুগন্ধা ও লাবণী পয়েন্টের অসংখ্য হোটেল, রিসোর্ট ও মোটেল।",
        foodInfo = "রূপচাঁদা ফ্রাই, লইট্টা ফ্রাই, সামুদ্রিক মাছের বারবিকিউ ও শুঁটকি ভর্তা।",
        guideInfo = "বিচ ট্যুরিস্ট পুলিশ বুথ ও স্থানীয় নিবন্ধিত ট্যুর অপারেটর।",
        safetyTips = "লাল পতাকা সংকেত থাকলে সাগরে নামবেন না। গভীর রাতে নির্জন বিচে যাবেন না।"
      ),
      Destination(
        id = "d1000000-0000-0000-0000-000000000003",
        name = "বান্দরবান (Bandarban)",
        country = "Bangladesh",
        region = "Chittagong Hill Tracts",
        description = "নীলাচল, নীলগিরি, বগালেক ও নাফাখুমের অপার সৌন্দর্যের পাহাড়ি রানী।",
        bestSeason = "সেপ্টেম্বর - ফেব্রুয়ারি",
        transportInfo = "ঢাকা থেকে সরাসরি বাস অথবা চট্টগ্রাম হয়ে বান্দরবান সদর।",
        accommodationInfo = "নীলগিরি রিসোর্ট, মিলানছড়ি রিসোর্ট ও বান্দরবান শহরের হোটেল সমূহ।",
        foodInfo = "মারমা ও পাহাড়ি জনগোষ্ঠীর ঐতিহ্যবাহী খাবার, পাহাড়ি পেঁপে ও ফলমূল।",
        guideInfo = "ট্র্যাকিং রুটের জন্য স্থানীয় অনুমোদিত রেজিস্টার্ড গাইড বাধ্যতামূলক।",
        safetyTips = "পাহাড়ে ভ্রমণের জন্য জাতীয় পরিচয়পত্র ও অনুমতিপত্র ফটোকপি সাথে রাখুন।"
      ),
      Destination(
        id = "d1000000-0000-0000-0000-000000000004",
        name = "সেন্ট মার্টিন (Saint Martin's Island)",
        country = "Bangladesh",
        region = "Bay of Bengal",
        description = "বাংলাদেশের একমাত্র প্রবাল দ্বীপ ও নারিকেল জিঞ্জিরা। স্বচ্ছ নীল জলরাশি।",
        bestSeason = "নভেম্বর - ফেব্রুয়ারি (জাহাজ চলাচলের মৌসুম)",
        transportInfo = "টেকনাফ অথবা কক্সবাজার ঘাট থেকে নিয়মিত সমুদ্রগামী ক্রুজ শিপ।",
        accommodationInfo = "ছেঁড়া দ্বীপের কাছাকাছি ইকো রিসোর্ট ও বিচ ভিউ হোটেল।",
        foodInfo = "তাজা ডাব, সামুদ্রিক কাঁকড়া, টুনা ও রূপচাঁদা মাছের ফ্রাই।",
        guideInfo = "দ্বীপের অনুমোদিত ভলান্টিয়ার ও পরিবেশ সংরক্ষণ গাইড।",
        safetyTips = "প্রবালের ক্ষতি করবেন না। প্লাস্টিক বর্জ্য সমুদ্রে ফেলা সম্পূর্ণ নিষেধ।"
      )
    )

    private val SEED_DATA = listOf(
      SeedDestination(
        name = "সাজেক ভ্যালি (Sajek Valley)",
        country = "Bangladesh",
        region = "Rangamati Hill District",
        description = "মেঘের রাজ্য সাজেক ভ্যালি বাংলাদেশের সবচেয়ে আকর্ষণীয় পাহাড়ি উপত্যকা।",
        bestSeason = "অক্টোবর - মার্চ (শীত ও হেমন্ত)",
        transportInfo = "ঢাকা থেকে খাগড়াছড়ি বাস, এরপর চাঁদের গাড়িতে সাজেক ভ্যালি।",
        accommodationInfo = "রিসোর্ট রুন্ময়, মেঘ মাচাং, সাজেক হিল ভিউ ও ঐতিহ্যবাহী কটেজ।",
        foodInfo = "স্থানীয় ব্যাম্বু চিকেন, পাহাড়ি কলা ও ট্রেডিশনাল বাংলা খাবার।",
        guideInfo = "স্থানীয় অনুমোদিত চাঁদের গাড়ি ড্রাইভার ও রেজিস্টার্ড ট্রাভেল গাইড।",
        safetyTips = "সেনাবাহিনীর এসকর্ট মেনে চলুন। আইডি কার্ড ও প্রয়োজনীয় ওষুধ সাথে রাখুন।",
        bnAliases = listOf("সাজেক", "সাজেক ভ্যালি", "মেঘের রাজ্য সাজেক"),
        enAliases = listOf("Sajek", "Sajek Valley", "Sajekvally")
      ),
      SeedDestination(
        name = "কক্সবাজার (Cox's Bazar)",
        country = "Bangladesh",
        region = "Chittagong Division",
        description = "বিশ্বের দীর্ঘতম ১২০ কিমি প্রাকৃতিক অবিচ্ছিন্ন বালুকাময় সমুদ্র সৈকত।",
        bestSeason = "নভেম্বর - এপ্রিল (শীতকালীন রৌদ্রোজ্জ্বল আবহাওয়া)",
        transportInfo = "ঢাকা থেকে সরাসরি বিমান, এসি বাস অথবা নতুন কক্সবাজার এক্সপ্রেস ট্রেন।",
        accommodationInfo = "কলাতলী, সুগন্ধা ও লাবণী পয়েন্টের অসংখ্য হোটেল, রিসোর্ট ও মোটেল।",
        foodInfo = "রূপচাঁদা ফ্রাই, লইট্টা ফ্রাই, সামুদ্রিক মাছের বারবিকিউ ও শুঁটকি ভর্তা।",
        guideInfo = "বিচ ট্যুরিস্ট পুলিশ বুথ ও স্থানীয় নিবন্ধিত ট্যুর অপারেটর।",
        safetyTips = "লাল পতাকা সংকেত থাকলে সাগরে নামবেন না। গভীর রাতে নির্জন বিচে যাবেন না।",
        bnAliases = listOf("কক্সবাজার", "কক্স বাজার", "কক্স", "সি ও এক", "সমুদ্র সৈকত"),
        enAliases = listOf("Cox's Bazar", "Cox Bazar", "Coxs Bazar", "C-O-X", "Cox")
      ),
      SeedDestination(
        name = "বান্দরবান (Bandarban)",
        country = "Bangladesh",
        region = "Chittagong Hill Tracts",
        description = "নীলাচল, নীলগিরি, বগালেক ও নাফাখুমের অপার সৌন্দর্যের পাহাড়ি রানী।",
        bestSeason = "সেপ্টেম্বর - ফেব্রুয়ারি",
        transportInfo = "ঢাকা থেকে সরাসরি বাস অথবা চট্টগ্রাম হয়ে বান্দরবান সদর।",
        accommodationInfo = "নীলগিরি রিসোর্ট, মিলানছড়ি রিসোর্ট ও বান্দরবান শহরের হোটেল সমূহ।",
        foodInfo = "মারমা ও পাহাড়ি জনগোষ্ঠীর ঐতিহ্যবাহী খাবার, পাহাড়ি পেঁপে ও ফলমূল।",
        guideInfo = "ট্র্যাকিং রুটের জন্য স্থানীয় অনুমোদিত রেজিস্টার্ড গাইড বাধ্যতামূলক।",
        safetyTips = "পাহাড়ে ভ্রমণের জন্য জাতীয় পরিচয়পত্র ও অনুমতিপত্র ফটোকপি সাথে রাখুন।",
        bnAliases = listOf("বান্দরবান", "বান্দারবান", "নীলগিরি", "নীলাচল"),
        enAliases = listOf("Bandarban", "Nilgiri", "Nilachal")
      ),
      SeedDestination(
        name = "সেন্ট মার্টিন (Saint Martin's Island)",
        country = "Bangladesh",
        region = "Bay of Bengal",
        description = "বাংলাদেশের একমাত্র প্রবাল দ্বীপ ও নারিকেল জিঞ্জিরা। স্বচ্ছ নীল জলরাশি।",
        bestSeason = "নভেম্বর - ফেব্রুয়ারি (জাহাজ চলাচলের মৌসুম)",
        transportInfo = "টেকনাফ অথবা কক্সবাজার ঘাট থেকে নিয়মিত সমুদ্রগামী ক্রুজ শিপ।",
        accommodationInfo = "ছেঁড়া দ্বীপের কাছাকাছি ইকো রিসোর্ট ও বিচ ভিউ হোটেল।",
        foodInfo = "তাজা ডাব, সামুদ্রিক কাঁকড়া, টুনা ও রূপচাঁদা মাছের ফ্রাই।",
        guideInfo = "দ্বীপের অনুমোদিত ভলান্টিয়ার ও পরিবেশ সংরক্ষণ গাইড।",
        safetyTips = "প্রবালের ক্ষতি করবেন না। প্লাস্টিক বর্জ্য সমুদ্রে ফেলা সম্পূর্ণ নিষেধ।",
        bnAliases = listOf("সেন্ট মার্টিন", "সেন্টমার্টিন", "নারিকেল জিঞ্জিরা", "ছেঁড়া দ্বীপ"),
        enAliases = listOf("Saint Martin", "Saint Martin's Island", "St Martin", "Chera Dwip")
      )
    )

    fun getInstance(context: Context): TribeRepository {
      return INSTANCE ?: synchronized(this) {
        val sessionMgr = UserSessionManager.getInstance(context)
        val client = SupabaseClient(sessionMgr)
        INSTANCE ?: TribeRepository(sessionMgr, client).also { INSTANCE = it }
      }
    }
  }
}

data class SeedDestination(
  val name: String,
  val country: String,
  val region: String,
  val description: String,
  val bestSeason: String,
  val transportInfo: String,
  val accommodationInfo: String,
  val foodInfo: String,
  val guideInfo: String,
  val safetyTips: String,
  val bnAliases: List<String>,
  val enAliases: List<String>
)
