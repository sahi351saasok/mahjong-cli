package sahi351.mahjong.score;

public enum ScoreTier {
    NORMAL(""),
    MANGAN("満貫"),
    HANEMAN("跳満"),
    BAIMAN("倍満"),
    SANBAIMAN("三倍満"),
    YAKUMAN("役満");

    private final String label;

    ScoreTier(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
