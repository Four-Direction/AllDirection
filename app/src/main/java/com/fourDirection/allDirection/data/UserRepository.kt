package com.fourDirection.allDirection.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    suspend fun saveUserToFirestore(user: FirebaseUser, customName: String? = null, customPhotoUrl: String? = null) {
        try {
            val userData = mutableMapOf<String, Any>(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "lastLogin" to Timestamp.now()
            )

            customName?.let { 
                userData["name"] = it
                userData["name_lowercase"] = it.lowercase()
            } ?: run {
                userData["name"] = user.displayName ?: "User"
                userData["name_lowercase"] = (user.displayName ?: "User").lowercase()
            }

            if (customPhotoUrl != null) {
                userData["photoUrl"] = customPhotoUrl
            } else if (user.photoUrl != null) {
                userData["photoUrl"] = user.photoUrl.toString()
            }

            db.collection("users").document(user.uid).set(userData, SetOptions.merge()).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving user to Firestore", e)
            throw e
        }
    }

    suspend fun saveCustomCategory(uid: String, category: String) {
        try {
            db.collection("users").document(uid)
                .update("customCategories", FieldValue.arrayUnion(category))
                .await()
        } catch (e: Exception) {
            db.collection("users").document(uid)
                .set(mapOf("customCategories" to listOf(category)), SetOptions.merge())
                .await()
        }
    }

    suspend fun getCustomCategories(uid: String): List<String> {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            @Suppress("UNCHECKED_CAST")
            doc.get("customCategories") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateUserInfo(uid: String, name: String, photoUrl: String) {
        try {
            val updates = hashMapOf<String, Any>(
                "name" to name,
                "name_lowercase" to name.lowercase(),
                "photoUrl" to photoUrl
            )
            db.collection("users").document(uid).update(updates).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating user info", e)
            throw e
        }
    }

    fun encodeImageToBase64(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else ""
        } catch (e: Exception) {
            Log.e("UserRepository", "Error encoding image", e)
            ""
        }
    }

    suspend fun getUserName(uid: String): String? {
        return try {
            val document = db.collection("users").document(uid).get().await()
            document.getString("name")
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getUserTravelData(uid: String): Map<String, Any>? {
        return try {
            val document = db.collection("users").document(uid).get().await()
            document.data
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateLastLogin(uid: String) {
        try {
            db.collection("users").document(uid).update("lastLogin", Timestamp.now()).await()
        } catch (e: Exception) {
            // Log error
        }
    }

    // --- Trip Management ---

    suspend fun saveTrip(uid: String, trip: Trip) {
        try {
            val dayPlansData = trip.dayPlans.mapKeys { it.key.format(dateFormatter) }
                .mapValues { (_, plan) ->
                    mapOf(
                        "date" to plan.date.format(dateFormatter),
                        "description" to plan.description,
                        "locations" to plan.locations,
                        "events" to plan.events,
                        "hotel" to plan.hotel,
                        "startLocation" to plan.startLocation,
                        "endLocation" to plan.endLocation
                    )
                }

            val tripData = hashMapOf(
                "id" to trip.id,
                "name" to trip.name,
                "startDate" to trip.startDate.format(dateFormatter),
                "endDate" to trip.endDate.format(dateFormatter),
                "dayPlans" to dayPlansData
            )

            db.collection("users").document(uid)
                .collection("trips").document(trip.id)
                .set(tripData).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving trip", e)
            throw e
        }
    }

    @Suppress("unused")
    suspend fun getTrips(uid: String): List<Trip> {
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("trips").get().await()
            
            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: return@mapNotNull null
                val name = doc.getString("name") ?: ""
                val startStr = doc.getString("startDate") ?: return@mapNotNull null
                val endStr = doc.getString("endDate") ?: return@mapNotNull null
                
                val startDate = LocalDate.parse(startStr, dateFormatter)
                val endDate = LocalDate.parse(endStr, dateFormatter)
                
                @Suppress("UNCHECKED_CAST")
                val dayPlansRaw = doc.get("dayPlans") as? Map<String, Map<String, Any>> ?: emptyMap()
                
                val dayPlans = dayPlansRaw.mapValues { (dateStr, data) ->
                    val eventsList = data["events"] as? List<*>
                    val locationsList = data["locations"] as? List<*> ?: data["location"]?.let { listOf(it) } ?: emptyList<Any>()
                    DayPlan(
                        date = LocalDate.parse(dateStr, dateFormatter),
                        description = data["description"] as? String ?: "",
                        locations = locationsList.filterIsInstance<String>(),
                        events = eventsList?.filterIsInstance<String>() ?: emptyList(),
                        hotel = data["hotel"] as? String,
                        startLocation = data["startLocation"] as? String,
                        endLocation = data["endLocation"] as? String
                    )
                }.mapKeys { LocalDate.parse(it.key, dateFormatter) }

                Trip(id, name, startDate, endDate, dayPlans)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting trips", e)
            emptyList()
        }
    }

    suspend fun deleteTrip(uid: String, tripId: String) {
        try {
            db.collection("users").document(uid)
                .collection("trips").document(tripId)
                .delete().await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting trip", e)
            throw e
        }
    }

    // --- Connections Management ---

    suspend fun sendConnectionRequest(fromUid: String, fromName: String, toUid: String) {
        try {
            val batch = db.batch()
            
            val requestData = hashMapOf(
                "fromUid" to fromUid,
                "fromName" to fromName,
                "sentAt" to Timestamp.now()
            )
            val incomingRef = db.collection("users").document(toUid)
                .collection("connectionRequests").document(fromUid)
            batch.set(incomingRef, requestData)
            
            val outgoingRef = db.collection("users").document(fromUid)
                .collection("sentRequests").document(toUid)
            batch.set(outgoingRef, mapOf(
                "toUid" to toUid,
                "sentAt" to Timestamp.now()
            ))
            
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error sending request", e)
            throw e
        }
    }

    @Suppress("unused")
    suspend fun getConnectionRequests(uid: String): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("connectionRequests").get().await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun acceptConnectionRequest(uid: String, currentUserName: String, friendUid: String, friendName: String) {
        try {
            val batch = db.batch()
            
            val myConnRef = db.collection("users").document(uid)
                .collection("friends").document(friendUid)
            batch.set(myConnRef, mapOf("uid" to friendUid, "name" to friendName, "addedAt" to Timestamp.now()))
            
            val theirConnRef = db.collection("users").document(friendUid)
                .collection("friends").document(uid)
            batch.set(theirConnRef, mapOf("uid" to uid, "name" to currentUserName, "addedAt" to Timestamp.now()))
            
            val reqRef = db.collection("users").document(uid)
                .collection("connectionRequests").document(friendUid)
            batch.delete(reqRef)
            
            val theirSentRef = db.collection("users").document(friendUid)
                .collection("sentRequests").document(uid)
            batch.delete(theirSentRef)
            
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error accepting request", e)
            throw e
        }
    }

    suspend fun removeConnection(uid: String, friendUid: String) {
        try {
            val batch = db.batch()
            batch.delete(db.collection("users").document(uid).collection("friends").document(friendUid))
            batch.delete(db.collection("users").document(friendUid).collection("friends").document(uid))
            batch.commit().await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun searchUsersByName(query: String): List<Map<String, Any>> {
        val lowercaseQuery = query.lowercase()
        return try {
            val lowercaseSnapshot = db.collection("users")
                .whereGreaterThanOrEqualTo("name_lowercase", lowercaseQuery)
                .whereLessThanOrEqualTo("name_lowercase", lowercaseQuery + "\uf8ff")
                .limit(10)
                .get().await()
            
            val results = lowercaseSnapshot.documents.mapNotNull { it.data }.toMutableList()

            if (results.size < 5) {
                val legacySnapshot = db.collection("users")
                    .whereGreaterThanOrEqualTo("name", query)
                    .whereLessThanOrEqualTo("name", query + "\uf8ff")
                    .limit(10)
                    .get().await()
                
                legacySnapshot.documents.forEach { doc ->
                    val data = doc.data
                    if (data != null && results.none { it["uid"] == data["uid"] }) {
                        results.add(data)
                    }
                }
            }
            
            results.take(10)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error searching users", e)
            emptyList()
        }
    }

    @Suppress("unused")
    suspend fun addConnection(uid: String, connectionUid: String, connectionName: String) {
        try {
            val connectionData = hashMapOf(
                "uid" to connectionUid,
                "name" to connectionName,
                "addedAt" to Timestamp.now()
            )
            
            db.collection("users").document(uid)
                .collection("friends").document(connectionUid)
                .set(connectionData).await()
                
            val currentUserName = getUserName(uid) ?: "User"
            val meData = hashMapOf(
                "uid" to uid,
                "name" to currentUserName,
                "addedAt" to Timestamp.now()
            )
            db.collection("users").document(connectionUid)
                .collection("friends").document(uid)
                .set(meData).await()
                
        } catch (e: Exception) {
            Log.e("UserRepository", "Error adding connection", e)
            throw e
        }
    }

    @Suppress("unused")
    suspend fun getConnections(uid: String): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("friends")
                .orderBy("name")
                .get().await()
            
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting connections", e)
            emptyList()
        }
    }

    // --- Groups Management ---

    suspend fun createGroup(uid: String, groupName: String, members: List<Map<String, String>>) {
        try {
            val groupId = UUID.randomUUID().toString()
            val groupData = hashMapOf(
                "id" to groupId,
                "name" to groupName,
                "createdBy" to uid,
                "createdAt" to Timestamp.now(),
                "memberUids" to (members.map { it["uid"] ?: "" } + uid).distinct()
            )

            db.collection("groups").document(groupId).set(groupData).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error creating group", e)
            throw e
        }
    }

    @Suppress("unused")
    suspend fun getMyGroups(uid: String): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("groups")
                .whereArrayContains("memberUids", uid)
                .get().await()
            
            snapshot.documents.mapNotNull { doc ->
                val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                data["groupName"] = data["name"] // Mapping for UI consistency
                data
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting groups", e)
            emptyList()
        }
    }

    suspend fun deleteGroup(groupId: String) {
        try {
            db.collection("groups").document(groupId).delete().await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting group", e)
            throw e
        }
    }

    suspend fun leaveGroup(groupId: String, uid: String) {
        try {
            db.collection("groups").document(groupId)
                .update("memberUids", FieldValue.arrayRemove(uid))
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error leaving group", e)
            throw e
        }
    }

    suspend fun sendMessage(groupId: String, senderUid: String, senderName: String, text: String) {
        try {
            val messageData = hashMapOf(
                "senderUid" to senderUid,
                "senderName" to senderName,
                "text" to text,
                "timestamp" to Timestamp.now()
            )
            db.collection("groups").document(groupId)
                .collection("messages").add(messageData).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error sending message", e)
            throw e
        }
    }

    suspend fun updateGroupDescription(groupId: String, description: String) {
        try {
            db.collection("groups").document(groupId)
                .update("description", description)
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating group description", e)
            throw e
        }
    }

    suspend fun kickMember(groupId: String, memberUid: String) {
        try {
            db.collection("groups").document(groupId)
                .update("memberUids", FieldValue.arrayRemove(memberUid))
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error kicking member", e)
            throw e
        }
    }

    suspend fun updateGroupName(groupId: String, name: String) {
        try {
            db.collection("groups").document(groupId)
                .update("name", name)
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating group name", e)
            throw e
        }
    }

    suspend fun pinGroup(uid: String, groupId: String) {
        try {
            db.collection("users").document(uid)
                .update("pinnedGroups", FieldValue.arrayUnion(groupId))
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error pinning group", e)
            throw e
        }
    }

    suspend fun unpinGroup(uid: String, groupId: String) {
        try {
            db.collection("users").document(uid)
                .update("pinnedGroups", FieldValue.arrayRemove(groupId))
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error unpinning group", e)
            throw e
        }
    }

    // --- Budget Plan Management ---

    suspend fun saveBudgetPlan(uid: String, plan: BudgetPlan) {
        try {
            val budgetData = hashMapOf(
                "id" to plan.id,
                "tripId" to plan.tripId,
                "tripName" to plan.tripName,
                "startDate" to plan.startDate.format(dateFormatter),
                "endDate" to plan.endDate.format(dateFormatter),
                "baseCurrency" to plan.baseCurrency,
                "exchangeCurrency" to plan.exchangeCurrency,
                "isLocalTrip" to plan.isLocalTrip,
                "isGroup" to plan.isGroup,
                "groupType" to plan.groupType.name,
                "customCategories" to plan.customCategories,
                "individualBudgets" to plan.individualBudgets.map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "amount" to it.amount,
                        "isAmountInExchangeCurrency" to it.isAmountInExchangeCurrency
                    )
                },
                "expenses" to plan.expenses.map {
                    mapOf(
                        "id" to it.id,
                        "name" to it.name,
                        "amount" to it.amount,
                        "category" to it.category,
                        "individualId" to it.individualId
                    )
                }
            )

            db.collection("users").document(uid)
                .collection("budgetPlans").document(plan.id)
                .set(budgetData).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving budget plan", e)
            throw e
        }
    }

    suspend fun getBudgetPlans(uid: String): List<BudgetPlan> {
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("budgetPlans").get().await()

            snapshot.documents.mapNotNull { doc ->
                val id = doc.getString("id") ?: return@mapNotNull null
                val tripName = doc.getString("tripName") ?: ""
                val startDate = LocalDate.parse(doc.getString("startDate") ?: return@mapNotNull null, dateFormatter)
                val endDate = LocalDate.parse(doc.getString("endDate") ?: return@mapNotNull null, dateFormatter)

                @Suppress("UNCHECKED_CAST")
                val individualsRaw = doc.get("individualBudgets") as? List<Map<String, Any>> ?: emptyList()
                val individualBudgets = individualsRaw.mapIndexed { index, it ->
                    IndividualBudget(
                        id = it["id"] as? String ?: (if (it["name"] == "You") "main_user" else "ind_${index}"),
                        name = it["name"] as? String ?: "",
                        amount = (it["amount"] as? Number)?.toDouble() ?: 0.0,
                        isAmountInExchangeCurrency = it["isAmountInExchangeCurrency"] as? Boolean ?: false
                    )
                }

                @Suppress("UNCHECKED_CAST")
                val expensesRaw = doc.get("expenses") as? List<Map<String, Any>> ?: emptyList()
                val expenses = expensesRaw.map {
                    ExpenseItem(
                        id = it["id"] as? String ?: UUID.randomUUID().toString(),
                        name = it["name"] as? String ?: "",
                        amount = (it["amount"] as? Number)?.toDouble() ?: 0.0,
                        category = it["category"] as? String ?: "",
                        individualId = it["individualId"] as? String
                    )
                }

                @Suppress("UNCHECKED_CAST")
                val customCats = doc.get("customCategories") as? List<String> ?: emptyList()
                
                BudgetPlan(
                    id = id,
                    tripId = doc.getString("tripId"),
                    tripName = tripName,
                    startDate = startDate,
                    endDate = endDate,
                    baseCurrency = doc.getString("baseCurrency") ?: "USD",
                    exchangeCurrency = doc.getString("exchangeCurrency") ?: "USD",
                    isLocalTrip = doc.getBoolean("isLocalTrip") ?: false,
                    isGroup = doc.getBoolean("isGroup") ?: false,
                    groupType = GroupBudgetType.valueOf(doc.getString("groupType") ?: "INDIVIDUAL"),
                    individualBudgets = individualBudgets,
                    expenses = expenses,
                    customCategories = customCats
                )
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting budget plans", e)
            emptyList()
        }
    }

    suspend fun deleteBudgetPlan(uid: String, planId: String) {
        try {
            db.collection("users").document(uid)
                .collection("budgetPlans").document(planId)
                .delete().await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting budget plan", e)
            throw e
        }
    }

    // --- Transaction Management (Actual Spending) ---

    suspend fun saveTransaction(uid: String, tx: Transaction) {
        try {
            val data = hashMapOf(
                "id" to tx.id,
                "planId" to tx.planId,
                "name" to tx.name,
                "amount" to tx.amount,
                "category" to tx.category,
                "date" to tx.date.format(dateFormatter),
                "individualId" to tx.individualId
            )
            db.collection("users").document(uid)
                .collection("budgetPlans").document(tx.planId)
                .collection("transactions").document(tx.id)
                .set(data).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error saving transaction", e)
            throw e
        }
    }

    suspend fun getTransactions(uid: String, planId: String): List<Transaction> {
        return try {
            val snapshot = db.collection("users").document(uid)
                .collection("budgetPlans").document(planId)
                .collection("transactions").get().await()

            snapshot.documents.mapNotNull { doc ->
                Transaction(
                    id = doc.getString("id") ?: doc.id,
                    planId = doc.getString("planId") ?: planId,
                    name = doc.getString("name") ?: "",
                    amount = (doc.get("amount") as? Number)?.toDouble() ?: 0.0,
                    category = doc.getString("category") ?: "Other",
                    date = LocalDate.parse(doc.getString("date") ?: return@mapNotNull null, dateFormatter),
                    individualId = doc.getString("individualId")
                )
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting transactions", e)
            emptyList()
        }
    }

    suspend fun deleteTransaction(uid: String, planId: String, txId: String) {
        try {
            db.collection("users").document(uid)
                .collection("budgetPlans").document(planId)
                .collection("transactions").document(txId)
                .delete().await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error deleting transaction", e)
            throw e
        }
    }
}
