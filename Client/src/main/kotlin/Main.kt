import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlin.system.exitProcess
import androidx.lifecycle.viewmodel.compose.viewModel
import viewmodels.MainViewModel
import viewmodels.ChatMessage

/** Entry point composable that switches between login screen and chat screen. */
@Composable
@Preview
fun App(viewModel: MainViewModel = viewModel { MainViewModel() }) {
    MaterialTheme {
        if (!viewModel.authorized) LoginScreen(viewModel) else ChatScreen(viewModel)
    }
}

@Composable
/** Screen shown before authentication. */
fun LoginScreen(vm: MainViewModel){
    Column(modifier = Modifier.padding(8.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)){
        OutlinedTextField(vm.login, { vm.login = it }, label={ Text("Login") })
        OutlinedTextField(vm.password, { vm.password = it }, label={ Text("Password") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)){
            Button(onClick = { vm.doLogin() }){ Text("Login") }
            Button(onClick = { vm.doRegister() }){ Text("Register") }
        }
        // Show all service messages from the server
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()){ 
            items(vm.messages){ MessageCard("", it) } 
        }
    }
}

@Composable
/** Main chat UI shown after login. */
fun ChatScreen(viewModel: MainViewModel){
    Row(modifier = Modifier.padding(8.dp).fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)){
        Column(modifier = Modifier.weight(1f)){
            // Messages list
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Bottom){
                items(viewModel.messages){ MessageCard("", it) }
            }
            Input(viewModel.inputText, Modifier.fillMaxWidth().padding(8.dp), onInput = { viewModel.inputText = it }){
                if (viewModel.inputText.isNotBlank()){
                    // Add message locally before sending
                    viewModel.messages.add(ChatMessage("Me->${viewModel.selectedUser ?: "all"}: ${viewModel.inputText.trim()}", true))
                    viewModel.sendMessage(viewModel.inputText.trim())
                    viewModel.inputText = ""
                }
            }
        }
        // List of users that can be selected as message recipients
        LazyColumn(modifier = Modifier.width(150.dp).fillMaxHeight()){
            items(viewModel.users){ u ->
                Text(u, modifier = Modifier.fillMaxWidth().padding(4.dp).clickable {
                    viewModel.selectedUser = if (u == "General") null else u
                })
            }
        }
    }
}

@Composable
/** Card displaying a single chat message. */
fun MessageCard(
    senderName: String,
    message: ChatMessage,
    modifier: Modifier = Modifier,
){
    Column {
        if (senderName.isNotBlank())
            Text(
                "$senderName:",
                modifier = Modifier.padding(top = 8.dp)
            )
        Card(
            // Lighter color is used for our own messages
            backgroundColor = if (message.own) MaterialTheme.colors.primary.copy(alpha = 0.6f) else MaterialTheme.colors.primary,
            contentColor = MaterialTheme.colors.onPrimary,
            elevation = 3.dp,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                message.text,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
/** Reusable component for text input with Enter key handling. */
fun Input(
    value: String,
    modifier: Modifier = Modifier,
    onInput: (String)->Unit = {},
    onApprove: (String)->Unit = {},
){
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onInput,
            modifier = Modifier.weight(1f).onKeyEvent {
                if (it.type == KeyEventType.KeyUp && !it.isShiftPressed && (it.key == Key.NumPadEnter || it.key == Key.Enter)) {
                    onApprove(value)
                    true
                } else false
            },
            trailingIcon = {
                IconButton(
                    onClick = { onApprove(value) },
                ) {
                    Icon(Icons.Default.MailOutline, null)
                }
            }
        )

    }
}

/** Desktop entry point launching the Compose application. */
fun main() = application(true) {
    Window(onCloseRequest = { exitProcess(0) }) {
        App()
    }
}
