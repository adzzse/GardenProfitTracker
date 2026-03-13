# Garden Profit Tracker

A Minecraft Fabric mod (1.21+) for Hypixel SkyBlock that tracks your real-time farming profit in the Garden.

## Features

*   **Location:** Automatically resume when detect the Garden or Plot, pauses the tracking and "Coins per Hour" when not.
*   **Profit Tracking:** 
    *   Tracks standard crop drops into your inventory and sacks.
    *   Tracks rare drop messages (e.g., RNGesus drops).
    *   Tracks Pet XP gain and converts it to equivalent coins (e.g., leveling a Greg or Gdrag).
*   **Live Bazaar Integration:** Fetches up-to-date Bazaar prices from Coflnet. Refresh every hour.
*   **HUD Modes:** 
    *   **Session:** Tracks profit, coins per hour, and Garden uptime for your current (Minecraft) session.
    *   **Daily:** Tracks your total profit everyday (reset at 12pm local time).
    *   **Lifetime:** Tracks your all-time profit while using the mod.
*   **Configurable:** Commands to hide, show, or customize your profit HUD.

## Commands

*   `/gardenprofit` - Opens the main configuration screen.
*   `/gardenprofit reset` - Resets session profit and garden uptime.
*   `/gardenprofit toggle` - Hides/show profit HUD.
*   `/gardenprofit pricemode` - Toggles the Bazaar price fetching mode between Insta-Sell and Insta-Buy and re-fetches prices.

## Configuration

Settings are saved in your instance's `config/garden_profit_config.json` folder. Key settings include:
*   HUD positioning and opacity.
*   Tracker lists.
*   Toggling the compact calculator mode.

## Installation

1.  Make sure you have [Fabric Loader](https://fabricmc.net/) installed for Minecraft 1.21.11/1.21.x.
2.  Install the required dependencies (such as Fabric API).
3.  Drop the compiled `.jar` file into your `.minecraft/mods` folder.

## Building from source

Clone the project, open a terminal in the project directory and run:

```bash
# On Windows
gradlew build

# On Linux/macOS
./gradlew build
```

The compiled mod will be located in the `build/libs` directory.
