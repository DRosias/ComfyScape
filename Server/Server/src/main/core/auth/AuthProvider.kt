package core.auth

import core.game.node.entity.player.Player
import core.storage.AccountStorageProvider
import core.ServerConstants
import core.game.world.GameWorld
import core.security.RolePolicy

abstract class AuthProvider<T: AccountStorageProvider> {
    lateinit var storageProvider: T

    abstract fun configureFor(provider: T)

    fun canCreateAccountWith(info: UserAccountInfo) : Boolean {
        val reservedClanName = GameWorld.settings?.enable_default_clan == true && RolePolicy.isReserved(info.username)
        return !reservedClanName && !storageProvider.checkUsernameTaken(info.username)
    }

    abstract fun createAccountWith(info: UserAccountInfo) : Boolean

    abstract fun checkLogin(username: String, password: String) : Pair<AuthResponse, UserAccountInfo?>

    abstract fun checkPassword(player: Player, password: String) : Boolean

    abstract fun updatePassword(username: String, newPassword: String)
}
