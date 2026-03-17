package com.gardenprofit.mod.modules;

import com.gardenprofit.mod.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Reads the Hypixel SkyBlock scoreboard sidebar to determine
 * which area the player is in. Emits a debug message whenever
 * the detected area changes.
 *
 * The tracking modules use {@link #isInGarden()} to pause when
 * the player leaves the Garden.
 *
 * Also tracks how long the player has been in the Garden (uptime).
 */
public class LocationTracker {

    // Strip ALL section-sign formatting codes (Hypixel uses non-standard ones like §y)
    private static final Pattern STRIP_COLOR = Pattern.compile("\u00A7.");

    private static String currentArea = "";
    private static boolean inGarden = false;

    // Garden uptime tracking (only counts time spent in Garden)
    private static long gardenEnteredTime = 0;
    private static long accumulatedGardenMs = 0;

    /**
     * Called every few ticks from the client tick callback.
     * Reads the scoreboard sidebar and updates the current area.
     */
    public static void tick(Minecraft client) {
        if (client.level == null || client.player == null) {
            return;
        }

        Scoreboard scoreboard = client.level.getScoreboard();
        if (scoreboard == null) return;

        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) return;

        Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(sidebar);
        String detectedArea = "";

        for (PlayerScoreEntry entry : scores) {
            String entryName = entry.owner();
            PlayerTeam team = scoreboard.getPlayersTeam(entryName);
            String fullText = entryName;
            if (team != null) {
                fullText = team.getPlayerPrefix().getString() + entryName + team.getPlayerSuffix().getString();
            }
            String line = STRIP_COLOR.matcher(fullText).replaceAll("").trim();

            // Hypixel SkyBlock shows area as " <icon> Area Name" on the sidebar
            if (line.contains("\u23E3")) {
                // The area icon (\u23E3) precedes the area name
                int idx = line.indexOf('\u23E3');
                detectedArea = line.substring(idx + 1).trim();
                break;
            }
        }

        if (!detectedArea.equals(currentArea)) {
            boolean wasInGarden = inGarden;
            String oldArea = currentArea;
            currentArea = detectedArea;
            inGarden = currentArea.toLowerCase().startsWith("the garden")
                    || currentArea.toLowerCase().startsWith("plot");

            // Update uptime tracking
            if (inGarden && !wasInGarden) {
                // Entered garden
                gardenEnteredTime = System.currentTimeMillis();
            } else if (!inGarden && wasInGarden) {
                // Left garden, accumulate time
                if (gardenEnteredTime > 0) {
                    accumulatedGardenMs += System.currentTimeMillis() - gardenEnteredTime;
                    gardenEnteredTime = 0;
                }
            }

            ClientUtils.sendDebugMessage(client,
                    "[Location] Area changed: '" + oldArea + "' -> '" + currentArea
                            + "' (inGarden=" + inGarden + ")");
        }
    }

    /**
     * Returns true if the player is currently in the Garden or on a Garden plot.
     */
    public static boolean isInGarden() {
        return inGarden;
    }

    /**
     * Returns the raw detected area string from the scoreboard.
     */
    public static String getCurrentArea() {
        return currentArea;
    }

    /**
     * Returns total milliseconds the player has spent in the Garden this session.
     * Includes currently ongoing garden time if the player is in the garden now.
     */
    public static long getGardenUptimeMs() {
        long total = accumulatedGardenMs;
        if (inGarden && gardenEnteredTime > 0) {
            total += System.currentTimeMillis() - gardenEnteredTime;
        }
        return total;
    }

    /**
     * Formats garden uptime as HH:MM:SS.
     */
    public static String getFormattedUptime() {
        long ms = getGardenUptimeMs();
        long totalSeconds = ms / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Reset state on world switch.
     */
    public static void onWorldSwitch() {
        if (inGarden && gardenEnteredTime > 0) {
            accumulatedGardenMs += System.currentTimeMillis() - gardenEnteredTime;
        }
        currentArea = "";
        inGarden = false;
        gardenEnteredTime = 0;
    }

    /**
     * Reset uptime counter (e.g. on session reset).
     */
    public static void resetUptime() {
        accumulatedGardenMs = 0;
        if (inGarden) {
            gardenEnteredTime = System.currentTimeMillis();
        } else {
            gardenEnteredTime = 0;
        }
    }
}
