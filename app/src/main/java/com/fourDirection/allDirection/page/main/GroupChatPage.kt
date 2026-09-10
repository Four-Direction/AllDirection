package com.fourDirection.allDirection.page.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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

    private val _currentUserName = MutableStateFlow("User")
    
    private var messagesListener: ListenerRegistration? = null
    private var groupId: String? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            groupId?.let { gid ->
                startMessagesListener(gid)
                fetchCurrentUserName(uid)
            }
        } else {
            stopMessagesListener()
            _messages.value = emptyList()
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
            startMessagesListener(groupId)
            fetchCurrentUserName(uid)
        }
    }

    private fun startMessagesListener(groupId: String) {
        stopMessagesListener()
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

    private fun stopMessagesListener() {
        messagesListener?.remove()
        messagesListener = null
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        stopMessagesListener()
        super.onCleared()
    }
    
    fun isMe(senderUid: Any?): Boolean {
        return senderUid == currentUid
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
    val listState = rememberLazyListState()

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
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = groupName,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Group Chat",
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
                    .fillMaxWidth()
                    .navigationBarsPadding(),
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
