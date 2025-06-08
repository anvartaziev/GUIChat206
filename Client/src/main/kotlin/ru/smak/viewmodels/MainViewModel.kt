package viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ru.smak.chat.Client

/** Data class describing a message shown in chat. */
data class ChatMessage(
    /** Text that will be displayed in the UI. */
    val text: String,
    /** Indicates that the message was written by the current user. */
    val own: Boolean = false,
)

/** View model holding all UI state of the chat client. */
class MainViewModel : ViewModel() {
    /** Text currently typed by the user. */
    var inputText by mutableStateOf("")
    /** List of chat messages to display. */
    val messages = mutableStateListOf<ChatMessage>()
    /** List of online users plus the general chat channel. */
    val users = mutableStateListOf<String>()

    /** Login and password entered by the user. */
    var login by mutableStateOf("")
    var password by mutableStateOf("")
    /** Flag indicating that the user passed authentication. */
    var authorized by mutableStateOf(false)
    /** Null means chat in the general channel, otherwise selected user. */
    var selectedUser by mutableStateOf<String?>(null)

    /** Client used for network communication with the server. */
    private val client = Client("localhost",5204)
    /**
     * Send message either privately to selected user or to the general chat
     * when no user is selected.
     */
    fun sendMessage(message: String){
        if (selectedUser != null)
            client.sendCommand("MSG ${selectedUser!!} $message")
        else
            client.sendCommand("BROADCAST $message")
    }

    /** Ask server for a list of connected users. */
    fun requestUsers(){
        client.sendCommand("LIST")
    }

    /** Try to login using provided credentials. */
    fun doLogin(){
        client.sendCommand("LOGIN ${login.trim()} ${password.trim()}")
    }

    /** Try to create a new account on the server. */
    fun doRegister(){
        client.sendCommand("REGISTER ${login.trim()} ${password.trim()}")
    }

    init {
        // Listen for messages from the server and forward them to the handler
        client.addMessageListener { handleMessage(it) }
    }

    /**
     * Parse messages received from the server and update state accordingly.
     */
    private fun handleMessage(m: String){
        when {
            // Successful authentication
            m.startsWith("LOGINOK") -> { authorized = true; requestUsers() }
            // Authentication problems or registration errors
            m.startsWith("LOGINFAIL") -> { messages.add(ChatMessage(m)) }
            // Server sends a comma separated list of online users
            m.startsWith("USERS ") -> {
                val list = m.removePrefix("USERS ")
                users.clear()
                users.add("General")
                if (list.isNotBlank()) users.addAll(list.split(","))
            }
            // Incoming chat message
            m.startsWith("MSG ") -> {
                messages.add(ChatMessage(m.removePrefix("MSG ")))
            }
            else -> messages.add(ChatMessage(m))
        }
    }
}
