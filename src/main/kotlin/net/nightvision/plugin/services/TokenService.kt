package net.nightvision.plugin.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.project.Project
import net.nightvision.plugin.Constants.Companion.NIGHTVISION
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
@State(
    name = "NightvisionIntelliJPlugin.TokenService",
    storages = [Storage("nightvisionIntelliJPlugin.tokens.xml")]
)
class TokenService(private val project: Project)
    : PersistentStateComponent<TokenService.State> {

    private var _token: String = ""

    // Returns the current token. If empty, clears old keys in background and obtains a new token.
    var token: String
        get() {
            if (_token.isEmpty()) {
                _token = createToken(project)
            }
            return _token
        }
        set(value) {
            _token = value
        }

    data class State(var tokenKeys: MutableList<String> = mutableListOf())

    private var myState = State()

    override fun getState() = myState

    override fun loadState(state: State) {
        myState = state
    }

    private fun addToken(fullToken: String) {
        val shortKey = fullToken.take(8)
        if (shortKey !in myState.tokenKeys) {
            myState.tokenKeys += shortKey
        }
    }

    private fun getAllTokenKeys() = myState.tokenKeys.toList()

    private fun clearAllTokenKeys() {
        for (tokenKey in myState.tokenKeys) {
            if (token.startsWith(tokenKey)) {
                continue
            }
            clearToken(tokenKey)
        }
    }

    private fun clearToken(tokenKey: String) {
        throw NotImplementedError("TODO: Must call the API to delete each individual token. Also, need to fetch digest...")
        if (tokenKey in myState.tokenKeys) {
            myState.tokenKeys.remove(tokenKey)
        }
    }

    companion object {
        fun getInstance(project: Project): TokenService =
            project.getService(TokenService::class.java)
    }

    fun createToken(project: Project): String {
        val response = CommandRunnerService.runCommandSync(NIGHTVISION, "token", "create")

        val t = response.output
        token = t.trim().takeIf { it.matches(Regex("^\\S{64}$")) } ?: ""
        //println(token)
        addToken(token)

        // TODO: Enable this later - asynchronously clean up any old tokens
//        ApplicationManager.getApplication().executeOnPooledThread {
//            clearAllTokenKeys()
//        }

        return token
    }
}
