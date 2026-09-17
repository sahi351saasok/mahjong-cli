package sahi351.mahjong.hand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import sahi351.mahjong.tile.Tile;

/**
 * プレイヤーの手牌（門前の手牌＋副露）。
 */
public final class Hand {

    private final List<Tile> concealedTiles = new ArrayList<>();
    private final List<Meld> melds = new ArrayList<>();

    public void addTile(Tile tile) {
        concealedTiles.add(tile);
    }

    public void removeTile(Tile tile) {
        if (!concealedTiles.remove(tile)) {
            throw new IllegalStateException("手牌に存在しない牌を捨てようとした: " + tile);
        }
    }

    public void addMeld(Meld meld) {
        melds.add(meld);
    }

    /** 加槓時に既存のポンをカンへ差し替える。 */
    public void upgradePonToKakan(Meld pon, Meld kakan) {
        int idx = melds.indexOf(pon);
        if (idx < 0) {
            throw new IllegalStateException("加槓対象のポンが見つからない");
        }
        melds.set(idx, kakan);
    }

    public List<Tile> concealedTiles() {
        return Collections.unmodifiableList(concealedTiles);
    }

    public List<Tile> sortedConcealedTiles() {
        List<Tile> sorted = new ArrayList<>(concealedTiles);
        Collections.sort(sorted);
        return sorted;
    }

    public List<Meld> melds() {
        return Collections.unmodifiableList(melds);
    }

    public boolean isMenzen() {
        for (Meld m : melds) {
            if (m.type() != MeldType.ANKAN) {
                return false;
            }
        }
        return true;
    }

    public List<Tile> allTiles() {
        List<Tile> all = new ArrayList<>(concealedTiles);
        for (Meld m : melds) {
            all.addAll(m.tiles());
        }
        return all;
    }

    public int shanten() {
        return ShantenCalculator.shanten(concealedTiles, melds.size());
    }

    public boolean isTenpai() {
        return shanten() == 0;
    }

    public boolean isAgari() {
        return shanten() == -1;
    }

    public boolean hasTile(Tile tile) {
        return concealedTiles.contains(tile);
    }
}
