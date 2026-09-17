package sahi351.mahjong.yaku;

public record YakuResult(String name, int han, boolean yakuman) {
    public static YakuResult normal(String name, int han) {
        return new YakuResult(name, han, false);
    }

    public static YakuResult yakuman(String name, int multiplier) {
        return new YakuResult(name, 13 * multiplier, true);
    }
}
