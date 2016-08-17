/**
 * Created by weijian on 16-8-2.
 */
package com.weijian.shadowsocks

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class Configuration constructor(val server: String = "0.0.0.0", @JsonProperty("server_port") val serverPort: Int = 0,
                                     @JsonProperty("local_port") val localPort: Int = 1080, val password: String = "",
                                     val timeout: Int = 600, val method: String = "table", val auth: Boolean = true,
                                     val debug: Boolean = true) {
    override fun toString(): String {
        return jacksonObjectMapper().writeValueAsString(this)
    }
}