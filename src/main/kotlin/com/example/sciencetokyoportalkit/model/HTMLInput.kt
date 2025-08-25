package app.titech.sciencetokyoportalkit.model

enum class HTMLInputType(val value: String) {
    TEXT("text"),
    PASSWORD("password"),
    CHECKBOX("checkbox"),
    RADIO("radio"),
    FILE("file"),
    HIDDEN("hidden"),
    SUBMIT("submit"),
    RESET("reset"),
    BUTTON("button"),
    IMAGE("image");

    companion object {
        fun fromValue(value: String): HTMLInputType {
            return entries.find { it.value == value } ?: TEXT
        }
    }
}

data class HTMLInput(
    val name: String,
    val type: HTMLInputType,
    var value: String
)