package com.weijian.shadowsocks

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.cli.*
import java.io.File
import com.fasterxml.jackson.module.kotlin.*
import org.apache.logging.log4j.LogManager
import java.io.FileNotFoundException

/**
 * Created by weijian on 16-8-2.
 */
object ConfigurationParser {
    private val options: Options

    init {
        options = Options()
        val builders = arrayOf(
                Option.builder("s").hasArg().argName("server_host").desc("Host name or IP address of your remote server"),
                Option.builder("p").hasArg().argName("server_port").desc("Port number of your remote server"),
                Option.builder("l").hasArg().argName("local_port").desc("Port number of your local server"),
                Option.builder("k").hasArg().argName("password").desc("Password of your remote server"),
                Option.builder("m").hasArg().argName("encrypt_method").desc("Encrypt method: "),
                Option.builder("c").hasArg().argName("config_file").desc("The path to config file"),
                Option.builder("v").hasArg(false).desc("verbose mode"))
        for (builder in builders) {
            options.addOption(builder.build())
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun parse(args: Array<String>): Configuration {
        val configuration = Configuration()
        val parser = DefaultParser()
        val cmd = parser.parse(options, args)
        if (cmd.hasOption("c")) {
            val configPath = cmd.getOptionValue("c")
            val file = File(configPath)
            if (!file.exists()) {
                throw FileNotFoundException("Invalid config path.")
            }
            val objectMapper = jacksonObjectMapper()
            objectMapper.readerForUpdating(configuration).readValue<Configuration>(file)
        }
        return configuration
    }
}


