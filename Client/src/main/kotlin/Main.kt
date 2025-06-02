import androidx.compose.animation.fadeIn
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import viewmodels.MainViewModel

@Composable
@Preview
fun App(viewModel: MainViewModel = viewModel { MainViewModel() }) {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(8.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {


            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Bottom,
            ) {
                items(viewModel.messages){
                    Card(
                        backgroundColor = MaterialTheme.colors.primary,
                        contentColor = MaterialTheme.colors.onPrimary,
                        elevation = 3.dp,
                        modifier = Modifier.padding(top=8.dp)
                    ){
                        Text(
                            it,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

            }
            Input(
                viewModel.inputText,
                Modifier.fillMaxWidth().padding(8.dp),
                onInput = { viewModel.inputText = it },
            ) {
                if(viewModel.inputText.isNotBlank()){
                    viewModel.messages.add(viewModel.inputText.trim())
                    viewModel.inputText = ""
                }

            }
        }
    }
}

@Composable
fun Input(
    value: String,
    modifier: Modifier = Modifier,
    onInput: (String)->Unit = {},
    onApprove: (String)->Unit = {},
){
    Row(modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onInput,
            modifier = Modifier.weight(1f).onKeyEvent{
                if (it.type == KeyEventType.KeyUp && !it.isShiftPressed && (it.key == Key.NumPadEnter || it.key == Key.Enter)){
                    onApprove(value)
                    true
                }
                else false
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

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
