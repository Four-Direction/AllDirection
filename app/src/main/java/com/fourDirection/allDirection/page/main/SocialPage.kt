package com.fourDirection.allDirection.page.main

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

class SocialViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUid get() = auth.currentUser?.uid

    private val _connections = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val connections = _connections.asStateFlow()

    private val _myGroups = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val myGroups = _myGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var connectionsListener: ListenerRegistration? = null
    private var groupsListener: ListenerRegistration? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            startRealtimeListeners(uid)
        } else {
            stopRealtimeListeners()
            _connections.value = emptyList()
            _myGroups.value = emptyList()
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    private fun startRealtimeListeners(uid: String) {
        stopRealtimeListeners()
        
        // Listen for Connections
        connectionsListener = db.collection("users").document(uid)
            .collection("friends")
            .orderBy("name")
            .addSnapshotListener { snapshot, _ ->
                _connections.value = snapshot?.documents?.mapNotNull { it.data } ?: emptyList()
            }
            
        // Listen for Groups I am a member of
        groupsListener = db.collection("groups")
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { snapshot, _ ->
                _myGroups.value = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data?.toMutableMap() ?: return@mapNotNull null
                    data["groupName"] = data["name"]
                    data
                } ?: emptyList()
            }
    }

    private fun stopRealtimeListeners() {
        connectionsListener?.remove()
        groupsListener?.remove()
        connectionsListener = null
        groupsListener = null
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        stopRealtimeListeners()
        super.onCleared()
    }

    fun createGroup(name: String, selectedMembers: List<Map<String, String>>) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                userRepository.createGroup(uid, name, selectedMembers)
            } catch (e: Exception) {
                // Log or handle error
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
                userRepository.leaveGroup(groupId, uid)
            } catch (e: Exception) {
                // Log or handle error
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

                // Tab Switcher
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 16.dp)
                        .clip(CircleShape),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SubTabItem(
                            label = "Groups",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        SubTabItem(
                            label = "Community",
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                    }
                }

                if (selectedTab == 0) {
                    GroupList(viewModel, onGroupClick)
                } else {
                    PlaceholderPage("Community Coming Soon")
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
fun GroupList(viewModel: SocialViewModel, onGroupClick: (Map<String, Any>) -> Unit) {
    val groups by viewModel.myGroups.collectAsState()
    var selectedGroupForOptions by remember { mutableStateOf<Map<String, Any>?>(null) }

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
            items(groups) { group ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupClick(group) },
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
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
                            Text(
                                text = group["groupName"] as? String ?: "Unnamed Group",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
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
            onDismiss = { selectedGroupForOptions = null },
            onDelete = {
                viewModel.deleteGroup(group["id"] as String)
                selectedGroupForOptions = null
            },
            onLeave = {
                viewModel.leaveGroup(group["id"] as String)
                selectedGroupForOptions = null
            }
        )
    }
}

@Composable
fun GroupOptionsDialog(
    groupName: String,
    isCreator: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onLeave: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1A1A),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = groupName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isCreator) {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                    ) {
                        Text("Delete Group", color = Color.Red)
                    }
                } else {
                    Button(
                        onClick = onLeave,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f))
                    ) {
                        Text("Leave Group", color = Color.Red)
                    }
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
                        val uid = connection["uid"] as String
                        val name = connection["name"] as String
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
                            .filter { selectedUids.contains(it["uid"] as String) }
                            .map { mapOf("uid" to it["uid"] as String, "name" to it["name"] as String) }
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
