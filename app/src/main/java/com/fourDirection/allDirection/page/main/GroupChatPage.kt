package com.fourDirection.allDirection.page.main

import android.util.Log
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
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fourDirection.allDirection.data.UserRepository
import com.fourDirection.allDirection.ui.theme.GlowBlue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
                if (e != null) return@addSnapshotListener
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

    fun updateDescription(description: String) {
        val gid = groupId ?: return
        viewModelScope.launch {
            userRepository.updateGroupDescription(gid, description)
        }
    }

    fun updateName(name: String) {
        val gid = groupId ?: return
        viewModelScope.launch {
            userRepository.updateGroupName(gid, name)
        }
    }

    fun kickMember(memberUid: String) {
        val gid = groupId ?: return
        viewModelScope.launch {
            userRepository.kickMember(gid, memberUid)
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
                        isMe = isMe
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
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
fun MessageBubble(text: String, senderName: String, isMe: Boolean) {
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
            Text(
                text = text,
                color = if (isMe) Color.Black else Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 14.sp
            )
        }
    }
}
