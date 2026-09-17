package sahi351.mahjong.hand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sahi351.mahjong.tile.Tile;

/**
 * 面子・雀頭を構成する牌のまとまり（副露は含まない、手牌内で完成した組）。
 */
public final class Group {

    private final GroupType type;
    private final List<Tile> tiles;

    public Group(GroupType type, List<Tile> tiles) {
        this.type = type;
        this.tiles = List.copyOf(tiles);
    }

    public GroupType type() {
        return type;
    }

    public List<Tile> tiles() {
        return Collections.unmodifiableList(tiles);
    }

    public boolean containsRedFive() {
        return tiles.stream().anyMatch(Tile::isRedFive);
    }

    public boolean isTerminalOrHonorInvolved() {
        return tiles.stream().anyMatch(Tile::isYaochuu);
    }

    public boolean isAllTerminalOrHonor() {
        return tiles.stream().allMatch(Tile::isYaochuu);
    }

    public Tile representative() {
        return tiles.get(0);
    }

    /** その組が使用している牌種のインデックス一覧（順子なら3種、刻子・雀頭なら1種）。 */
    public List<Integer> tileIndices() {
        List<Integer> result = new ArrayList<>();
        for (Tile t : tiles) {
            result.add(TileIndex.of(t));
        }
        return result;
    }

    @Override
    public String toString() {
        return type + tiles.toString();
    }
}
