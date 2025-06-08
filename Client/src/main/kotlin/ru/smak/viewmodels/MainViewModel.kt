package viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import ru.smak.chat.Client

class MainViewModel : ViewModel() {
    var inputText by mutableStateOf("")
    val messages = mutableStateListOf<String>()
    val users = mutableStateListOf<String>()

    var login by mutableStateOf("")
    var password by mutableStateOf("")
    var authorized by mutableStateOf(false)
    var selectedUser by mutableStateOf<String?>(null)

    private val client = Client("localhost",5204)
    fun sendMessage(message: String){
        if (selectedUser != null)
            client.sendCommand("MSG ${selectedUser!!} $message")
        else
            client.sendCommand("BROADCAST $message")
    }

    fun requestUsers(){
        client.sendCommand("LIST")
    }

    fun doLogin(){
        client.sendCommand("LOGIN $login $password")
    }

    fun doRegister(){
        client.sendCommand("REGISTER $login $password")
    }

    init {
        client.addMessageListener { handleMessage(it) }
    }

    private fun handleMessage(m: String){
        when {
            m.startsWith("LOGINOK") -> { authorized = true; requestUsers() }
            m.startsWith("LOGINFAIL") -> { messages.add(m) }
            m.startsWith("USERS ") -> {
                val list = m.removePrefix("USERS ")
                users.clear()
                if (list.isNotBlank()) users.addAll(list.split(","))
            }
            m.startsWith("MSG ") -> {
                messages.add(m.removePrefix("MSG "))
            }
            else -> messages.add(m)
        }
    }
}
