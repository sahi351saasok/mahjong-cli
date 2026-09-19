package sahi351.mahjong.game;

import java.util.Arrays;

/**
 * 対局終了時の最終ポイントを算出する。
 *
 * <p>各プレイヤーの持ち点から返し点（30000点）を差し引き、1000点を1ポイントとして換算したうえで、
 * 順位に応じたポイント（1位+50、2位+10、3位-10、4位-30）を加算する。
 * 同点のプレイヤーが複数いる場合は、該当する順位の加算ポイントを人数で按分する。
 */
public final class FinalPointCalculator {
    static final int RETURN_POINTS = 30000;
    static final double POINTS_PER_UNIT = 1000.0;
    private static final double[] RANK_BONUS = {50, 10, -10, -30};

    private FinalPointCalculator() {
    }

    /**
     * @param finalScores 各プレイヤーの最終持ち点（4人分）
     * @return 引数と同じ並びの各プレイヤーの最終ポイント
     */
    public static double[] calculate(int[] finalScores) {
        if (finalScores.length != RANK_BONUS.length) {
            throw new IllegalArgumentException("プレイヤーは" + RANK_BONUS.length + "人である必要があります");
        }
        Integer[] order = new Integer[finalScores.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, (a, b) -> Integer.compare(finalScores[b], finalScores[a]));

        double[] result = new double[finalScores.length];
        int i = 0;
        while (i < order.length) {
            int j = i;
            while (j + 1 < order.length && finalScores[order[j + 1]] == finalScores[order[i]]) {
                j++;
            }
            double bonusSum = 0;
            for (int k = i; k <= j; k++) {
                bonusSum += RANK_BONUS[k];
            }
            double bonusEach = bonusSum / (j - i + 1);
            for (int k = i; k <= j; k++) {
                int player = order[k];
                result[player] = (finalScores[player] - RETURN_POINTS) / POINTS_PER_UNIT + bonusEach;
            }
            i = j + 1;
        }
        return result;
    }
}
