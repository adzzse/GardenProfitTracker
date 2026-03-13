# Garden Profit Tracker

A Minecraft Fabric mod (1.21+) for Hypixel SkyBlock that tracks your real-time farming profit in the Garden.

## Features

*   **Location-Aware Tracking:** Automatically detects when you are in the Garden or Plot areas. Accurately pauses the tracking and "Coins per Hour" calculation when you leave or visit other islands, ensuring your session stats are perfectly accurate.
*   **Comprehensive Profit Tracking:** 
    *   Tracks standard crop drops into your inventory and sacks.
    *   Tracks rare drop messages (e.g., RNGesus drops, Squash, Fermento).
    *   Tracks Pet XP gain and converts it to equivalent coins (e.g., leveling a Greg or Gdrag).
*   **Live Bazaar Integration:** Fetches up-to-date Bazaar prices from Coflnet. You can configure it to use Insta-Sell (your actual expected profit) or Insta-Buy prices.
*   **Three HUD Modes:** 
    *   **Session:** Tracks profit, coins per hour, and accurate Garden uptime for your current session.
    *   **Daily:** Tracks your total profit over the last 24 hours.
    *   **Lifetime:** Tracks your all-time profit while using the mod.
*   **Configurable Display:** Commands to hide, show, or customize your profit HUD.

## Commands

*   `/gardenprofit` - Opens the main configuration screen.
*   `/gardenprofit reset` - Resets the current session profit and garden uptime to 0.
*   `/gardenprofit hide` - Hides the profit HUD displays.
*   `/gardenprofit show` - Shows the profit HUD displays.
*   `/gardenprofit pricemode` - Toggles the Bazaar price fetching mode between Insta-Sell and Insta-Buy and re-fetches prices.

## Configuration

Settings are saved in your instance's `config/garden_profit_config.json` folder. Key settings include:
*   HUD positioning and opacity.
*   Tracker lists for specific pets and items to watch.
*   Toggling the compact calculator mode.

## Installation

1.  Make sure you have [Fabric Loader](https://fabricmc.net/) installed for Minecraft 1.21.1.
2.  Install the required dependencies (such as Fabric API).
3.  Drop the compiled `.jar` file into your `.minecraft/mods` folder.

## Building from source

To build the mod from the source code, open a terminal in the project directory and run:

```bash
# On Windows
gradlew build

# On Linux/macOS
./gradlew build
```

The compiled mod will be located in the `build/libs` directory.
