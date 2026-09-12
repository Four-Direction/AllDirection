package com.fourDirection.allDirection.page.main

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fourDirection.allDirection.data.UserRepository
import com.fourDirection.allDirection.ui.theme.GlowBlue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SocialViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUid get() = auth.currentUser?.uid

    private val _connections = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val connections = _connections.asStateFlow()

    private val _requests = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val requests = _requests.asStateFlow()

    private val _sentRequestUids = MutableStateFlow<Set<String>>(emptySet())
    val sentRequestUids = _sentRequestUids.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _myGroups = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val myGroups = _myGroups.asStateFlow()

    private val _pinnedGroups = MutableStateFlow<List<String>>(emptyList())
    val pinnedGroups = _pinnedGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _currentUserName = MutableStateFlow("User")
    
    private var connectionsListener: ListenerRegistration? = null
    private var requestsListener: ListenerRegistration? = null
    private var sentRequestsListener: ListenerRegistration? = null
    private var groupsListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            startRealtimeListeners(uid)
            fetchCurrentUserName(uid)
        } else {
            stopRealtimeListeners()
            _connections.value = emptyList()
            _requests.value = emptyList()
            _sentRequestUids.value = emptySet()
            _searchResults.value = emptyList()
            _myGroups.value = emptyList()
            _pinnedGroups.value = emptyList()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    private fun startRealtimeListeners(uid: String) {
        stopRealtimeListeners()

        // 1. Listen for Connections
        connectionsListener = db.collection("users").document(uid)
            .collection("friends")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SocialViewModel", "Friends listener error", e)
                    return@addSnapshotListener
                }
                val friendList = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    data["uid"] = doc.id
                    data["name"] = data["name"] as? String ?: "Unknown"
                    data
                } ?: emptyList()

                Log.d("SocialViewModel", "Fetched ${friendList.size} friends")
                _connections.value = friendList.sortedBy { (it["name"] as? String ?: "").lowercase() }
            }

        // 2. Listen for Incoming Requests
        requestsListener = db.collection("users").document(uid)
            .collection("connectionRequests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SocialViewModel", "Requests listener error", e)
                    return@addSnapshotListener
                }
                _requests.value = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    data["fromUid"] = doc.id // Ensure ID is mapped if missing
                    data
                } ?: emptyList()
            }

        // 3. Listen for Outgoing Requests
        sentRequestsListener = db.collection("users").document(uid)
            .collection("sentRequests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SocialViewModel", "Sent requests listener error", e)
                    return@addSnapshotListener
                }
                _sentRequestUids.value = snapshot?.documents?.mapNotNull { it.id }?.toSet() ?: emptySet()
            }

        // 4. Listen for Groups I am a member of
        groupsListener = db.collection("groups")
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SocialViewModel", "Groups listener error", e)
                    return@addSnapshotListener
                }
                _myGroups.value = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    data["id"] = doc.id
                    data["groupName"] = data["name"] as? String ?: "Unnamed Group"
                    data
                } ?: emptyList()
            }

        // 5. Listen for User Metadata (Pinned Groups)
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SocialViewModel", "User metadata listener error", e)
                    return@addSnapshotListener
                }
                @Suppress("UNCHECKED_CAST")
                _pinnedGroups.value = snapshot?.get("pinnedGroups") as? List<String> ?: emptyList()
            }
    }

    private fun stopRealtimeListeners() {
        connectionsListener?.remove()
        requestsListener?.remove()
        sentRequestsListener?.remove()
        groupsListener?.remove()
        userListener?.remove()
        connectionsListener = null
        requestsListener = null
        sentRequestsListener = null
        groupsListener = null
        userListener = null
    }

    private fun fetchCurrentUserName(uid: String) {
        viewModelScope.launch {
            _currentUserName.value = userRepository.getUserName(uid) ?: "User"
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = userRepository.searchUsersByName(query)
                .filter { it["uid"] != currentUid }
        }
    }

    fun sendRequest(toUid: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            try {
                userRepository.sendConnectionRequest(uid, _currentUserName.value, toUid)
                Log.d("SocialViewModel", "Request sent successfully to $toUid")
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error sending request", e)
            }
        }
    }

    fun acceptRequest(friendUid: String, friendName: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            try {
                userRepository.acceptConnectionRequest(uid, _currentUserName.value, friendUid, friendName)
                Log.d("SocialViewModel", "Request accepted: $friendUid")
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error accepting request", e)
            }
        }
    }

    fun removeConnection(friendUid: String) {
        val uid = currentUid ?: return
        Log.d("SocialViewModel", "removeConnection: friendUid=$friendUid")
        viewModelScope.launch {
            try {
                userRepository.removeConnection(uid, friendUid)
                Log.d("SocialViewModel", "Connection removed successfully")
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error removing connection", e)
            }
        }
    }

    fun createGroup(name: String, selectedMembers: List<Map<String, String>>) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.createGroup(uid, name, selectedMembers)
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error creating group", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            try {
                userRepository.deleteGroup(groupId)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    fun leaveGroup(groupId: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            try {
                // Also unpin if leaving
                if (_pinnedGroups.value.contains(groupId)) {
                    userRepository.unpinGroup(uid, groupId)
                }
                userRepository.leaveGroup(groupId, uid)
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    fun togglePinGroup(groupId: String) {
        val uid = currentUid ?: return
        val currentlyPinned = _pinnedGroups.value

        viewModelScope.launch {
            try {
                if (currentlyPinned.contains(groupId)) {
                    userRepository.unpinGroup(uid, groupId)
                } else {
                    if (currentlyPinned.size >= 3) {
                        // Could show toast here, but simple return for now
                        return@launch
                    }
                    userRepository.pinGroup(uid, groupId)
                }
            } catch (e: Exception) {
                Log.e("SocialViewModel", "Error toggling group pin", e)
            }
        }
    }

    fun isCreator(group: Map<String, Any>): Boolean {
        return group["createdBy"] == currentUid
    }
}

@Composable
fun SocialPage(onGroupClick: (Map<String, Any>) -> Unit = {}) {
    val viewModel: SocialViewModel = viewModel()
    var selectedTab by remember { mutableStateOf(0) } // 0 for Groups, 1 for Community
    val isLoading by viewModel.isLoading.collectAsState()
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Header
                Text(
                    text = if (selectedTab == 0) "Groups" else "Community",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(24.dp)
                )

                // Tab Switcher with Animation
                var groupsTabSize by remember { mutableStateOf(IntSize.Zero) }
                var communityTabSize by remember { mutableStateOf(IntSize.Zero) }
                val density = LocalDensity.current
                
                val springSpec = spring<Float>(
                    dampingRatio = 0.8f,
                    stiffness = 400f
                )
                
                val indicatorOffset by animateFloatAsState(
                    targetValue = if (selectedTab == 0) 0f else with(density) { groupsTabSize.width.toDp().toPx() },
                    animationSpec = springSpec,
                    label = "indicatorOffset"
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                        .clip(CircleShape),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Box(modifier = Modifier.padding(4.dp)) {
                        // Background Indicator
                        if (groupsTabSize.width > 0 && communityTabSize.width > 0) {
                            val currentWidth = if (selectedTab == 0) groupsTabSize.width else communityTabSize.width
                            val indicatorWidth by animateFloatAsState(
                                targetValue = with(density) { currentWidth.toDp().toPx() },
                                animationSpec = springSpec,
                                label = "indicatorWidth"
                            )

                            Box(
                                modifier = Modifier
                                    .offset(x = with(density) { indicatorOffset.toDp() })
                                    .size(
                                        width = with(density) { indicatorWidth.toDp() },
                                        height = 36.dp
                                    )
                                    .background(GlowBlue, CircleShape)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SubTabItem(
                                label = "Groups",
                                isSelected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                modifier = Modifier.onGloballyPositioned { groupsTabSize = it.size }
                            )
                            SubTabItem(
                                label = "Community",
                                isSelected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                modifier = Modifier.onGloballyPositioned { communityTabSize = it.size }
                            )
                        }
                    }
                }

                if (selectedTab == 0) {
                    GroupList(viewModel, onGroupClick)
                } else {
                    CommunityView(viewModel)
                }
            }

            // Create Group FAB
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 24.dp, bottom = 120.dp),
                    containerColor = GlowBlue,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
                }
            }
        }
    }

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            viewModel = viewModel,
            onDismiss = { showCreateGroupDialog = false }
        )
    }
}

@Composable
fun CommunityView(viewModel: SocialViewModel) {
    val context = LocalContext.current
    val connections by viewModel.connections.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val sentRequestUids by viewModel.sentRequestUids.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    
    var selectedConnection by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showOptionsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Search Toggle & Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isSearching) "Search Users" else "My Connections",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { isSearching = !isSearching }) {
                Icon(
                    imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = null,
                    tint = GlowBlue
                )
            }
        }

        if (isSearching) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchUsers(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Find travelers...", color = Color.White.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = GlowBlue,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(searchResults) { user ->
                    val uid = user["uid"] as String
                    val isRequested = sentRequestUids.contains(uid)
                    val isAlreadyFriend = connections.any { it["uid"] == uid }
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = user["name"] as? String ?: "Unknown", color = Color.White)
                            
                            if (isAlreadyFriend) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = GlowBlue)
                            } else if (isRequested) {
                                Text("Requested", color = GlowBlue, fontSize = 12.sp)
                            } else {
                                Button(
                                    onClick = { 
                                        viewModel.sendRequest(uid)
                                        Toast.makeText(context, "Connection request sent!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GlowBlue),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                ) {
                                    Text("Add", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Pending Requests
            if (requests.isNotEmpty()) {
                Text("Requests", color = GlowBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                requests.forEach { req ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        color = GlowBlue.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = req["fromName"] as? String ?: "Unknown", color = Color.White)
                            Button(
                                onClick = { 
                                    viewModel.acceptRequest(req["fromUid"] as String, req["fromName"] as String)
                                    Toast.makeText(context, "Connection accepted!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GlowBlue),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Accept", color = Color.Black, fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Friends List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (connections.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("No connections yet.", color = Color.White.copy(alpha = 0.3f))
                        }
                    }
                } else {
                    items(connections) { friend ->
                        Surface(
                            onClick = { 
                                selectedConnection = friend
                                showOptionsDialog = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = (friend["name"] as? String ?: "U").take(1), color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(text = friend["name"] as? String ?: "Unknown", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOptionsDialog && selectedConnection != null) {
        Dialog(onDismissRequest = { showOptionsDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1A1A1A),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = selectedConnection!!["name"] as String, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { 
                            viewModel.removeConnection(selectedConnection!!["uid"] as String)
                            Toast.makeText(context, "Connection removed", Toast.LENGTH_SHORT).show()
                            showOptionsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                    ) {
                        Text("Remove Connection", color = Color.Red)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showOptionsDialog = false }) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun GroupList(viewModel: SocialViewModel, onGroupClick: (Map<String, Any>) -> Unit) {
    val groups by viewModel.myGroups.collectAsState()
    val pinnedGroupIds by viewModel.pinnedGroups.collectAsState()
    var selectedGroupForOptions by remember { mutableStateOf<Map<String, Any>?>(null) }
    
    // Sort groups: Pinned first, then by name
    val sortedGroups = remember(groups, pinnedGroupIds) {
        groups.sortedWith(compareByDescending<Map<String, Any>> { pinnedGroupIds.contains(it["id"]) }
            .thenBy { it["groupName"] as? String ?: "" })
    }

    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("You haven't joined any groups yet.", color = Color.White.copy(alpha = 0.4f))
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sortedGroups) { group ->
                val isPinned = pinnedGroupIds.contains(group["id"])
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupClick(group) },
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = 0.5.dp, 
                        color = if (isPinned) GlowBlue.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(GlowBlue.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Groups, contentDescription = null, tint = GlowBlue)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = group["groupName"] as? String ?: "Unnamed Group",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isPinned) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            Icons.Default.PushPin,
                                            contentDescription = "Pinned",
                                            tint = GlowBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        IconButton(onClick = { selectedGroupForOptions = group }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }

    selectedGroupForOptions?.let { group ->
        GroupOptionsDialog(
            groupName = group["groupName"] as? String ?: "Group",
            isCreator = viewModel.isCreator(group),
            isPinned = pinnedGroupIds.contains(group["id"]),
            canPinMore = pinnedGroupIds.size < 3,
            onDismiss = { selectedGroupForOptions = null },
            onDelete = {
                viewModel.deleteGroup(group["id"] as String)
                selectedGroupForOptions = null
            },
            onLeave = {
                viewModel.leaveGroup(group["id"] as String)
                selectedGroupForOptions = null
            },
            onTogglePin = {
                viewModel.togglePinGroup(group["id"] as String)
                selectedGroupForOptions = null
            }
        )
    }
}

@Composable
fun GroupOptionsDialog(
    groupName: String,
    isCreator: Boolean,
    isPinned: Boolean,
    canPinMore: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onLeave: () -> Unit,
    onTogglePin: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showLeaveConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete '$groupName'? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.7f)
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Leave Group") },
            text = { Text("Are you sure you want to leave '$groupName'?") },
            confirmButton = {
                TextButton(onClick = onLeave, colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1A1A1A),
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.7f)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = groupName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                val isLimitReached = !isPinned && !canPinMore
                Button(
                    onClick = {
                        if (isLimitReached) {
                            Toast.makeText(context, "You can only pin 3 groups", Toast.LENGTH_SHORT).show()
                        } else {
                            onTogglePin()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLimitReached) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.1f),
                        contentColor = if (isLimitReached) Color.Gray else Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = null,
                        tint = when {
                            isPinned -> GlowBlue
                            isLimitReached -> Color.Gray.copy(alpha = 0.6f)
                            else -> Color.White
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPinned) "Unpin Group" else "Pin Group",
                        color = if (isLimitReached) Color.Gray.copy(alpha = 0.6f) else Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                if (isCreator) {
                    Button(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                    ) {
                        Text("Delete Group", color = Color.Red)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                Button(
                    onClick = { showLeaveConfirm = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                ) {
                    Text(if (isCreator) "Leave Group (Owner)" else "Leave Group", color = Color.Red)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun CreateGroupDialog(
    viewModel: SocialViewModel,
    onDismiss: () -> Unit
) {
    val connections by viewModel.connections.collectAsState()
    var groupName by remember { mutableStateOf("") }
    val selectedUids = remember { mutableStateListOf<String>() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Create New Group", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GlowBlue
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Add Connections", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(connections) { connection ->
                        val uid = connection["uid"] as? String ?: return@items
                        val name = connection["name"] as? String ?: "Unknown"
                        val isSelected = selectedUids.contains(uid)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (isSelected) selectedUids.remove(uid) else selectedUids.add(uid)
                                },
                            color = if (isSelected) GlowBlue.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { if (it) selectedUids.add(uid) else selectedUids.remove(uid) },
                                    colors = CheckboxDefaults.colors(checkedColor = GlowBlue)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = name, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val members = connections
                            .filter { selectedUids.contains(it["uid"] as? String ?: "") }
                            .map { mapOf("uid" to (it["uid"] as String), "name" to (it["name"] as? String ?: "Unknown")) }
                        viewModel.createGroup(groupName, members)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlowBlue),
                    enabled = groupName.isNotBlank() && selectedUids.isNotEmpty()
                ) {
                    Text("Start Planning", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
