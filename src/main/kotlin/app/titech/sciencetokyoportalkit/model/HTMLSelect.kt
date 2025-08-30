package app.titech.sciencetokyoportalkit.model

data class HTMLSelect(
    val name: String,
    val values: List<String>,
    var selectedValue: String? = null
) {
    fun select(value: String) {
        if (values.contains(value)) {
            selectedValue = value
        }
    }
}