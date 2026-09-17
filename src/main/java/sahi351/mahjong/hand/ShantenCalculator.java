package sahi351.mahjong.hand;

import java.util.List;
import sahi351.mahjong.tile.Tile;

/**
 * シャンテン数計算。通常形・七対子・国士無双のうち最小値を返す。
 * 標準形は「4面子1雀頭」を目標に、面子・搭子・雀頭の組み合わせを全探索して求める。
 */
public final class ShantenCalculator {

    private ShantenCalculator() {
    }

    public static int shanten(List<Tile> concealedTiles, int meldCount) {
        int[] counts = TileIndex.toCounts(concealedTiles);
        int best = standardShanten(counts, meldCount);
        if (meldCount == 0) {
            best = Math.min(best, chiitoiShanten(counts));
            best = Math.min(best, kokushiShanten(counts));
        }
        return best;
    }

    public static boolean isAgari(List<Tile> concealedTiles, int meldCount) {
        return shanten(concealedTiles, meldCount) == -1;
    }

    public static int standardShanten(int[] baseCounts, int meldCount) {
        int slots = 4 - meldCount;
        int[] counts = baseCounts.clone();
        int best = Integer.MIN_VALUE;

        // 頭（雀頭）なしの場合
        best = Math.max(best, scanMeldsAndTaatsu(counts, 0, 0, 0, slots));

        // 頭ありの場合、全ての候補インデックスを試す
        for (int i = 0; i < TileIndex.SIZE; i++) {
            if (counts[i] >= 2) {
                counts[i] -= 2;
                int value = scanMeldsAndTaatsu(counts, 0, 0, 0, slots) + 1;
                best = Math.max(best, value);
                counts[i] += 2;
            }
        }
        return 8 - 2 * meldCount - best;
    }

    private static int scanMeldsAndTaatsu(int[] counts, int index, int m, int t, int slots) {
        if (index >= TileIndex.SIZE || m + t >= slots) {
            return 2 * m + t;
        }
        if (counts[index] == 0) {
            return scanMeldsAndTaatsu(counts, index + 1, m, t, slots);
        }

        int best = scanMeldsAndTaatsu(counts, index + 1, m, t, slots);

        if (counts[index] >= 3) {
            counts[index] -= 3;
            best = Math.max(best, scanMeldsAndTaatsu(counts, index, m + 1, t, slots));
            counts[index] += 3;
        }

        int posInSuit = index % 9;
        boolean suited = index < 27;

        if (suited && posInSuit <= 6
                && counts[index] >= 1 && counts[index + 1] >= 1 && counts[index + 2] >= 1) {
            counts[index]--;
            counts[index + 1]--;
            counts[index + 2]--;
            best = Math.max(best, scanMeldsAndTaatsu(counts, index, m + 1, t, slots));
            counts[index]++;
            counts[index + 1]++;
            counts[index + 2]++;
        }

        if (counts[index] >= 2) {
            counts[index] -= 2;
            best = Math.max(best, scanMeldsAndTaatsu(counts, index, m, t + 1, slots));
            counts[index] += 2;
        }

        if (suited && posInSuit <= 7 && counts[index] >= 1 && counts[index + 1] >= 1) {
            counts[index]--;
            counts[index + 1]--;
            best = Math.max(best, scanMeldsAndTaatsu(counts, index, m, t + 1, slots));
            counts[index]++;
            counts[index + 1]++;
        }

        if (suited && posInSuit <= 6 && counts[index] >= 1 && counts[index + 2] >= 1) {
            counts[index]--;
            counts[index + 2]--;
            best = Math.max(best, scanMeldsAndTaatsu(counts, index, m, t + 1, slots));
            counts[index]++;
            counts[index + 2]++;
        }

        return best;
    }

    public static int chiitoiShanten(int[] counts) {
        int pairs = 0;
        int kinds = 0;
        for (int c : counts) {
            if (c > 0) {
                kinds++;
            }
            if (c >= 2) {
                pairs++;
            }
        }
        return 6 - pairs + Math.max(0, 7 - kinds);
    }

    private static final int[] YAOCHUU_INDICES = {0, 8, 9, 17, 18, 26, 27, 28, 29, 30, 31, 32, 33};

    public static int kokushiShanten(int[] counts) {
        int kinds = 0;
        boolean hasPair = false;
        for (int idx : YAOCHUU_INDICES) {
            if (counts[idx] > 0) {
                kinds++;
            }
            if (counts[idx] >= 2) {
                hasPair = true;
            }
        }
        return 13 - kinds - (hasPair ? 1 : 0);
    }
}
