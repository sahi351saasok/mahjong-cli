package sahi351.mahjong.game;

public enum GameMode {
    HANCHAN,
    SINGLE_KYOKU;

    public static GameMode fromArg(String arg) {
        if (arg == null || arg.equals("a")) {
            return HANCHAN;
        }
        if (arg.equals("b")) {
            return SINGLE_KYOKU;
        }
        throw new IllegalArgumentException("不明なモード指定: " + arg + " (a または b を指定してください)");
    }
}
