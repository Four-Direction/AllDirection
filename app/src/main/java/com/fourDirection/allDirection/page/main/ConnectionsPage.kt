package com.fourDirection.allDirection.page.main

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.fourDirection.allDirection.data.UserRepository
import com.fourDirection.allDirection.ui.theme.GlowBlue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionsViewModel : ViewModel() {
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

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _currentUserName = MutableStateFlow("User")
    
    private var connectionsListener: ListenerRegistration? = null
    private var requestsListener: ListenerRegistration? = null
    private var sentRequestsListener: ListenerRegistration? = null

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
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    private fun startRealtimeListeners(uid: String) {
        Log.d("ConnectionsViewModel", "Starting real-time listeners for $uid")

        // Clear old listeners if any
        stopRealtimeListeners()

        // 1. Listen for Connections
        connectionsListener = db.collection("users").document(uid)
            .collection("friends")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ConnectionsViewModel", "Connections listener error", e)
                    return@addSnapshotListener
                }
                val friendList = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    data["uid"] = doc.id
                    data["name"] = data["name"] as? String ?: "Unknown"
                    data
                } ?: emptyList()

                Log.d("ConnectionsViewModel", "Fetched ${friendList.size} friends")
                _connections.value = friendList.sortedBy { (it["name"] as? String ?: "").lowercase() }
            }

        // 2. Listen for Incoming Requests
        requestsListener = db.collection("users").document(uid)
            .collection("connectionRequests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ConnectionsViewModel", "Requests listener error", e)
                    return@addSnapshotListener
                }
                _requests.value = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    data["fromUid"] = doc.id
                    data
                } ?: emptyList()
            }

        // 3. Listen for Outgoing Requests (to show "Requested" status persistently)
        sentRequestsListener = db.collection("users").document(uid)
            .collection("sentRequests")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ConnectionsViewModel", "Sent requests listener error", e)
                    return@addSnapshotListener
                }
                _sentRequestUids.value = snapshot?.documents?.mapNotNull { it.id }?.toSet() ?: emptySet()
            }
    }

    private fun stopRealtimeListeners() {
        connectionsListener?.remove()
        requestsListener?.remove()
        sentRequestsListener?.remove()
        connectionsListener = null
        requestsListener = null
        sentRequestsListener = null
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        stopRealtimeListeners()
        super.onCleared()
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
                _sentRequestUids.value = _sentRequestUids.value + toUid
            } catch (e: Exception) {
                Log.e("ConnectionsViewModel", "Error sending request", e)
            }
        }
    }

    fun acceptRequest(friendUid: String, friendName: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            try {
                userRepository.acceptConnectionRequest(uid, _currentUserName.value, friendUid, friendName)
                Log.d("ConnectionsViewModel", "Request accepted from $friendUid")
            } catch (e: Exception) {
                Log.e("ConnectionsViewModel", "Error accepting request", e)
            }
        }
    }

    fun removeConnection(friendUid: String) {
        val uid = currentUid ?: return
        Log.d("ConnectionsViewModel", "removeConnection: friendUid=$friendUid")
        viewModelScope.launch {
            try {
                userRepository.removeConnection(uid, friendUid)
                Log.d("ConnectionsViewModel", "Successfully removed connection")
                // No need to manually update state, the listener will handle it
            } catch (e: Exception) {
                Log.e("ConnectionsViewModel", "Error removing connection", e)
            }
        }
    }
}

@Composable
fun ConnectionsPage(
    onDismiss: () -> Unit,
    viewModel: ConnectionsViewModel = viewModel()
) {
    val context = LocalContext.current
    val connections by viewModel.connections.collectAsState()
    val requests by viewModel.requests.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val sentRequestUids by viewModel.sentRequestUids.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    
    var selectedConnection by remember { mutableStateOf<Map<String, Any>?>(null) }
    var showOptionsDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connections",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = { isSearching = !isSearching }) {
                    Icon(
                        imageVector = if (isSearching) Icons.Default.Close else Icons.Default.PersonAdd,
                        contentDescription = "Toggle Search",
                        tint = GlowBlue
                    )
                }
            }

            if (isSearching) {
                // Search Mode
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        if (it.isNotEmpty()) {
                            viewModel.searchUsers(it)
                        } else {
                            viewModel.searchUsers("") 
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by username...", color = Color.White.copy(alpha = 0.4f)) },
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
                        UserItem(
                            name = user["name"] as? String ?: "Unknown",
                            isConnection = connections.any { it["uid"] == uid },
                            isRequested = isRequested,
                            onAddClick = { 
                                viewModel.sendRequest(uid)
                                Toast.makeText(context, "Connection request sent!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            } else {
                // Connections Mode
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    // Pending Requests Section
                    if (requests.isNotEmpty()) {
                        Text(
                            "Pending Requests",
                            color = GlowBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        requests.forEach { req ->
                            RequestItem(
                                name = req["fromName"] as? String ?: "Unknown",
                                onAccept = { 
                                    viewModel.acceptRequest(req["fromUid"] as String, req["fromName"] as String)
                                    Toast.makeText(context, "Connection accepted!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Connections List
                    Text(
                        "Your Connections",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (connections.isEmpty() && !isLoading) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("No connections yet.", color = Color.White.copy(alpha = 0.3f))
                        }
                    } else {
                        connections.forEach { friend ->
                            ConnectionItem(
                                name = friend["name"] as? String ?: "Unknown",
                                onClick = { 
                                    selectedConnection = friend
                                    showOptionsDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
        
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GlowBlue)
            }
        }
    }

    if (showOptionsDialog && selectedConnection != null) {
        ConnectionOptionsDialog(
            name = selectedConnection!!["name"] as String,
            onDismiss = { showOptionsDialog = false },
            onViewProfile = { /* Navigate to profile */ },
            onRemove = { 
                viewModel.removeConnection(selectedConnection!!["uid"] as String)
                showOptionsDialog = false
            }
        )
    }
}

@Composable
fun RequestItem(name: String, onAccept: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = GlowBlue.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(0.5.dp, GlowBlue.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = name, color = Color.White, fontWeight = FontWeight.Bold)
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = GlowBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("Accept", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UserItem(
    name: String,
    isConnection: Boolean,
    isRequested: Boolean,
    onAddClick: () -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(GlowBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        color = GlowBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = name, color = Color.White, fontWeight = FontWeight.Medium)
            }
            
            if (isConnection) {
                Icon(Icons.Default.Check, contentDescription = "Connected", tint = GlowBlue, modifier = Modifier.size(20.dp))
            } else if (isRequested) {
                Text(text = "Requested", color = GlowBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Button(
                    onClick = onAddClick,
                    colors = ButtonDefaults.buttonColors(containerColor = GlowBlue),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConnectionItem(name: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
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
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "Online", color = GlowBlue, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ConnectionOptionsDialog(
    name: String,
    onDismiss: () -> Unit,
    onViewProfile: () -> Unit,
    onRemove: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onViewProfile,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                ) {
                    Text("View Profile", color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onRemove,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                ) {
                    Text("Remove Connection", color = Color.Red)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}
