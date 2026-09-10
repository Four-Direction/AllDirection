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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConnectionsViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val auth = FirebaseAuth.getInstance()
    private val currentUid get() = auth.currentUser?.uid

    private val _connections = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val connections = _connections.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadConnections()
    }

    fun loadConnections() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _connections.value = userRepository.getConnections(uid)
            _isLoading.value = false
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _searchResults.value = userRepository.searchUsersByName(query)
                .filter { it["uid"] != currentUid } // Don't show myself
        }
    }

    fun addConnection(connectionUid: String, connectionName: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            try {
                userRepository.addConnection(uid, connectionUid, connectionName)
                loadConnections()
                _searchResults.value = emptyList() // Clear search after adding
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

@Composable
fun ConnectionsPage(
    onDismiss: () -> Unit,
    viewModel: ConnectionsViewModel = viewModel()
) {
    val connections by viewModel.connections.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            Spacer(modifier = Modifier.height(24.dp))

            if (isSearching) {
                // Search Input
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
                    placeholder = { Text("Start typing a username...", color = Color.White.copy(alpha = 0.4f)) },
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

                Spacer(modifier = Modifier.height(16.dp))

                // Search Results
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(searchResults) { user ->
                        UserItem(
                            name = user["name"] as? String ?: "Unknown",
                            isConnection = connections.any { it["uid"] == user["uid"] },
                            onAddClick = { 
                                viewModel.addConnection(user["uid"] as String, user["name"] as String)
                                searchQuery = ""
                                isSearching = false
                            }
                        )
                    }
                }
            } else {
                // Connections List
                if (connections.isEmpty() && !isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Group,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.White.copy(alpha = 0.1f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No connections yet.",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 16.sp
                            )
                            TextButton(onClick = { isSearching = true }) {
                                Text("Find your connections", color = GlowBlue)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(connections) { connection ->
                            ConnectionItem(name = connection["name"] as? String ?: "Unknown")
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
}

@Composable
fun UserItem(
    name: String,
    isConnection: Boolean,
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
fun ConnectionItem(name: String) {
    Surface(
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
