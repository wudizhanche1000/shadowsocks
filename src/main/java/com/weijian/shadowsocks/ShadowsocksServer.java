package com.weijian.shadowsocks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
            if (configuration.getServerPort() == 0 || configuration.getPassword().equals("")) {
                logger.error("Not enough arguments");
            }
            NettyUtils.startServer(configuration.getServer(), configuration.getServerPort(), new ShadowsocksInitializer());
        } catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
    }
}
