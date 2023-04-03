package fi.metatavu.oss.api.impl

sealed class UserRole {

    object MANAGER : fi.metatavu.oss.api.impl.UserRole() {
        const val name = "manager"
    }

    object CONSUMER_DISPLAY : fi.metatavu.oss.api.impl.UserRole() {
        const val name = "consumer_display"
    }

}