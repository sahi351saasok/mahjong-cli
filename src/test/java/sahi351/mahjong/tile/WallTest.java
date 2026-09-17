package sahi351.mahjong.tile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class WallTest {

    @Test
    void wallContains136TilesWithCorrectComposition() {
        Wall wall = new Wall(new Random(42));
        Map<String, Integer> counts = new HashMap<>();
        int total = 0;
        assertEquals(122, wall.remainingLiveTiles());
        while (wall.hasNextDraw()) {
            Tile t = wall.draw();
            counts.merge(t.suit() + ":" + t.rank(), 1, Integer::sum);
            total++;
        }
        assertEquals(122, total);
        for (int i = 0; i < 4; i++) {
            wall.drawRinshan();
            total++;
        }
        // 王牌14枚のうち、嶺上牌4枚は上で消費済み。残りはドラ表示牌5枚+裏ドラ表示牌5枚。
        wall.revealNewDoraIndicator();
        wall.revealNewDoraIndicator();
        wall.revealNewDoraIndicator();
        wall.revealNewDoraIndicator();
        total += wall.doraIndicators().size() + wall.uraDoraIndicators().size();
        assertEquals(136, total);
    }

    @Test
    void doraIndicatorsStartWithOneAndCanGrowWithKan() {
        Wall wall = new Wall(new Random(1));
        assertEquals(1, wall.doraIndicators().size());
        for (int i = 1; i <= 4; i++) {
            wall.revealNewDoraIndicator();
            assertEquals(1 + i, wall.doraIndicators().size());
            assertEquals(1 + i, wall.uraDoraIndicators().size());
        }
    }

    @Test
    void nextForDoraWrapsAround() {
        assertEquals(Tile.of(Suit.MANZU, 1), Tile.of(Suit.MANZU, 9).nextForDora());
        assertEquals(Tile.of(Suit.JIHAI, Tile.EAST), Tile.of(Suit.JIHAI, Tile.NORTH).nextForDora());
        assertEquals(Tile.of(Suit.JIHAI, Tile.HAKU), Tile.of(Suit.JIHAI, Tile.CHUN).nextForDora());
    }

    @Test
    void remainingLiveTilesDecreasesOnDraw() {
        Wall wall = new Wall(new Random(7));
        int before = wall.remainingLiveTiles();
        wall.draw();
        assertTrue(wall.remainingLiveTiles() == before - 1);
    }
}
