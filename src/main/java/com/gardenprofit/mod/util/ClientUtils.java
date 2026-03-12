package com.gardenprofit.mod.util;

import com.gardenprofit.mod.GardenProfitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.DisplaySlot;

import java.util.Collection;

public class ClientUtils {

    public static void sendDebugMessage(Minecraft client, String message) {
        if (GardenProfitConfig.showDebug) {
            client.execute(() -> {
                if (client.player != null) {
                    client.player.displayClientMessage(
                            Component.literal("\u00A79[Debug] " + message),
                            false);
                }
            });
        }
    }

    public static long getPurse(Minecraft client) {
        if (client.level == null || client.player == null)
            return 0;

        Scoreboard scoreboard = client.level.getScoreboard();
        if (scoreboard == null)
            return 0;

        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null)
            return 0;

        Collection<PlayerScoreEntry> scores = scoreboard.listPlayerScores(sidebar);
        for (PlayerScoreEntry entry : scores) {
            String entryName = entry.owner();
            PlayerTeam team = scoreboard.getPlayersTeam(entryName);
            String fullText = entryName;
            if (team != null) {
                fullText = team.getPlayerPrefix().getString() + entryName + team.getPlayerSuffix().getString();
            }
            String line = fullText.replaceAll("(?i)\u00A7[0-9A-FK-ORZ]", "").replaceAll(",", "").trim();

            if (line.contains("Purse:")) {
                try {
                    String valuePart = line.split("Purse:")[1].trim();
                    String mainBalance = valuePart.split(" ")[0].replaceAll("[^0-9]", "");
                    return Long.parseLong(mainBalance);
                } catch (Exception ignored) {
                }
            }
        }
        return -1;
    }
}
