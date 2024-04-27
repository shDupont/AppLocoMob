package br.com.fiap.applocomob

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.EditText
import androidx.activity.ComponentActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class CadastroActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cadastro) // Certifique-se de que o nome do XML corresponda ao seu arquivo XML
        val dtNascEditText: EditText = findViewById(R.id.dt_nasc_edit_text)

        // Defina um listener para o campo de data de nascimento
        dtNascEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Crie um DatePickerDialog para selecionar a data
            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    // Formate a data selecionada no formato "dd/MM/yyyy"
                    val selectedDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                    val outputFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    val formattedDate = selectedDate.format(outputFormat)

                    // Atualize o texto do EditText com a data formatada
                    dtNascEditText.setText(formattedDate)

                    // Verifique se a idade é maior ou igual a 18 anos
                    val currentDate = LocalDate.now()
                    val age = currentDate.year - selectedYear
                    if (age >= 18) {
                        // A idade é válida
                        // Faça o que for necessário aqui
                    } else {
                        // A idade não é válida
                        // Exiba uma mensagem de erro ou tome alguma outra ação
                    }
                },
                year,
                month,
                day
            )

            // Defina a data máxima como a data atual
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

            // Exiba o DatePickerDialog
            datePickerDialog.show()
        }
    }

}
