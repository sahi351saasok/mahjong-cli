package sahi351.mahjong.yaku;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import sahi351.mahjong.hand.Group;
import sahi351.mahjong.hand.GroupType;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.hand.MeldType;
import sahi351.mahjong.hand.TileIndex;
import sahi351.mahjong.hand.WinContext;
import sahi351.mahjong.hand.WinDecomposer.Decomposition;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

/**
 * 役判定ロジック。標準形（4面子1雀頭）・七対子形・国士無双形それぞれについて
 * 役満と通常役を判定する。
 */
public final class YakuChecker {

    private YakuChecker() {
    }

    // ==================== 標準形 ====================

    public static List<Group> combinedGroups(Hand hand, Decomposition decomp) {
        List<Group> groups = new ArrayList<>(decomp.sets());
        for (Meld meld : hand.melds()) {
            GroupType type = meld.type() == MeldType.CHI ? GroupType.SEQUENCE : GroupType.TRIPLET;
            groups.add(new Group(type, meld.tiles()));
        }
        return groups;
    }

    public static boolean isRonCompletedTriplet(WinContext ctx, Group group) {
        return ctx.isRon() && group.type() == GroupType.TRIPLET
                && group.representative().isSameKind(ctx.winningTile());
    }

    private static boolean isAnkou(WinContext ctx, Group group, boolean fromMeld) {
        if (group.type() != GroupType.TRIPLET) {
            return false;
        }
        if (fromMeld) {
            return false; // ポン・明槓は暗刻ではない（暗槓は呼び出し元で別扱い）
        }
        return !isRonCompletedTriplet(ctx, group);
    }

    public static int countAnkou(WinContext ctx, Hand hand, Decomposition decomp) {
        int count = 0;
        for (Group g : decomp.sets()) {
            if (isAnkou(ctx, g, false)) {
                count++;
            }
        }
        for (Meld m : hand.melds()) {
            if (m.type() == MeldType.ANKAN) {
                count++;
            }
        }
        return count;
    }

    public static List<YakuResult> checkYakumanStandard(WinContext ctx, Decomposition decomp) {
        List<YakuResult> results = new ArrayList<>();
        Hand hand = ctx.hand();
        List<Group> groups = combinedGroups(hand, decomp);
        List<Tile> allTiles = hand.allTiles();

        if (ctx.isTenhou()) {
            results.add(YakuResult.yakuman("天和", 1));
        }
        if (ctx.isChiihou()) {
            results.add(YakuResult.yakuman("地和", 1));
        }

        int ankouCount = countAnkou(ctx, hand, decomp);
        if (ankouCount == 4) {
            boolean tanki = decomp.pair().representative().isSameKind(ctx.winningTile());
            results.add(YakuResult.yakuman("四暗刻" + (tanki ? "単騎" : ""), tanki ? 2 : 1));
        }

        long dragonTriplets = groups.stream()
                .filter(g -> g.type() == GroupType.TRIPLET && g.representative().isDragonTile())
                .count();
        if (dragonTriplets == 3) {
            results.add(YakuResult.yakuman("大三元", 1));
        }

        long windTriplets = groups.stream()
                .filter(g -> g.type() == GroupType.TRIPLET && g.representative().isWindTile())
                .count();
        boolean pairIsWind = decomp.pair() != null && decomp.pair().representative().isWindTile();
        if (windTriplets == 4) {
            results.add(YakuResult.yakuman("大四喜", 2));
        } else if (windTriplets == 3 && pairIsWind) {
            results.add(YakuResult.yakuman("小四喜", 1));
        }

        if (allTiles.stream().allMatch(Tile::isHonor)) {
            results.add(YakuResult.yakuman("字一色", 1));
        }

        if (isRyuuiisou(allTiles)) {
            results.add(YakuResult.yakuman("緑一色", 1));
        }

        if (allTiles.stream().allMatch(Tile::isTerminal)) {
            results.add(YakuResult.yakuman("清老頭", 1));
        }

        if (hand.melds().isEmpty()) {
            YakuResult chuuren = checkChuurenpoutou(ctx, allTiles);
            if (chuuren != null) {
                results.add(chuuren);
            }
        }

        long kanCount = hand.melds().stream().filter(Meld::isKan).count();
        if (kanCount == 4) {
            results.add(YakuResult.yakuman("四槓子", 1));
        }

        return results;
    }

    private static boolean isRyuuiisou(List<Tile> tiles) {
        for (Tile t : tiles) {
            boolean ok = (t.suit() == Suit.SOUZU && (t.rank() == 2 || t.rank() == 3 || t.rank() == 4
                    || t.rank() == 6 || t.rank() == 8))
                    || (t.isHonor() && t.rank() == Tile.HATSU);
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static YakuResult checkChuurenpoutou(WinContext ctx, List<Tile> allTiles) {
        Suit suit = allTiles.get(0).suit();
        if (suit == Suit.JIHAI) {
            return null;
        }
        for (Tile t : allTiles) {
            if (t.suit() != suit) {
                return null;
            }
        }
        int[] counts = new int[10];
        for (Tile t : allTiles) {
            counts[t.rank()]++;
        }
        if (counts[1] < 3 || counts[9] < 3) {
            return null;
        }
        for (int r = 2; r <= 8; r++) {
            if (counts[r] < 1) {
                return null;
            }
        }
        int total = 0;
        for (int r = 1; r <= 9; r++) {
            total += counts[r];
        }
        if (total != 14) {
            return null;
        }
        int[] pure = {0, 3, 1, 1, 1, 1, 1, 1, 1, 3};
        int[] preWin = counts.clone();
        preWin[ctx.winningTile().rank()]--;
        boolean isPure = true;
        for (int r = 1; r <= 9; r++) {
            if (preWin[r] != pure[r]) {
                isPure = false;
                break;
            }
        }
        return YakuResult.yakuman("九蓮宝燈" + (isPure ? "(純正)" : ""), isPure ? 2 : 1);
    }

    public static List<YakuResult> checkNormalStandard(WinContext ctx, Decomposition decomp) {
        List<YakuResult> results = new ArrayList<>();
        Hand hand = ctx.hand();
        List<Group> groups = combinedGroups(hand, decomp);
        boolean menzen = hand.isMenzen();
        boolean fullyConcealed = hand.melds().isEmpty();

        if (ctx.isDoubleRiichi()) {
            results.add(YakuResult.normal("ダブル立直", 2));
        } else if (ctx.isRiichi()) {
            results.add(YakuResult.normal("立直", 1));
        }
        if (ctx.isIppatsu()) {
            results.add(YakuResult.normal("一発", 1));
        }
        if (ctx.isTsumo() && menzen) {
            results.add(YakuResult.normal("門前清自摸和", 1));
        }
        if (ctx.isHaitei()) {
            results.add(YakuResult.normal("海底摸月", 1));
        }
        if (ctx.isHoutei()) {
            results.add(YakuResult.normal("河底撈魚", 1));
        }
        if (ctx.isRinshan()) {
            results.add(YakuResult.normal("嶺上開花", 1));
        }
        if (ctx.isChankan()) {
            results.add(YakuResult.normal("槍槓", 1));
        }

        boolean allSequences = groups.stream().allMatch(g -> g.type() == GroupType.SEQUENCE);
        boolean pairIsYakuhai = isYakuhaiTile(decomp.pair().representative(), ctx);
        if (menzen && allSequences && !pairIsYakuhai && waitType(decomp, ctx) == WaitType.RYANMEN) {
            results.add(YakuResult.normal("平和", 1));
        }

        List<Tile> allTiles = hand.allTiles();
        if (allTiles.stream().noneMatch(Tile::isYaochuu)) {
            results.add(YakuResult.normal("断幺九", 1));
        }

        Map<String, Integer> yakuhaiHan = new HashMap<>();
        for (Group g : groups) {
            if (g.type() != GroupType.TRIPLET) {
                continue;
            }
            Tile t = g.representative();
            if (t.isDragonTile()) {
                yakuhaiHan.merge("役牌(" + t + ")", 1, Integer::sum);
            } else if (t.isWindTile()) {
                if (t.rank() == ctx.seatWind().tileRank()) {
                    yakuhaiHan.merge("役牌(自風 " + t + ")", 1, Integer::sum);
                }
                if (t.rank() == ctx.roundWind().tileRank()) {
                    yakuhaiHan.merge("役牌(場風 " + t + ")", 1, Integer::sum);
                }
            }
        }
        yakuhaiHan.forEach((name, han) -> results.add(YakuResult.normal(name, han)));

        if (fullyConcealed) {
            int duplicateSeqPairs = countDuplicateSequencePairs(decomp);
            if (duplicateSeqPairs == 2) {
                results.add(YakuResult.normal("二盃口", 3));
            } else if (duplicateSeqPairs == 1) {
                results.add(YakuResult.normal("一盃口", 1));
            }
        }

        Integer sanshokuDoujunHan = checkSanshokuDoujun(groups, menzen);
        if (sanshokuDoujunHan != null) {
            results.add(YakuResult.normal("三色同順", sanshokuDoujunHan));
        }

        Integer ittsuuHan = checkIttsuu(groups, menzen);
        if (ittsuuHan != null) {
            results.add(YakuResult.normal("一気通貫", ittsuuHan));
        }

        boolean allInvolveYaochuu = groups.stream().allMatch(Group::isTerminalOrHonorInvolved)
                && decomp.pair().isTerminalOrHonorInvolved();
        if (allInvolveYaochuu) {
            boolean anyHonor = groups.stream().anyMatch(g -> g.tiles().stream().anyMatch(Tile::isHonor))
                    || decomp.pair().tiles().stream().anyMatch(Tile::isHonor);
            if (anyHonor) {
                results.add(YakuResult.normal("混全帯幺九", menzen ? 2 : 1));
            } else {
                results.add(YakuResult.normal("純全帯幺九", menzen ? 3 : 2));
            }
        }

        boolean allTriplets = groups.stream().allMatch(g -> g.type() == GroupType.TRIPLET);
        if (allTriplets) {
            results.add(YakuResult.normal("対々和", 2));
        }

        int ankouCount = countAnkou(ctx, hand, decomp);
        if (ankouCount == 3) {
            results.add(YakuResult.normal("三暗刻", 2));
        }

        long kanCount = hand.melds().stream().filter(Meld::isKan).count();
        if (kanCount == 3) {
            results.add(YakuResult.normal("三槓子", 2));
        }

        if (checkSanshokuDoukou(groups)) {
            results.add(YakuResult.normal("三色同刻", 2));
        }

        boolean allTripletOrPairYaochuu = groups.stream()
                .allMatch(g -> g.type() == GroupType.TRIPLET && g.isAllTerminalOrHonor())
                && decomp.pair().isAllTerminalOrHonor();
        boolean hasHonorGroup = groups.stream().anyMatch(g -> g.representative().isHonor())
                || decomp.pair().representative().isHonor();
        boolean hasTerminalGroup = groups.stream().anyMatch(g -> g.representative().isTerminal())
                || decomp.pair().representative().isTerminal();
        if (allTripletOrPairYaochuu && hasHonorGroup && hasTerminalGroup) {
            results.add(YakuResult.normal("混老頭", 2));
        }

        long dragonTriplets = groups.stream()
                .filter(g -> g.type() == GroupType.TRIPLET && g.representative().isDragonTile())
                .count();
        boolean pairIsDragon = decomp.pair().representative().isDragonTile();
        if (dragonTriplets == 2 && pairIsDragon) {
            results.add(YakuResult.normal("小三元", 2));
        }

        HonitsuKind honitsu = checkHonitsuChinitsu(allTiles);
        if (honitsu == HonitsuKind.HONITSU) {
            results.add(YakuResult.normal("混一色", menzen ? 3 : 2));
        } else if (honitsu == HonitsuKind.CHINITSU) {
            results.add(YakuResult.normal("清一色", menzen ? 6 : 5));
        }

        return results;
    }

    public static WaitType waitType(Decomposition decomp, WinContext ctx) {
        Tile win = ctx.winningTile();
        if (decomp.pair().representative().isSameKind(win)) {
            return WaitType.TANKI;
        }
        for (Group g : decomp.sets()) {
            if (g.type() == GroupType.TRIPLET && g.representative().isSameKind(win)) {
                return WaitType.SHANPON;
            }
            if (g.type() == GroupType.SEQUENCE) {
                List<Tile> tiles = g.tiles();
                for (int i = 0; i < 3; i++) {
                    if (tiles.get(i).isSameKind(win)) {
                        int seqStart = tiles.get(0).rank();
                        if (i == 1) {
                            return WaitType.KANCHAN;
                        }
                        boolean penchan = (seqStart == 1 && i == 2) || (seqStart == 7 && i == 0);
                        return penchan ? WaitType.PENCHAN : WaitType.RYANMEN;
                    }
                }
            }
        }
        return WaitType.RYANMEN;
    }

    private static boolean isYakuhaiTile(Tile t, WinContext ctx) {
        if (t.isDragonTile()) {
            return true;
        }
        if (t.isWindTile()) {
            return t.rank() == ctx.seatWind().tileRank() || t.rank() == ctx.roundWind().tileRank();
        }
        return false;
    }

    private static int countDuplicateSequencePairs(Decomposition decomp) {
        List<Group> sequences = decomp.sets().stream()
                .filter(g -> g.type() == GroupType.SEQUENCE)
                .toList();
        Map<String, Integer> keyCounts = new HashMap<>();
        for (Group g : sequences) {
            String key = g.tiles().get(0).suit() + ":" + g.tiles().get(0).rank();
            keyCounts.merge(key, 1, Integer::sum);
        }
        int pairs = 0;
        for (int c : keyCounts.values()) {
            pairs += c / 2;
        }
        return pairs;
    }

    private static Integer checkSanshokuDoujun(List<Group> groups, boolean menzen) {
        for (int start = 1; start <= 7; start++) {
            boolean m = false, p = false, s = false;
            for (Group g : groups) {
                if (g.type() != GroupType.SEQUENCE) {
                    continue;
                }
                Tile first = g.tiles().get(0);
                if (first.rank() != start) {
                    continue;
                }
                if (first.suit() == Suit.MANZU) m = true;
                else if (first.suit() == Suit.PINZU) p = true;
                else if (first.suit() == Suit.SOUZU) s = true;
            }
            if (m && p && s) {
                return menzen ? 2 : 1;
            }
        }
        return null;
    }

    private static boolean checkSanshokuDoukou(List<Group> groups) {
        for (int rank = 1; rank <= 9; rank++) {
            boolean m = false, p = false, s = false;
            for (Group g : groups) {
                if (g.type() != GroupType.TRIPLET) {
                    continue;
                }
                Tile t = g.representative();
                if (t.isHonor() || t.rank() != rank) {
                    continue;
                }
                if (t.suit() == Suit.MANZU) m = true;
                else if (t.suit() == Suit.PINZU) p = true;
                else if (t.suit() == Suit.SOUZU) s = true;
            }
            if (m && p && s) {
                return true;
            }
        }
        return false;
    }

    private static Integer checkIttsuu(List<Group> groups, boolean menzen) {
        for (Suit suit : new Suit[]{Suit.MANZU, Suit.PINZU, Suit.SOUZU}) {
            boolean s1 = false, s4 = false, s7 = false;
            for (Group g : groups) {
                if (g.type() != GroupType.SEQUENCE) {
                    continue;
                }
                Tile first = g.tiles().get(0);
                if (first.suit() != suit) {
                    continue;
                }
                if (first.rank() == 1) s1 = true;
                else if (first.rank() == 4) s4 = true;
                else if (first.rank() == 7) s7 = true;
            }
            if (s1 && s4 && s7) {
                return menzen ? 2 : 1;
            }
        }
        return null;
    }

    private enum HonitsuKind {NONE, HONITSU, CHINITSU}

    private static HonitsuKind checkHonitsuChinitsu(List<Tile> allTiles) {
        Set<Suit> suits = new HashSet<>();
        boolean hasHonor = false;
        for (Tile t : allTiles) {
            if (t.isHonor()) {
                hasHonor = true;
            } else {
                suits.add(t.suit());
            }
        }
        if (suits.size() != 1) {
            return HonitsuKind.NONE;
        }
        return hasHonor ? HonitsuKind.HONITSU : HonitsuKind.CHINITSU;
    }

    // ==================== 七対子 ====================

    public static List<YakuResult> checkYakumanChiitoi(WinContext ctx) {
        List<Tile> tiles = ctx.hand().allTiles();
        List<YakuResult> results = new ArrayList<>();
        if (ctx.isTenhou()) {
            results.add(YakuResult.yakuman("天和", 1));
        }
        if (ctx.isChiihou()) {
            results.add(YakuResult.yakuman("地和", 1));
        }
        if (tiles.stream().allMatch(Tile::isHonor)) {
            results.add(YakuResult.yakuman("字一色", 1));
        }
        return results;
    }

    public static List<YakuResult> checkNormalChiitoi(WinContext ctx) {
        List<YakuResult> results = new ArrayList<>();
        List<Tile> tiles = ctx.hand().allTiles();

        if (ctx.isDoubleRiichi()) {
            results.add(YakuResult.normal("ダブル立直", 2));
        } else if (ctx.isRiichi()) {
            results.add(YakuResult.normal("立直", 1));
        }
        if (ctx.isIppatsu()) {
            results.add(YakuResult.normal("一発", 1));
        }
        if (ctx.isTsumo()) {
            results.add(YakuResult.normal("門前清自摸和", 1));
        }
        if (ctx.isHaitei()) {
            results.add(YakuResult.normal("海底摸月", 1));
        }
        if (ctx.isHoutei()) {
            results.add(YakuResult.normal("河底撈魚", 1));
        }

        results.add(YakuResult.normal("七対子", 2));

        if (tiles.stream().noneMatch(Tile::isYaochuu)) {
            results.add(YakuResult.normal("断幺九", 1));
        }

        HonitsuKind honitsu = checkHonitsuChinitsu(tiles);
        if (honitsu == HonitsuKind.HONITSU) {
            results.add(YakuResult.normal("混一色", 3));
        } else if (honitsu == HonitsuKind.CHINITSU) {
            results.add(YakuResult.normal("清一色", 6));
        }
        return results;
    }

    // ==================== 国士無双 ====================

    public static List<YakuResult> checkKokushi(WinContext ctx) {
        List<Tile> tiles = ctx.hand().allTiles();
        Map<Integer, Integer> byIndex = new HashMap<>();
        for (Tile t : tiles) {
            byIndex.merge(TileIndex.of(t), 1, Integer::sum);
        }
        Map<Integer, Integer> preWin = new HashMap<>(byIndex);
        int winIdx = TileIndex.of(ctx.winningTile());
        preWin.merge(winIdx, -1, Integer::sum);
        boolean thirteenWait = preWin.values().stream().noneMatch(v -> v >= 2);

        List<YakuResult> results = new ArrayList<>();
        if (ctx.isTenhou()) {
            results.add(YakuResult.yakuman("天和", 1));
        }
        if (ctx.isChiihou()) {
            results.add(YakuResult.yakuman("地和", 1));
        }
        results.add(YakuResult.yakuman("国士無双" + (thirteenWait ? "十三面待ち" : ""), thirteenWait ? 2 : 1));
        return results;
    }
}
