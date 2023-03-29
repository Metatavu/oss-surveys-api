package fi.metatavu.example.api.impl

sealed class UserRole {

    object MANAGER : UserRole() {
        const val name = "manager"
    }

    object CONSUMER_DISPLAY : UserRole() {
        const val name = "consumer_display"
    }

}