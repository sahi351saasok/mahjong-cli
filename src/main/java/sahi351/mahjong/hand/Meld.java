package sahi351.mahjong.hand;

import java.util.Collections;
import java.util.List;
import sahi351.mahjong.tile.Tile;

/**
 * 副露（ポン・チー・カン）または暗槓を表す。
 */
public final class Meld {

    private final MeldType type;
    private final List<Tile> tiles;
    private final Tile calledTile;
    private final int fromSeat;

    private Meld(MeldType type, List<Tile> tiles, Tile calledTile, int fromSeat) {
        this.type = type;
        this.tiles = List.copyOf(tiles);
        this.calledTile = calledTile;
        this.fromSeat = fromSeat;
    }

    public static Meld chi(List<Tile> tiles, Tile calledTile, int fromSeat) {
        return new Meld(MeldType.CHI, tiles, calledTile, fromSeat);
    }

    public static Meld pon(List<Tile> tiles, Tile calledTile, int fromSeat) {
        return new Meld(MeldType.PON, tiles, calledTile, fromSeat);
    }

    public static Meld ankan(List<Tile> tiles) {
        return new Meld(MeldType.ANKAN, tiles, null, -1);
    }

    public static Meld minkan(List<Tile> tiles, Tile calledTile, int fromSeat) {
        return new Meld(MeldType.MINKAN, tiles, calledTile, fromSeat);
    }

    public static Meld kakan(List<Tile> tiles, Tile calledTile, int fromSeat) {
        return new Meld(MeldType.KAKAN, tiles, calledTile, fromSeat);
    }

    public MeldType type() {
        return type;
    }

    public List<Tile> tiles() {
        return Collections.unmodifiableList(tiles);
    }

    public Tile calledTile() {
        return calledTile;
    }

    public int fromSeat() {
        return fromSeat;
    }

    public boolean isKan() {
        return type == MeldType.ANKAN || type == MeldType.MINKAN || type == MeldType.KAKAN;
    }

    public boolean isConcealed() {
        return type == MeldType.ANKAN;
    }

    public boolean isOpen() {
        return !isConcealed();
    }

    public Tile representativeTile() {
        return tiles.get(0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Tile t : tiles) {
            sb.append(t).append(' ');
        }
        return "[" + type + " " + sb.toString().trim() + "]";
    }
}
