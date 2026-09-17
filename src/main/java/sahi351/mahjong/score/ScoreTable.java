package sahi351.mahjong.score;

/**
 * 翻・符から基本点を算出する。満貫〜役満の判定を含む。
 */
public final class ScoreTable {

    private ScoreTable() {
    }

    public record BasePoints(int value, ScoreTier tier) {
    }

    public static BasePoints basePoints(int han, int fu) {
        if (han >= 13) {
            return new BasePoints(8000 * (han / 13), ScoreTier.YAKUMAN);
        }
        if (han >= 11) {
            return new BasePoints(6000, ScoreTier.SANBAIMAN);
        }
        if (han >= 8) {
            return new BasePoints(4000, ScoreTier.BAIMAN);
        }
        if (han >= 6) {
            return new BasePoints(3000, ScoreTier.HANEMAN);
        }
        if (han >= 5) {
            return new BasePoints(2000, ScoreTier.MANGAN);
        }
        int raw = fu * (1 << (2 + han));
        if (raw >= 2000) {
            return new BasePoints(2000, ScoreTier.MANGAN);
        }
        return new BasePoints(raw, ScoreTier.NORMAL);
    }

    public static int roundUpTo100(int value) {
        return ((value + 99) / 100) * 100;
    }
}
