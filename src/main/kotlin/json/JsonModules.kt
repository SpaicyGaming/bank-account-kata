package json

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.json.jackson.DatabindCodec

/**
 * This class is responsible for registering all the JSON modules.
 */
object JsonModules {

    /**
     * Register all the JSON modules.
     * This method should be called before any JSON serialization or deserialization.
     */
    fun registerAll() {
        DatabindCodec.mapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
    }

}