package com.example.i18n

object L10n {
  fun formatCurrency(amount: Double?, lang: AppLanguage): String {
    if (amount == null) return if (lang == AppLanguage.BANGLA) "৳ ০" else "৳ 0"
    val intVal = amount.toLong()
    return if (lang == AppLanguage.BANGLA) {
      val banglaDigits = intVal.toString().map { ch ->
        when (ch) {
          '0' -> '০'; '1' -> '১'; '2' -> '২'; '3' -> '৩'; '4' -> '৪'
          '5' -> '৫'; '6' -> '৬'; '7' -> '৭'; '8' -> '৮'; '9' -> '৯'
          else -> ch
        }
      }.joinToString("")
      "৳ $banglaDigits"
    } else {
      "৳ %,d".format(intVal)
    }
  }

  fun toBanglaDigits(num: Int): String {
    return num.toString().map { ch ->
      when (ch) {
        '0' -> '০'; '1' -> '১'; '2' -> '২'; '3' -> '৩'; '4' -> '৪'
        '5' -> '৫'; '6' -> '৬'; '7' -> '৭'; '8' -> '৮'; '9' -> '৯'
        else -> ch
      }
    }.joinToString("")
  }

  // App Identity
  fun appName(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ট্রাইব" else "TRIBE"
  fun appTagline(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "প্ল্যান • ট্রাভেল • শেয়ার" else "Plan • Travel • Share"
  fun heroSub(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ভ্রমণ হোক আরও পরিকল্পিত, আরও স্মার্ট!" else "Make travel more planned, more smart!"
  fun communityBadge(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ভ্রমণের সাথী আপনার নিজের কমিউনিটি" else "Your Travel Community in Bangladesh"

  // Bottom Navigation
  fun navExplore(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "এক্সপ্লোর" else "Explore"
  fun navMyTours(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "আমার ট্যুর" else "My Tours"
  fun navCommunity(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "কমিউনিটি" else "Community"
  fun navProfile(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "প্রোফাইল" else "Profile"
  fun navCreate(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "নতুন ট্যুর" else "New Tour"

  // Auth
  fun loginTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "লগইন করুন" else "Sign In"
  fun signupTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "অ্যাকাউন্ট তৈরি করুন" else "Sign Up"
  fun email(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ইমেইল" else "Email"
  fun password(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পাসওয়ার্ড" else "Password"
  fun fullName(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পূর্ণ নাম" else "Full Name"
  fun username(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ইউজারনেম" else "Username"
  fun forgotPassword(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পাসওয়ার্ড ভুলে গেছেন?" else "Forgot password?"
  fun resetPassword(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পাসওয়ার্ড রিসেট" else "Reset Password"
  fun resetPasswordSent(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পাসওয়ার্ড রিসেট ইমেইল পাঠানো হয়েছে!" else "Password reset email sent!"
  fun sendResetLink(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "রিসেট লিংক পাঠান" else "Send Reset Link"
  fun dontHaveAccount(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "অ্যাকাউন্ট নেই? সাইন আপ করুন" else "Don't have an account? Sign Up"
  fun alreadyHaveAccount(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ইতিমধ্যে অ্যাকাউন্ট আছে? লগইন করুন" else "Already have an account? Sign In"
  fun signOut(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সাইন আউট" else "Sign Out"
  fun signOutConfirm(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "আপনি কি নিশ্চিতভাবে সাইন আউট করতে চান?" else "Are you sure you want to sign out?"
  fun googleLoginUnavailable(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "গুগল লগইন এখনো প্রজেক্টে কনফিগার করা হয়নি। ইমেইল এবং পাসওয়ার্ড দিয়ে প্রবেশ করুন। গুগল লগইন চালুর জন্য Supabase ড্যাশবোর্ডে Google OAuth প্রোভাইডার ক্রেডেনশিয়াল কনফিগার করতে হবে।" else "Google Login is not yet configured in this project. Please sign in with email and password. To enable Google Login, configure the Google OAuth provider credentials in your Supabase Dashboard."
  fun authSuccess(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সফলভাবে সম্পন্ন হয়েছে!" else "Success!"

  // Home Screen
  fun greeting(name: String, lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "স্বাগতম, $name 👋" else "Welcome, $name 👋"
  fun guest(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "অতিথি" else "Guest"
  fun searchPlaceholder(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "কোথাও যেতে চান? সার্চ করুন..." else "Where do you want to go? Search..."
  fun quickAccess(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "দ্রুত অ্যাক্সেস" else "Quick Access"
  fun planNewTour(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "নতুন ট্যুর প্ল্যান" else "Plan New Tour"
  fun viewCommunity(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "কমিউনিটি দেখুন" else "View Community"
  fun destinationGuide(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "গন্তব্য গাইড" else "Destination Guide"
  fun myTravelHistory(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ভ্রমণ ইতিহাস" else "Travel History"
  fun popularDestinations(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "জনপ্রিয় গন্তব্য" else "Popular Destinations"
  fun seeAll(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "আরও দেখুন →" else "See All →"
  fun featuredDestination(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "আকর্ষণীয় ভ্রমণ স্পট" else "Featured Destination"

  // Destination Search & System
  fun searchTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "গন্তব্য অনুসন্ধান" else "Destination Search"
  fun noDestinationFound(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "কোনো গন্তব্য পাওয়া যায়নি" else "No destination found"
  fun createNewDestination(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "+ নতুন গন্তব্য যোগ করুন" else "+ Add New Destination"
  fun createDestinationTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "নতুন গন্তব্য তৈরি করুন" else "Create Destination"
  fun destinationName(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "গন্তব্যের নাম *" else "Destination Name *"
  fun country(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "দেশ" else "Country"
  fun region(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "অঞ্চল / জেলা" else "Region / District"
  fun description(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "বিবরণ" else "Description"
  fun bestSeason(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ভ্রমণের সেরা সময়" else "Best Visiting Season"
  fun transportInfo(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "যাতায়াত ও পরিবহন তথ্য" else "Transport Information"
  fun hotelInfo(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "হোটেল ও থাকার ব্যবস্থা" else "Hotel & Accommodation"
  fun foodInfo(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "খাবার ও রেস্তোরাঁ" else "Food & Restaurants"
  fun guideInfo(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "লোকাল গাইড তথ্য" else "Local Guide Information"
  fun safetyTips(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "নিরাপত্তা ও সতর্কতা টিপস" else "Safety Tips"
  fun bnAlias(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "বাংলা বিকল্প নাম / বানান (কমা দিয়ে আলাদা করুন)" else "Bengali Aliases (comma separated)"
  fun enAlias(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ইংরেজি বিকল্প নাম / বানান (কমা দিয়ে আলাদা করুন)" else "English Aliases (comma separated)"
  fun addAliasBtn(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "+ বিকল্প নাম যোগ করুন" else "+ Add Alias"
  fun aliasAddedSuccess(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "বিকল্প নাম সফলভাবে যোগ হয়েছে!" else "Alias added successfully!"
  fun destinationCreatedSuccess(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "গন্তব্য সফলভাবে তৈরি হয়েছে!" else "Destination created successfully!"

  // Destination Details
  fun sharedToursForDestination(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "এই গন্তব্যের শেয়ার করা ট্যুর প্ল্যান" else "Shared Tour Plans for This Destination"
  fun noSharedToursFound(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "এই গন্তব্যে এখনো কোনো শেয়ার করা ট্যুর প্ল্যান পাওয়া যায়নি।" else "No shared tour plans found for this destination."
  fun planTourHere(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "এখানে একটি ট্যুর প্ল্যান করুন" else "Plan a Tour Here"

  // Public Tour Cards
  fun activeMembers(count: Int, lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "${toBanglaDigits(count)} জন সদস্য" else "$count members"
  fun viewDetails(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "বিস্তারিত দেখুন" else "View Details"
  fun estimatedBudget(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "আনুমানিক বাজেট" else "Estimated Budget"
  fun actualExpense(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "মোট খরচ" else "Total Expenses"
  fun perPersonEstimated(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "জনপ্রতি আনুমানিক" else "Per Person Est."
  fun perPersonActual(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "জনপ্রতি প্রকৃত খরচ" else "Per Person Actual"
  fun noExpensesRecorded(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "কোনো খরচ এখনো যোগ করা হয়নি" else "No expenses recorded yet"
  fun expenseBreakdown(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "খরচের বিবরণ" else "Expense Breakdown"
  fun transportExpense(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পরিবহন" else "Transport"
  fun accommodationExpense(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "হোটেল ও আবাসন" else "Accommodation"
  fun foodExpense(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "খাবার" else "Food"
  fun otherExpense(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "অন্যান্য" else "Other"

  // Tour Sharing & Privacy
  fun shareTour(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ট্যুর শেয়ার করুন" else "Share Tour"
  fun makePrivate(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "প্রাইভেট করুন" else "Make Private"
  fun visibilityPublic(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পাবলিক (সবার জন্য উন্মুক্ত)" else "Public (Discoverable)"
  fun visibilityPrivate(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "প্রাইভেট (শুধুমাত্র সদস্যরা)" else "Private (Members only)"
  fun visibilityUnlisted(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "আনলিস্টেড (লিংক দিয়ে)" else "Unlisted (Link only)"
  fun sharedSuccess(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ট্যুরটি সফলভাবে পাবলিকে শেয়ার করা হয়েছে!" else "Tour successfully shared publicly!"
  fun madePrivateSuccess(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ট্যুরটি সফলভাবে প্রাইভেট করা হয়েছে।" else "Tour set to private."
  fun mustSelectDestinationBeforeSharing(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পাবলিক শেয়ার করার আগে একটি গন্তব্য নির্বাচন করতে হবে।" else "You must select a destination before sharing publicly."
  fun onlyOwnerCanShare(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "শুধুমাত্র ট্যুর মালিক বা ম্যানেজার শেয়ারিং পরিবর্তন করতে পারেন।" else "Only the tour owner or manager can change sharing visibility."

  // Tour Creation & Management
  fun myToursTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "আমার ভ্রমণ সমূহ" else "My Tours"
  fun createTourTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "নতুন ট্যুর প্ল্যান" else "New Tour Plan"
  fun tourTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ট্যুরের নাম *" else "Tour Title *"
  fun selectDestination(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "গন্তব্য নির্বাচন করুন *" else "Select Destination *"
  fun startDate(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "শুরুর তারিখ (YYYY-MM-DD)" else "Start Date (YYYY-MM-DD)"
  fun endDate(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "শেষের তারিখ (YYYY-MM-DD)" else "End Date (YYYY-MM-DD)"
  fun maxMembers(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সর্বোচ্চ সদস্য সংখ্যা" else "Max Members"
  fun meetingPoint(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "মিটিং পয়েন্ট / শুরুর স্থান" else "Meeting Point"
  fun saveTour(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সংরক্ষণ করুন" else "Save Tour"
  fun tourCreatedSuccess(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ট্যুর সফলভাবে তৈরি হয়েছে!" else "Tour created successfully!"

  // Tour Details & Tabs
  fun tabOverview(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সংক্ষিপ্ত ও খরচ" else "Overview & Expenses"
  fun tabItinerary(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ইটিনারি (সময়সূচি)" else "Itinerary"
  fun tabChecklist(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "চেকলিস্ট" else "Checklist"
  fun tabMembers(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সদস্যবৃন্দ" else "Members"
  fun tabGuides(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "গাইড" else "Guides"
  fun tabComments(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "মন্তব্য" else "Comments"

  // Status
  fun statusPlanning(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পরিকল্পিত" else "Planning"
  fun statusOngoing(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "চলমান" else "Ongoing"
  fun statusCompleted(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সম্পন্ন" else "Completed"
  fun statusCancelled(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "বাতিল" else "Cancelled"

  // Expenses CRUD
  fun addExpense(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "+ নতুন খরচ যোগ করুন" else "+ Add Expense"
  fun expenseTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "খরচের খাত / শিরোনাম *" else "Expense Title *"
  fun expenseAmount(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "টাকার পরিমাণ (৳) *" else "Amount (৳) *"
  fun expenseCategory(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ক্যাটাগরি" else "Category"
  fun expenseAddedSuccess(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "খরচ সফলভাবে যোগ করা হয়েছে!" else "Expense added successfully!"

  // Itinerary CRUD
  fun addItineraryDay(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "+ সময়সূচি যোগ করুন" else "+ Add Day Plan"
  fun dayNumber(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "দিন নম্বর (যেমন: ১)" else "Day Number (e.g. 1)"
  fun itineraryTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "কার্যক্রম / শিরোনাম *" else "Activity Title *"
  fun location(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "স্থান" else "Location"

  // Checklist CRUD
  fun addChecklistItem(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "+ নতুন আইটেম" else "+ New Checklist Item"
  fun checklistItemPlaceholder(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "আইটেমের নাম (যেমন: জাতীয় পরিচয়পত্র, ফার্স্ট এইড)" else "Item title (e.g. NID, First Aid)"

  // Guides CRUD
  fun addGuide(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "+ লোকাল গাইড যোগ করুন" else "+ Add Local Guide"
  fun guideName(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "গাইডের নাম *" else "Guide Name *"
  fun phone(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ফোন নম্বর" else "Phone Number"

  // Comments CRUD
  fun addComment(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "মন্তব্য লিখুন..." else "Write a comment..."
  fun postComment(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পোস্ট" else "Post"

  // Likes & Saves
  fun like(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পছন্দ" else "Like"
  fun save(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সংরক্ষণ" else "Save"
  fun savedTours(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সংরক্ষিত ট্যুর" else "Saved Tours"

  // Profile
  fun editProfile(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "প্রোফাইল সম্পাদনা" else "Edit Profile"
  fun bio(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "বায়ো" else "Bio"
  fun city(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "শহর" else "City"
  fun avatarUrl(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "প্রোফাইল ছবি লিংক" else "Avatar URL"
  fun profileUpdatedSuccess(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "প্রোফাইল সফলভাবে আপডেট হয়েছে!" else "Profile updated successfully!"
  fun languageSetting(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ভাষা পরিবর্তন (Language)" else "Change Language (ভাষা)"
  fun notifications(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "নোটিফিকেশন" else "Notifications"
  fun noNotifications(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "কোনো নতুন নোটিফিকেশন নেই" else "No new notifications"

  // Room Saved Trips
  fun savedTripsTitle(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সংরক্ষিত ট্রিপসমূহ (Room DB)" else "Saved Trips (Room DB)"
  fun noSavedTrips(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "রুম ডাটাবেজে কোনো সংরক্ষিত ট্রিপ নেই।" else "No saved trips found in Room database."
  fun addSavedTrip(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "+ নতুন ট্রিপ সেভ করুন" else "+ Save New Trip"
  fun saveToRoom(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "লোকাল ডাটাবেজে সেভ করুন" else "Save to Local DB"
  fun savedToRoomSuccess(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ট্রিপটি সফলভাবে Room ডাটাবেজে সংরক্ষিত হয়েছে!" else "Trip saved to Room database!"
  fun removedFromRoomSuccess(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ট্রিপটি Room ডাটাবেজ থেকে মুছে ফেলা হয়েছে।" else "Trip removed from Room database."
  fun roomDatabaseTag(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "অফলাইন Room স্টোরেজ" else "Offline Room Storage"

  // Common Actions
  fun saveBtn(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "সংরক্ষণ করুন" else "Save"
  fun cancelBtn(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "বাতিল" else "Cancel"
  fun closeBtn(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "বন্ধ করুন" else "Close"
  fun deleteBtn(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "মুছুন" else "Delete"
  fun back(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "ফিরে যান" else "Back"
  fun retry(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "পুনরায় চেষ্টা করুন" else "Retry"
  fun loading(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "লোড হচ্ছে..." else "Loading..."
  fun errorOccurred(lang: AppLanguage) = if (lang == AppLanguage.BANGLA) "একটি ত্রুটি ঘটেছে" else "An error occurred"
}
