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
  @Json(name = "id") val tourId: String,
  @Json(name = "destination") val destinationName: String? = null,
  @Json(name = "title") val tourTitle: String? = null,
  @Json(name = "start_date") val startDate: String? = null,
  @Json(name = "end_date") val endDate: String? = null,
  val status: String? = "upcoming",
  @Json(name = "budget_per_person") val estimatedPerPerson: Double? = null,
  val actualExpenseTotal: Double? = null,
  val actualPerPerson: Double? = null,
  val transportTotal: Double? = null,
  val accommodationTotal: Double? = null,
  val foodTotal: Double? = null,
  val otherTotal: Double? = null,
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
  @Json(name = "notes") val description: String? = null,
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
  @Json(name = "details") val description: String? = null,
  val location: String? = null,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "day_date") val dayDate: String? = null,
  @Json(name = "starts_at") val startsAt: String? = null,
  @Json(name = "ends_at") val endsAt: String? = null,
  @Json(name = "estimated_cost") val estimatedCost: Double? = null
)

@JsonClass(generateAdapter = true)
data class TourChecklistItem(
  val id: String = "",
  @Json(name = "tour_id") val tourId: String,
  val title: String,
  @Json(name = "status") val status: String = "todo",
  @Json(name = "created_at") val createdAt: String? = null,
  val details: String? = null,
  @Json(name = "assigned_to") val assignedTo: String? = null,
  @Json(name = "assigned_guest_id") val assignedGuestId: String? = null,
  @Json(name = "due_at") val dueAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
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
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "created_by") val createdBy: String? = null
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
  @Json(name = "sender_id") val userId: String,
  val body: String,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "content_type") val contentType: String? = null,
  @Json(name = "client_created_at") val clientCreatedAt: String? = null,
  @Json(name = "sender_guest_id") val senderGuestId: String? = null
)


@JsonClass(generateAdapter = true)
data class TourJoinRequest(
  val id: String,
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "requester_id") val requesterId: String,
  val message: String = "",
  val status: String = "pending",
  @Json(name = "reviewed_at") val reviewedAt: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TourGuestMember(
  val id: String,
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "guest_code") val guestCode: String,
  @Json(name = "display_name") val displayName: String,
  val status: String = "active",
  @Json(name = "created_by") val createdBy: String,
  @Json(name = "claimed_by") val claimedBy: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TourDocument(
  val id: String,
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "uploaded_by") val uploadedBy: String,
  @Json(name = "storage_path") val storagePath: String,
  @Json(name = "file_name") val fileName: String,
  @Json(name = "mime_type") val mimeType: String,
  @Json(name = "file_size") val fileSize: Long,
  val category: String = "other",
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TourImage(
  val id: String,
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "uploaded_by") val uploadedBy: String,
  @Json(name = "storage_path") val storagePath: String,
  val caption: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TourAttendance(
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "user_id") val userId: String,
  val response: String = "going",
  @Json(name = "responded_at") val respondedAt: String? = null,
  @Json(name = "checked_in_at") val checkedInAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)


@JsonClass(generateAdapter = true)
data class TourGuestAttendance(
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "guest_member_id") val guestMemberId: String,
  val response: String = "going",
  @Json(name = "responded_at") val respondedAt: String? = null,
  @Json(name = "checked_in_at") val checkedInAt: String? = null,
  @Json(name = "updated_by") val updatedBy: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TourMemberPermission(
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "user_id") val userId: String,
  @Json(name = "can_edit_tour") val canEditTour: Boolean = false,
  @Json(name = "can_manage_members") val canManageMembers: Boolean = false,
  @Json(name = "can_manage_content") val canManageContent: Boolean = false,
  @Json(name = "granted_by") val grantedBy: String,
  @Json(name = "created_at") val createdAt: String? = null,
  @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TourDevice(
  val id: String,
  @Json(name = "user_id") val userId: String,
  @Json(name = "device_label") val deviceLabel: String,
  val platform: String = "android",
  @Json(name = "revoked_at") val revokedAt: String? = null,
  @Json(name = "last_seen_at") val lastSeenAt: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TourSyncConflict(
  val id: String,
  @Json(name = "operation_id") val operationId: String,
  @Json(name = "tour_id") val tourId: String,
  @Json(name = "entity_type") val entityType: String,
  @Json(name = "entity_id") val entityId: String,
  @Json(name = "client_base_revision") val clientBaseRevision: String? = null,
  @Json(name = "server_revision") val serverRevision: String? = null,
  val reason: String,
  val resolution: String? = null,
  @Json(name = "resolved_by") val resolvedBy: String? = null,
  @Json(name = "resolved_at") val resolvedAt: String? = null,
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
  @Json(name = "actor_id") val actorId: String? = null,
  val title: String,
  val body: String? = null,
  val type: String = "general",
  @Json(name = "read_at") val readAt: String? = null,
  @Json(name = "created_at") val createdAt: String? = null
) {
  val isRead: Boolean get() = readAt != null
}
