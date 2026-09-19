package sahi351.mahjong.game;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FinalPointCalculatorTest {

    private static final double EPS = 1e-9;

    @Test
    void subtractsReturnPointsAndAddsRankBonus() {
        // 順位は入力の並びとは無関係（座席0=2位, 1=1位, 2=4位, 3=3位）
        double[] points = FinalPointCalculator.calculate(new int[] {28000, 42000, 12000, 18000});

        // (28000-30000)/1000+10, (42000-30000)/1000+50, (12000-30000)/1000-30, (18000-30000)/1000-10
        assertArrayEquals(new double[] {8.0, 62.0, -48.0, -22.0}, points, EPS);
    }

    @Test
    void allEqualScoresShareTheBonusEvenly() {
        double[] points = FinalPointCalculator.calculate(new int[] {25000, 25000, 25000, 25000});

        // ボーナス合計 (50+10-10-30)/4 = 5、各自 (25000-30000)/1000 = -5
        assertArrayEquals(new double[] {0.0, 0.0, 0.0, 0.0}, points, EPS);
    }

    @Test
    void tiedPlayersSplitTheirRankBonuses() {
        // 1位と2位が同点 → (50+10)/2 = 30 ずつ、3位と4位が同点 → (-10-30)/2 = -20 ずつ
        double[] points = FinalPointCalculator.calculate(new int[] {35000, 15000, 35000, 15000});

        assertEquals(5.0 + 30.0, points[0], EPS);
        assertEquals(-15.0 - 20.0, points[1], EPS);
        assertEquals(5.0 + 30.0, points[2], EPS);
        assertEquals(-15.0 - 20.0, points[3], EPS);
    }

    @Test
    void partialTieOnlySplitsAffectedRanks() {
        // 2位と3位が同点 → (10-10)/2 = 0
        double[] points = FinalPointCalculator.calculate(new int[] {40000, 25000, 25000, 10000});

        assertArrayEquals(new double[] {60.0, -5.0, -5.0, -50.0}, points, EPS);
    }

    @Test
    void totalPointsAreZeroWhenScoresSumTo100000() {
        double[] points = FinalPointCalculator.calculate(new int[] {51300, 22700, 14100, 11900});

        assertEquals(0.0, points[0] + points[1] + points[2] + points[3], EPS);
    }

    @Test
    void rejectsWrongNumberOfPlayers() {
        assertThrows(IllegalArgumentException.class, () -> FinalPointCalculator.calculate(new int[] {25000, 25000}));
    }
}
