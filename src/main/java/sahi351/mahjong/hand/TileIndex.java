package sahi351.mahjong.hand;

import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

/**
 * 牌の種類を 0-33 のインデックスに変換するユーティリティ。
 * 0-8: 萬子1-9, 9-17: 筒子1-9, 18-26: 索子1-9, 27-33: 東南西北白發中
 */
public final class TileIndex {

    public static final int SIZE = 34;

    private TileIndex() {
    }

    public static int of(Tile tile) {
        return switch (tile.suit()) {
            case MANZU -> tile.rank() - 1;
            case PINZU -> 9 + tile.rank() - 1;
            case SOUZU -> 18 + tile.rank() - 1;
            case JIHAI -> 27 + tile.rank() - 1;
        };
    }

    public static Tile toTile(int index) {
        if (index < 9) {
            return Tile.of(Suit.MANZU, index + 1);
        } else if (index < 18) {
            return Tile.of(Suit.PINZU, index - 9 + 1);
        } else if (index < 27) {
            return Tile.of(Suit.SOUZU, index - 18 + 1);
        } else {
            return Tile.of(Suit.JIHAI, index - 27 + 1);
        }
    }

    public static boolean isHonor(int index) {
        return index >= 27;
    }

    /** 同じスート内かつ数牌であり、順子の起点になり得るか（8,9からは順子を作れない）。 */
    public static boolean canStartSequence(int index) {
        if (isHonor(index)) {
            return false;
        }
        int posInSuit = index % 9;
        return posInSuit <= 6;
    }

    public static int[] toCounts(Iterable<Tile> tiles) {
        int[] counts = new int[SIZE];
        for (Tile t : tiles) {
            counts[of(t)]++;
        }
        return counts;
    }
}
