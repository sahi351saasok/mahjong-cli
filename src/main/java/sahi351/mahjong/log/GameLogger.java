package sahi351.mahjong.log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 標準出力（局・半荘の要約）と CSV テキストログ（各プレイヤーの行動・結果の全履歴、
 * logs/年月日時分秒.csv）を別々に出力する。
 */
public final class GameLogger implements AutoCloseable {

    private static final String[] HEADER = {"局", "本場", "巡目", "プレイヤー", "行動", "詳細"};

    private final PrintWriter csvWriter;

    public GameLogger(Path logsDir) {
        try {
            Files.createDirectories(logsDir);
            String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
            this.csvWriter = new PrintWriter(Files.newBufferedWriter(logsDir.resolve(fileName)));
            writeRow(HEADER);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 標準出力にのみメッセージを表示する（局の開始・終了、半荘終了時の要約用）。 */
    public void console(String message) {
        System.out.println(message);
    }

    public void consoleBlank() {
        System.out.println();
    }

    /** 各プレイヤーの行動・結果を CSV テキストログにのみ記録する（打牌、鳴き、和了役など）。 */
    public void record(String kyoku, int honba, int turn, String player, String action, String detail) {
        writeRow(new String[] {kyoku, String.valueOf(honba), String.valueOf(turn), player, action, detail});
        csvWriter.flush();
    }

    private void writeRow(String[] fields) {
        csvWriter.println(Arrays.stream(fields).map(GameLogger::escape).collect(Collectors.joining(",")));
    }

    private static String escape(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    @Override
    public void close() {
        csvWriter.close();
    }
}
