package com.weijian.shadowsocks;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Created by weijian on 16-8-17.
 */
public class ShadowsocksLocal {
    private static Logger logger = LogManager.getLogger();

    public static void main(String[] args) throws InterruptedException {
        Configuration configuration;
        try {
            configuration = ConfigurationParser.parse(args);
            Context.INSTANCE.setConfiguration(configuration);
            if (configuration.getServerPort() == 0 && configuration.getPassword().equals("")) {
                throw new Exception("Not enough arguments");
            }
            NettyUtils.startServer(configuration.getServer(),configuration.getLocalPort(), new ClientInitializer());
        } catch (Exception e) {
            logger.error(e.getMessage());
            System.exit(1);
        }

    }
}
