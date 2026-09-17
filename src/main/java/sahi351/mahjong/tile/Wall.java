package sahi351.mahjong.tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 136枚の牌山。王牌14枚（ドラ表示牌・裏ドラ表示牌・嶺上牌を含む）を確保し、
 * 残り122枚を通常のツモ用の牌として扱う。
 */
public final class Wall {

    private static final int DEAD_WALL_SIZE = 14;
    private static final int MAX_KAN_DORA = 4;

    private final List<Tile> tiles;
    private int drawIndex;
    private final List<Tile> deadWall;
    private int doraIndicatorCount;
    private int rinshanDrawn;

    public Wall(Random random) {
        this.tiles = buildAllTiles();
        Collections.shuffle(this.tiles, random);
        this.deadWall = new ArrayList<>(this.tiles.subList(0, DEAD_WALL_SIZE));
        this.tiles.subList(0, DEAD_WALL_SIZE).clear();
        this.drawIndex = 0;
        this.doraIndicatorCount = 1;
        this.rinshanDrawn = 0;
    }

    private static List<Tile> buildAllTiles() {
        List<Tile> result = new ArrayList<>(136);
        for (Suit suit : new Suit[]{Suit.MANZU, Suit.PINZU, Suit.SOUZU}) {
            for (int rank = 1; rank <= 9; rank++) {
                for (int i = 0; i < 4; i++) {
                    if (rank == 5 && i == 0) {
                        result.add(Tile.redFive(suit));
                    } else {
                        result.add(Tile.of(suit, rank));
                    }
                }
            }
        }
        for (int rank = 1; rank <= 7; rank++) {
            for (int i = 0; i < 4; i++) {
                result.add(Tile.of(Suit.JIHAI, rank));
            }
        }
        return result;
    }

    public boolean hasNextDraw() {
        return remainingLiveTiles() > 0;
    }

    /** 残りツモ可能枚数（嶺上牌を除く）。 */
    public int remainingLiveTiles() {
        return tiles.size() - drawIndex;
    }

    public Tile draw() {
        if (!hasNextDraw()) {
            throw new IllegalStateException("山に残り牌がありません");
        }
        return tiles.get(drawIndex++);
    }

    /** カン成立時の嶺上牌をツモる。 */
    public Tile drawRinshan() {
        if (rinshanDrawn >= MAX_KAN_DORA) {
            throw new IllegalStateException("嶺上牌がありません");
        }
        Tile tile = deadWall.get(rinshanDrawn);
        rinshanDrawn++;
        return tile;
    }

    /** カン成立時に新ドラ表示牌を1枚追加する。 */
    public void revealNewDoraIndicator() {
        if (doraIndicatorCount >= 1 + MAX_KAN_DORA) {
            throw new IllegalStateException("これ以上ドラ表示牌をめくれません");
        }
        doraIndicatorCount++;
    }

    public List<Tile> doraIndicators() {
        List<Tile> result = new ArrayList<>();
        for (int i = 0; i < doraIndicatorCount; i++) {
            result.add(deadWall.get(4 + i));
        }
        return result;
    }

    public List<Tile> uraDoraIndicators() {
        List<Tile> result = new ArrayList<>();
        for (int i = 0; i < doraIndicatorCount; i++) {
            result.add(deadWall.get(9 + i));
        }
        return result;
    }
}
