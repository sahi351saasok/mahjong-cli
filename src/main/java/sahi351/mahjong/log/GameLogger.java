package sahi351.mahjong.log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 標準出力とログファイル（logs/年月日時分秒.txt）へ同時出力する。
 */
public final class GameLogger implements AutoCloseable {

    private final PrintWriter fileWriter;

    public GameLogger(Path logsDir) {
        try {
            Files.createDirectories(logsDir);
            String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".txt";
            this.fileWriter = new PrintWriter(Files.newBufferedWriter(logsDir.resolve(fileName)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void log(String message) {
        System.out.println(message);
        fileWriter.println(message);
        fileWriter.flush();
    }

    public void blank() {
        log("");
    }

    @Override
    public void close() {
        fileWriter.close();
    }
}
