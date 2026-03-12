package com.gardenprofit.mod;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GardenProfit implements ModInitializer {
        public static final Logger LOGGER = LoggerFactory.getLogger("gardenprofit");

        @Override
        public void onInitialize() {
                LOGGER.info("GardenProfit v0 Initialized!");
        }
}
