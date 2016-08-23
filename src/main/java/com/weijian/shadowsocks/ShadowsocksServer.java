package com.weijian.shadowsocks;

import io.netty.channel.ChannelOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by weijian on 16-8-2.
 */
public class ShadowsocksServer {
    private static final Logger logger = LogManager.getLogger("Shadowsocks");

    public static void main(String[] args) throws InterruptedException {
        Configuration configuration;
        try {
            configuration = ConfigurationParser.parse(args);
            Context.INSTANCE.setConfiguration(configuration);
            Context.INSTANCE.setServerMode(true);
            if (configuration.getServerPort() == 0 || configuration.getPassword().equals("")) {
                logger.error("Not enough arguments");
            }
            Map<ChannelOption<?>, Object> childOptions = new HashMap<>();
            childOptions.put(ChannelOption.AUTO_READ, false);
            NettyUtils.startServer(configuration.getServer(), configuration.getServerPort(), new ShadowsocksInitializer(), null, childOptions);
        } catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
    }
}

