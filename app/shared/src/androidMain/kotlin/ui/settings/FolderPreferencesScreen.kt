package com.xmvisio.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xmvisio.app.data.FolderType
import com.xmvisio.app.data.MediaFolder
import com.xmvisio.app.folder.FolderRepository

/**
 * 文件夹管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun FolderPreferencesScreen(
    onBack: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val folderRepository = remember { FolderRepository.getInstance(context) }
    val folders by folderRepository.folders.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<MediaFolder?>(null) }
    var renameText by remember { mutableStateOf("") }

    // SAF 文件夹选择器
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        val path = getPathFromUri(uri) ?: return@rememberLauncherForActivityResult
        val name = getFileNameFromUri(context, uri) ?: path.substringAfterLast("/")

        // 检查是否已存在
        if (folderRepository.getFolderByPath(path) == null) {
            folderRepository.addFolder(name, path)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件夹管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // 打开系统文件夹选择器
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    folderPickerLauncher.launch(intent)
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加文件夹")
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ) { paddingValues ->
        if (folders.isEmpty()) {
            // 空状态
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = folderPainter(),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "暂无文件夹",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "点击右下角按钮添加文件夹",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // 文件夹列表
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(folders) { folder ->
                    FolderItem(
                        folder = folder,
                        onRename = {
                            selectedFolder = folder
                            renameText = folder.name
                            showRenameDialog = true
                        },
                        onDelete = {
                            selectedFolder = folder
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // 重命名对话框
    if (showRenameDialog && selectedFolder != null) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("重命名文件夹") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("文件夹名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedFolder?.let { folder ->
                            folderRepository.renameFolder(folder.id, renameText)
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 删除确认对话框
    if (showDeleteDialog && selectedFolder != null) {
        var deleteFiles by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除文件夹") },
            text = {
                Column {
                    Text("确定要删除文件夹 \"${selectedFolder?.name}\" 吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = deleteFiles,
                            onCheckedChange = { deleteFiles = it }
                        )
                        Text("同时删除文件")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedFolder?.let { folder ->
                            folderRepository.deleteFolder(folder.id, deleteFiles)
                        }
                        showDeleteDialog = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 文件夹列表项
 */
@Composable
private fun FolderItem(
    folder: MediaFolder,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = folderPainter(),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = folder.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = when (folder.type) {
                        FolderType.MUSIC -> "音频"
                        FolderType.VIDEO -> "视频"
                        FolderType.ALL -> "混合"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onRename) {
                Icon(
                    imageVector = Icons.Default.DriveFileRenameOutline,
                    contentDescription = "重命名"
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 从 URI 获取路径
 */
private fun getPathFromUri(uri: Uri): String? {
    // 处理 SAF 树 URI: content://com.android.externalstorage.documents/tree/primary%3ADownload
    if (uri.toString().contains("/tree/")) {
        val docId = android.provider.DocumentsContract.getDocumentId(uri)
        // docId 格式: "primary:Download" 或 "primary:Download/SubFolder"
        if (docId.startsWith("primary:")) {
            return "/storage/emulated/0/" + docId.removePrefix("primary:").trimStart('/')
        }
        return null
    }
    // 普通 content URI
    return uri.path
}

/**
 * 从 URI 获取文件夹名
 */
private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String? {
    // 处理 SAF 树 URI
    if (uri.toString().contains("/tree/")) {
        val docId = android.provider.DocumentsContract.getDocumentId(uri)
        if (docId.startsWith("primary:")) {
            val name = docId.removePrefix("primary:").substringAfterLast("/").trim('/')
            return name.ifEmpty { "根目录" }
        }
        return null
    }
    // 普通 content URI
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                it.getString(nameIndex)
            } else {
                null
            }
        } else {
            null
        }
    }
}

@Composable
private fun folderPainter(): androidx.compose.ui.graphics.painter.Painter {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier("folder_thumb", "drawable", context.packageName)
    return painterResource(resId)
}
