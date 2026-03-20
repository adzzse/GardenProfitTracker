package com.gardenprofit.mod.modules;

import com.gardenprofit.mod.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts all chat-message parsing logic (pest drops, rare drops,
 * bazaar buys, visitor rewards, sprays) that was previously inlined
 * inside ProfitManager.handleChatMessage().
 *
 * This class is stateless except for the visitor-reward tracking state
 * machine which spans multiple consecutive messages.
 */
public final class ChatMessageParser {

    private static final ChatMessageParser INSTANCE = new ChatMessageParser();

    // ── Regex patterns ──────────────────────────────────────────────────

    private static final Pattern PEST_PATTERN = Pattern.compile("received\\s+(\\d+)x\\s+(.+?)\\s+for\\s+killing",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RARE_DROP_PATTERN = Pattern.compile(
            "(?:UNCOMMON|RARE|CRAZY RARE|PRAY TO RNGESUS) DROP!\\s+(?:You dropped\\s+)?(?:an?\\s+)?(?:(\\d+)x\\s+)?(.+?)(?=\\s*(?:\u00A7[0-9a-fk-or])*\\s*[\\(!]|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PET_DROP_PATTERN = Pattern.compile(
            "PET DROP!\\s+.*?\u00A7([0-9a-f])(?:\u00A7[0-9a-fk-or])*\\s*(?:(?:COMMON|UNCOMMON|RARE|EPIC|LEGENDARY|MYTHIC)\\s+(?:\u00A7[0-9a-fk-or])*)?(.+?)(?=\\s*(?:\u00A7[0-9a-fk-or])*\\s*[\\(!]|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern OVERFLOW_DROP_PATTERN = Pattern.compile(
            "OVERFLOW!\\s+.*?\\s+has\\s+just\\s+dropped\\s+(?:an?\\s+)?(?:(\\d+)x\\s+)?(.+?)(?=\\s*\\(!|!|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PEST_SHARD_PATTERN = Pattern.compile(
            "charmed\\s+a\\s+Pest\\s+and\\s+captured\\s+(?:its\\s+Shard|(\\d+)\\s+Shards)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern BAZAAR_BUY_PATTERN = Pattern.compile(
            "\\[Bazaar\\] Bought (\\d+)x (.+?) for ([\\d,.]+) coins!",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SPRAY_PATTERN = Pattern.compile(
            "SPRAYONATOR! You sprayed Plot - \\d+ with (.+?)(?:!|$)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)\u00A7[0-9A-FK-OR]");

    // Bazaar purchase ignore tracking
    private static final Map<String, Long> recentBazaarPurchases = new java.util.LinkedHashMap<>();
    private static final long BAZAAR_PURCHASE_IGNORE_MS = 15_000;
    private long lastBazaarSprayBuyTime = 0;

    // Visitor reward state machine
    private boolean isTrackingVisitorRewards = false;
    private boolean copperSeenInRewards = false;

    private ChatMessageParser() {}

    public static ChatMessageParser getInstance() { return INSTANCE; }

    public void handleChatMessage(Component component) {
        String text = toLegacyText(component);

        // PET DROP needs raw text to detect color-coded rarity
        Matcher petMatcher = PET_DROP_PATTERN.matcher(text);
        if (petMatcher.find()) {
            String colorCode = petMatcher.group(1).toLowerCase();
            String petName = petMatcher.group(2).trim();
            String finalName = petName;

            if (petName.equalsIgnoreCase("Slug")) {
                if (colorCode.equals("5") || colorCode.equals("d")) {
                    finalName = "Epic Slug";
                } else if (colorCode.equals("6")) {
                    finalName = "Legendary Slug";
                }
            } else if (petName.equalsIgnoreCase("Rat")) {
                finalName = "Rat";
            }
            ProfitManager.getInstance().addDropFromSource(finalName, 1, ProfitManager.DropSource.PET_DROP);
            return;
        }

        // Plain text processing for standard drops
        String plainText = STRIP_COLOR_PATTERN.matcher(text).replaceAll("").trim();

        Matcher overflowMatcher = OVERFLOW_DROP_PATTERN.matcher(plainText);
        if (overflowMatcher.find()) {
            try {
                String countStr = overflowMatcher.group(1);
                int count = (countStr != null) ? Integer.parseInt(countStr) : 1;
                String itemName = overflowMatcher.group(2).trim();
                ProfitManager.getInstance().addDropFromSource(itemName, count, ProfitManager.DropSource.OVERFLOW_DROP);
                return;
            } catch (Exception ignored) {
            }
        }

        Matcher pestMatcher = PEST_PATTERN.matcher(plainText);
        if (pestMatcher.find()) {
            try {
                int count = Integer.parseInt(pestMatcher.group(1));
                String itemName = pestMatcher.group(2).trim();
                ProfitManager.getInstance().addDropFromSource(itemName, count, ProfitManager.DropSource.PEST);
                return;
            } catch (Exception ignored) {
            }
        }

        Matcher rareMatcher = RARE_DROP_PATTERN.matcher(plainText);
        if (rareMatcher.find()) {
            try {
                String countStr = rareMatcher.group(1);
                int count = (countStr != null) ? Integer.parseInt(countStr) : 1;
                String itemName = rareMatcher.group(2).trim();
                ProfitManager.getInstance().addDropFromSource(itemName, count, ProfitManager.DropSource.RARE_DROP);
            } catch (Exception ignored) {
            }
        }

        Matcher shardMatcher = PEST_SHARD_PATTERN.matcher(plainText);
        if (shardMatcher.find()) {
            try {
                String countStr = shardMatcher.group(1);
                int count = (countStr != null) ? Integer.parseInt(countStr) : 1;
                ProfitManager.getInstance().addDropFromSource("Pest Shard", count, ProfitManager.DropSource.PEST_SHARD);
            } catch (Exception ignored) {
            }
        }

        Matcher bazaarMatcher = BAZAAR_BUY_PATTERN.matcher(plainText);
        if (bazaarMatcher.find()) {
            try {
                int count = Integer.parseInt(bazaarMatcher.group(1));
                String itemName = bazaarMatcher.group(2).trim();
                double coins = Double.parseDouble(bazaarMatcher.group(3).replace(",", ""));
                ClientUtils.sendDebugMessage(Minecraft.getInstance(),
                        "Bazaar buy detected: " + count + "x " + itemName + " for " + coins + " coins");
                if (ItemConstants.PEST_ITEMS.contains(itemName)) {
                    ProfitManager.getInstance().addSprayCost(count, Math.round(coins));
                    lastBazaarSprayBuyTime = System.currentTimeMillis();
                } else {
                    ProfitManager.getInstance().addVisitorCost(Math.round(coins));
                }
                
                recentBazaarPurchases.put(itemName, System.currentTimeMillis() + BAZAAR_PURCHASE_IGNORE_MS);
            } catch (Exception ignored) {
            }
        }

        // ── Visitor Rewards Tracking ──
        if (plainText.equalsIgnoreCase("REWARDS")) {
            isTrackingVisitorRewards = true;
            copperSeenInRewards = false;
            return;
        }

        if (isTrackingVisitorRewards) {
            if (plainText.isEmpty()) {
                isTrackingVisitorRewards = false;
                return;
            }

            if (plainText.contains("Farming XP") || plainText.contains("Garden Experience")) {
                return;
            }

            if (plainText.contains("Copper")) {
                copperSeenInRewards = true;
            }

            if (copperSeenInRewards) {
                Matcher m = Pattern.compile("^\\+?([\\d,.]+)[xX]?\\s+(.+)").matcher(plainText);
                if (m.find()) {
                    String item = m.group(2).trim();
                    String countStr = m.group(1).replace(",", "");
                    long rewardCount = 1;
                    try {
                        if (countStr.toLowerCase().endsWith("k")) {
                            rewardCount = (long) (Double.parseDouble(countStr.substring(0, countStr.length() - 1)) * 1000);
                        } else {
                            rewardCount = Long.parseLong(countStr);
                        }
                    } catch (Exception ignored) {
                    }
                    ProfitManager.getInstance().addVisitorGain(item, rewardCount);
                } else {
                    ProfitManager.getInstance().addVisitorGain(plainText, 1);
                }
            }
            return;
        }

        Matcher sprayMatcher = SPRAY_PATTERN.matcher(plainText);
        if (sprayMatcher.find()) {
            String baitName = sprayMatcher.group(1).trim();
            long now = System.currentTimeMillis();
            if (now - lastBazaarSprayBuyTime < 15000) {
                ClientUtils.sendDebugMessage(Minecraft.getInstance(),
                        "Sprayonator use ignored due to recent Bazaar buy.");
            } else {
                ClientUtils.sendDebugMessage(Minecraft.getInstance(), "Sprayonator use detected (" + baitName + ").");
                double baitPrice = ProfitManager.getInstance().getItemPrice(baitName);
                if (baitPrice <= 0) baitPrice = 1.0;
                ProfitManager.getInstance().addSprayCost(1, Math.round(baitPrice));
            }
        }
    }

    /**
     * Returns true if the given item was recently purchased from the Bazaar
     * and should be ignored by SackTracker / InventoryTracker.
     */
    public boolean isBazaarPurchaseIgnored(String itemName) {
        Long expiry = recentBazaarPurchases.get(itemName);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            recentBazaarPurchases.remove(itemName);
            return false;
        }
        return true;
    }

    // ── Text utilities ──────────────────────────────────────────────────

    private static String toLegacyText(Component component) {
        StringBuilder sb = new StringBuilder();
        component.visit((style, part) -> {
            net.minecraft.network.chat.TextColor color = style.getColor();
            if (color != null) {
                int rgb = color.getValue();
                String code = "f";
                if (rgb == 16755200)
                    code = "6"; // Gold
                else if (rgb == 11141290)
                    code = "5"; // Dark Purple
                else if (rgb == 5636095)
                    code = "b"; // Aqua
                else if (rgb == 16733695)
                    code = "d"; // Light Purple
                else if (rgb == 5592405)
                    code = "8"; // Dark Gray
                else if (rgb == 11184810)
                    code = "7"; // Gray
                else if (rgb == 5592575)
                    code = "9"; // Blue
                else if (rgb == 5635925)
                    code = "a"; // Green
                else if (rgb == 16711680)
                    code = "c"; // Red
                else if (rgb == 16777045)
                    code = "e"; // Yellow
                sb.append("\u00A7").append(code);
            }
            if (style.isBold())
                sb.append("\u00A7l");
            if (style.isItalic())
                sb.append("\u00A7o");
            sb.append(part);
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }
}
