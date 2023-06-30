import json.JsonModules
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class JsonModules : BeforeAllCallback {
    override fun beforeAll(extension: ExtensionContext) {
        JsonModules.registerAll()
    }
}