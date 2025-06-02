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
    private val client = Client("localhost",5204)
    fun sendMessage(message: String)=client.sendMessage(message)
    init {
        client.addMessageListener {
            messages.add(it)
        }
    }
}
