package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Destination(
  val id: String = "",
  val name: String,
  val country: String? = null,
  val region: String? = null,
  val description: String? = null,
  @Json(name = "best_season") val bestSeason: String? = null,
  @Json(name = "transport_info") val transportInfo: String? = null,
  @Json(name = "accommodation_info") val accommodationInfo: String? = null,
  @Json(name = "food_info") val foodInfo: String? = null,
  @Json(name = "guide_info") val guideInfo: String? = null,
  @Json(name = "safety_tips") val safetyTips: String? = null,
  @Json(name = "image_url") val imageUrl: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  @Json(name = "is_featured") val isFeatured: Boolean? = false,
  @Json(name = "is_active") val isActive: Boolean? = true,
  @Json(name = "created_by") val createdBy: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class DestinationAlias(
  val id: String? = null,
  @Json(name = "destination_id") val destinationId: String,
  val alias: String,
  val language: String? = null
)

@JsonClass(generateAdapter = true)
data class PublicTourDiscoveryItem(
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "destination_id") val destinationId: String,
  @Json(name = "destination_name") val destinationName: String? = null,
  @Json(name = "tour_title") val tourTitle: String? = null,
  @Json(name = "start_date") val startDate: String? = null,
  @Json(name = "end_date") val endDate: String? = null,
  val status: String? = "planning",
  @Json(name = "actual_expense_total") val actualExpenseTotal: Double? = null,
  @Json(name = "estimated_per_person") val estimatedPerPerson: Double? = null,
  @Json(name = "actual_per_person") val actualPerPerson: Double? = null,
  @Json(name = "accommodation_total") val accommodationTotal: Double? = null,
  @Json(name = "transport_total") val transportTotal: Double? = null,
  @Json(name = "food_total") val foodTotal: Double? = null,
  @Json(name = "other_total") val otherTotal: Double? = null,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Tour(
  val id: String = "",
  val title: String,
  val description: String? = null,
  @Json(name = "destination") val destinationName: String = "",
  @Json(name = "creator_id") val creatorId: String? = null,
  @Json(name = "start_date") val startDate: String? = null,
  @Json(name = "end_date") val endDate: String? = null,
  val status: String = "upcoming",
  @Json(name = "is_public") val isPublic: Boolean = false,
  @Json(name = "max_members") val maxMembers: Int? = 10,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
) {
  // Compatibility accessors for existing Compose screens.
  val ownerId: String? get() = creatorId
  val visibility: String get() = if (isPublic) "public" else "private"
  val destinationId: String? get() = null
  val meetingPoint: String? get() = null
}

@JsonClass(generateAdapter = true)
data class TourExpense(
  val id: String = "",
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "paid_by") val paidBy: String? = null,
  val title: String,
  val description: String? = null,
  val amount: Double,
  val category: String = "Other",
  @Json(name = "expense_date") val expenseDate: String? = null,
  @Json(name = "receipt_url") val receiptUrl: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TourItineraryItem(
  val id: String = "",
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "sort_order") val dayNumber: Int,
  val title: String,
  val description: String? = null,
  val location: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TourChecklistItem(
  val id: String = "",
  @Json(name = "tour_id") val tourId: String,
  val title: String,
  @Json(name = "status") val status: String = "todo",
  @Json(name = "created_at") val createdAt: String? = null
) {
  val isCompleted: Boolean get() = status == "done" || status == "completed"
}

@JsonClass(generateAdapter = true)
data class TourGuide(
  val id: String = "",
  @Json(name = "tour_id") val tourId: String? = null,
  @Json(name = "destination_id") val destinationId: String? = null,
  val name: String,
  val phone: String? = null,
  val email: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TourMember(
  val id: String = "",
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "user_id") val userId: String,
  val role: String = "member",
  @Json(name = "joined_at") val joinedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TourComment(
  val id: String = "",
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "user_id") val userId: String,
  val body: String,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class UserProfile(
  val id: String,
  @Json(name = "full_name") val fullName: String? = null,
  val username: String? = null,
  val email: String? = null,
  @Json(name = "avatar_url") val avatarUrl: String? = null,
  val bio: String? = null,
  @Json(name = "location") val city: String? = null,
  val country: String? = null,
  val phone: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AppNotification(
  val id: String = "",
  @Json(name = "user_id") val userId: String,
  @Json(name = "tour_id") val tourId: String? = null,
  val title: String,
  val body: String? = null,
  val type: String = "general",
  @Json(name = "read_at") val readAt: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
) {
  val isRead: Boolean get() = readAt != null
}
