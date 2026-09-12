package com.fourDirection.allDirection.page.main

import android.util.Log
import java.util.UUID
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fourDirection.allDirection.data.Trip
import com.fourDirection.allDirection.data.UserRepository
import com.fourDirection.allDirection.ui.theme.GlowBlue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GroupChatViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUid get() = auth.currentUser?.uid

    private val _messages = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _groupMetadata = MutableStateFlow<Map<String, Any>?>(null)
    val groupMetadata = _groupMetadata.asStateFlow()

    private val _memberNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val memberNames = _memberNames.asStateFlow()

    private val _connections = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val connections = _connections.asStateFlow()

    private val _userTrips = MutableStateFlow<List<Trip>>(emptyList())
    val userTrips = _userTrips.asStateFlow()

    private val _viewingTrip = MutableStateFlow<Trip?>(null)
    val viewingTrip = _viewingTrip.asStateFlow()

    private val _currentUserName = MutableStateFlow("User")
    
    private var messagesListener: ListenerRegistration? = null
    private var groupListener: ListenerRegistration? = null
    private var groupId: String? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            groupId?.let { gid ->
                startListeners(gid)
                fetchCurrentUserName(uid)
            }
        } else {
            stopListeners()
            _messages.value = emptyList()
            _groupMetadata.value = null
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun init(groupId: String) {
        val uid = auth.currentUser?.uid
        if (this.groupId == groupId) {
            // Even if groupId is same, if name is not fetched, fetch it
            if (uid != null && _currentUserName.value == "User") {
                fetchCurrentUserName(uid)
            }
            return
        }
        
        this.groupId = groupId
        if (uid != null) {
            startListeners(groupId)
            fetchCurrentUserName(uid)
        }
    }

    private fun startListeners(groupId: String) {
        stopListeners()
        
        // Listen to group metadata
        groupListener = db.collection("groups").document(groupId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("GroupChatViewModel", "Error listening to group metadata", e)
                    return@addSnapshotListener
                }
                val data = snapshot?.data
                _groupMetadata.value = data
                
                // Fetch member names
                val uids = data?.get("memberUids") as? List<*>
                uids?.filterIsInstance<String>()?.let { fetchMemberNames(it) }
            }

        // Listen to messages
        messagesListener = db.collection("groups").document(groupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("GroupChatViewModel", "Error listening to messages", e)
                    return@addSnapshotListener
                }
                _messages.value = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            }
    }

    private fun fetchMemberNames(uids: List<String>) {
        viewModelScope.launch {
            val names = mutableMapOf<String, String>()
            uids.forEach { uid ->
                val name = userRepository.getUserName(uid) ?: "Unknown"
                names[uid] = name
            }
            _memberNames.value = names
        }
    }

    private fun fetchCurrentUserName(uid: String) {
        viewModelScope.launch {
            _currentUserName.value = userRepository.getUserName(uid) ?: "User"
        }
    }

    fun sendMessage(text: String) {
        val uid = auth.currentUser?.uid ?: return
        val gid = groupId ?: return
        if (text.isBlank()) return
        
        viewModelScope.launch {
            try {
                userRepository.sendMessage(gid, uid, _currentUserName.value, text)
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Error sending message", e)
            }
        }
    }

    fun shareTrip(trip: Trip) {
        val uid = auth.currentUser?.uid ?: return
        val gid = groupId ?: return
        
        viewModelScope.launch {
            try {
                userRepository.sendTripMessage(gid, uid, _currentUserName.value, trip.id, trip.name)
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Error sharing trip", e)
            }
        }
    }

    fun fetchUserTrips() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _userTrips.value = userRepository.getTrips(uid)
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Error fetching user trips", e)
            }
        }
    }

    fun fetchTripDetail(tripId: String) {
        viewModelScope.launch {
            _viewingTrip.value = userRepository.getTripById(tripId)
        }
    }

    fun joinSharedTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                userRepository.joinTrip(tripId, uid)
                // Refresh detail to show joined state
                fetchTripDetail(tripId)
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Error joining shared trip", e)
            }
        }
    }

    fun leaveSharedTrip(tripId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                userRepository.leaveTrip(tripId, uid)
                fetchTripDetail(tripId)
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Error leaving shared trip", e)
            }
        }
    }

    fun fetchConnections() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(uid)
                    .collection("friends")
                    .orderBy("name")
                    .get().await()
                _connections.value = snapshot.documents.mapNotNull { it.data }
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Error fetching connections", e)
            }
        }
    }

    fun addMemberToGroup(memberUid: String) {
        val gid = groupId ?: return
        viewModelScope.launch {
            try {
                userRepository.addMemberToGroup(gid, memberUid)
                Log.d("GroupChatViewModel", "Added member: $memberUid")
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Error adding member", e)
            }
        }
    }

    fun updateDescription(description: String) {
        val gid = groupId ?: return
        viewModelScope.launch {
            try {
                userRepository.updateGroupDescription(gid, description)
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Error updating description", e)
            }
        }
    }

    fun updateName(name: String) {
        val gid = groupId ?: return
        viewModelScope.launch {
            try {
                userRepository.updateGroupName(gid, name)
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Error updating group name", e)
            }
        }
    }

    fun kickMember(memberUid: String) {
        val gid = groupId ?: return
        viewModelScope.launch {
            try {
                userRepository.kickMember(gid, memberUid)
            } catch (e: Exception) {
                Log.e("GroupChatViewModel", "Error kicking member", e)
            }
        }
    }

    private fun stopListeners() {
        messagesListener?.remove()
        groupListener?.remove()
        messagesListener = null
        groupListener = null
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        stopListeners()
        super.onCleared()
    }
    
    fun isMe(senderUid: Any?): Boolean {
        return senderUid == currentUid
    }

    fun isCreator(): Boolean {
        return _groupMetadata.value?.get("createdBy") == currentUid
    }
}

@Composable
fun GroupChatPage(
    groupId: String,
    groupName: String,
    onDismiss: () -> Unit,
    viewModel: GroupChatViewModel = viewModel()
) {
    var messageText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val groupMetadata by viewModel.groupMetadata.collectAsState()
    val listState = rememberLazyListState()
    var showDetails by remember { mutableStateOf(false) }
    var showTripSelector by remember { mutableStateOf(false) }
    var viewingTripId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(groupId) {
        viewModel.init(groupId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding() // Adjust for keyboard
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDetails = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = groupMetadata?.get("name") as? String ?: groupName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Group Chat • Tap for details",
                        color = GlowBlue,
                        fontSize = 12.sp
                    )
                }
            }

            // Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    val isMe = viewModel.isMe(message["senderUid"])
                    MessageBubble(
                        text = message["text"] as? String ?: "",
                        senderName = message["senderName"] as? String ?: "Unknown",
                        isMe = isMe,
                        tripId = message["tripId"] as? String,
                        tripName = message["tripName"] as? String,
                        onTripClick = { tripId -> viewingTripId = tripId }
                    )
                }
            }

            // Input Field
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                color = Color.White.copy(alpha = 0.05f),
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { 
                        viewModel.fetchUserTrips()
                        showTripSelector = true 
                    }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Share Trip", tint = GlowBlue)
                    }
                    
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...", color = Color.White.copy(alpha = 0.4f)) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            viewModel.sendMessage(messageText)
                            messageText = ""
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) GlowBlue else Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }

    if (showTripSelector) {
        TripSelectorDialog(
            viewModel = viewModel,
            onDismiss = { showTripSelector = false },
            onTripSelected = { trip ->
                viewModel.shareTrip(trip)
                showTripSelector = false
            }
        )
    }

    if (viewingTripId != null) {
        SharedTripViewDialog(
            tripId = viewingTripId!!,
            viewModel = viewModel,
            onDismiss = { 
                viewingTripId = null
                // Clear the viewing trip in VM too
                viewModel.fetchTripDetail("") // Effectively clear
            }
        )
    }

    if (showDetails) {
        GroupDetailsPage(
            viewModel = viewModel,
            onDismiss = { showDetails = false }
        )
    }
}

@Composable
fun GroupDetailsPage(
    viewModel: GroupChatViewModel,
    onDismiss: () -> Unit
) {
    val groupMetadata by viewModel.groupMetadata.collectAsState()
    val memberNames by viewModel.memberNames.collectAsState()
    val isCreator = viewModel.isCreator()
    
    var description by remember(groupMetadata) { 
        mutableStateOf(groupMetadata?.get("description") as? String ?: "") 
    }
    var isEditingDescription by remember { mutableStateOf(false) }

    var groupNameInput by remember(groupMetadata) {
        mutableStateOf(groupMetadata?.get("name") as? String ?: "")
    }
    var isEditingName by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "Group Details",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Group Name & Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GlowBlue.copy(alpha = 0.1f))
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = GlowBlue, modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditingName) {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GlowBlue
                    ),
                    trailingIcon = {
                        IconButton(onClick = { 
                            if (groupNameInput.isNotBlank()) {
                                viewModel.updateName(groupNameInput)
                                isEditingName = false
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = GlowBlue)
                        }
                    },
                    singleLine = true
                )
            } else {
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = groupMetadata?.get("name") as? String ?: "",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isCreator) {
                        IconButton(onClick = { isEditingName = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = GlowBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Description Section
            Text("Description", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isEditingDescription) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GlowBlue
                    ),
                    trailingIcon = {
                        IconButton(onClick = { 
                            viewModel.updateDescription(description)
                            isEditingDescription = false 
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = GlowBlue)
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (description.isBlank()) "No description provided." else description,
                        color = Color.White,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (isCreator) {
                        IconButton(onClick = { isEditingDescription = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = GlowBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Members Section
            Text(
                "Members (${(groupMetadata?.get("memberUids") as? List<*>)?.size ?: 0})",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isCreator) {
                var showAddMemberDialog by remember { mutableStateOf(false) }
                
                Button(
                    onClick = { 
                        viewModel.fetchConnections()
                        showAddMemberDialog = true 
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlowBlue.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, GlowBlue.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = GlowBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Member", color = GlowBlue)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (showAddMemberDialog) {
                    val memberUids = groupMetadata?.get("memberUids") as? List<*> ?: emptyList<Any>()
                    AddMemberDialog(
                        viewModel = viewModel,
                        currentMemberUids = memberUids.filterIsInstance<String>(),
                        onDismiss = { showAddMemberDialog = false }
                    )
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                val memberUids = groupMetadata?.get("memberUids") as? List<*> ?: emptyList<Any>()
                items(memberUids) { uid ->
                    val name = memberNames[uid] ?: "Loading..."
                    val isMemberCreator = uid == groupMetadata?.get("createdBy")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(name.take(1).uppercase(), color = Color.White, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = name, color = Color.White)
                            if (isMemberCreator) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = GlowBlue.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        "Owner",
                                        color = GlowBlue,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (isCreator && !isMemberCreator) {
                            IconButton(onClick = { viewModel.kickMember(uid as String) }) {
                                Icon(Icons.Default.PersonRemove, contentDescription = "Kick", tint = Color.Red.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemberDialog(
    viewModel: GroupChatViewModel,
    currentMemberUids: List<String>,
    onDismiss: () -> Unit
) {
    val connections by viewModel.connections.collectAsState()
    val availableConnections = connections.filter { !currentMemberUids.contains(it["uid"] as String) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1A),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Member", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (availableConnections.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No connections to add.", color = Color.White.copy(alpha = 0.4f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(availableConnections) { connection ->
                            val uid = connection["uid"] as String
                            val name = connection["name"] as String

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        viewModel.addMemberToGroup(uid)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GlowBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(name.take(1).uppercase(), color = GlowBlue)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(name, color = Color.White)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Add, contentDescription = null, tint = GlowBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SharedTripViewDialog(
    tripId: String,
    viewModel: GroupChatViewModel,
    onDismiss: () -> Unit
) {
    val trip by viewModel.viewingTrip.collectAsState()
    val currentUid = FirebaseAuth.getInstance().currentUser?.uid
    
    LaunchedEffect(tripId) {
        viewModel.fetchTripDetail(tripId)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF121212),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            if (trip == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GlowBlue)
                }
            } else {
                val t = trip!!
                val isOwner = t.ownerUid == currentUid
                val isCollaborator = t.collaboratorUids.contains(currentUid)
                val canJoin = !isOwner && !isCollaborator

                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(t.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("${t.startDate} - ${t.endDate}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (canJoin) {
                        Button(
                            onClick = { viewModel.joinSharedTrip(t.id) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GlowBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add to My Calendar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = GlowBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = GlowBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("In your calendar", color = GlowBlue, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                            
                            if (!isOwner) {
                                Button(
                                    onClick = { viewModel.leaveSharedTrip(t.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove Plan", tint = Color.Red)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Text("Itinerary", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        val days = t.dayPlans.keys.sorted()
                        items(days) { date ->
                            val plan = t.dayPlans[date]!!
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = date.toString(),
                                        color = GlowBlue,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (plan.description.isNotBlank()) {
                                        Text(plan.description, color = Color.White, fontSize = 14.sp)
                                    }
                                    if (plan.locations.isNotEmpty()) {
                                        Text(
                                            "Stops: ${plan.locations.joinToString(", ")}",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TripSelectorDialog(
    viewModel: GroupChatViewModel,
    onDismiss: () -> Unit,
    onTripSelected: (Trip) -> Unit
) {
    val trips by viewModel.userTrips.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1A),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select a Trip to Share", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (trips.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No trips found in your calendar.", color = Color.White.copy(alpha = 0.4f))
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(trips) { trip ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable { onTripSelected(trip) },
                                color = Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = GlowBlue)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(trip.name, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("${trip.startDate} - ${trip.endDate}", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    text: String,
    senderName: String,
    isMe: Boolean,
    tripId: String? = null,
    tripName: String? = null,
    onTripClick: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(
                text = senderName,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }
        Surface(
            color = if (isMe) GlowBlue else Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 0.dp,
                bottomEnd = if (isMe) 0.dp else 16.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (tripId != null) {
                    TripCard(
                        tripName = tripName ?: "Trip Plan",
                        isMe = isMe,
                        onClick = { onTripClick(tripId) }
                    )
                    if (text.isNotBlank()) Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (text.isNotBlank()) {
                    Text(
                        text = text,
                        color = if (isMe) Color.Black else Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TripCard(
    tripName: String,
    isMe: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isMe) Color.Black.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isMe) Color.Black.copy(alpha = 0.2f) else GlowBlue.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(0.7f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.FlightTakeoff,
                contentDescription = null,
                tint = if (isMe) Color.Black else GlowBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tripName,
                    color = if (isMe) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Tap to view plan",
                    color = if (isMe) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isMe) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
