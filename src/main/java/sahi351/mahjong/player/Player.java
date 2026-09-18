package sahi351.mahjong.player;

import java.util.ArrayList;
import java.util.List;
import sahi351.mahjong.ai.PlayerStrategy;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.tile.Tile;

/**
 * 対局中のプレイヤー状態（手牌・持ち点・リーチ状態など）。
 */
public final class Player {
    private final String name;
    private final PlayerStrategy strategy;
    private final int seatIndex;
    private Wind seatWind;
    private int points;
    private Hand hand = new Hand();
    private boolean riichi;
    private boolean doubleRiichi;
    private boolean doubleRiichiEligible = true;
    private boolean ippatsuActive;
    private int riichiDeclaredTurn = -1;
    private final List<Tile> discards = new ArrayList<>();
    private final List<Boolean> discardRiichiTile = new ArrayList<>();

    public Player(String name, PlayerStrategy strategy, int seatIndex) {
        this.name = name;
        this.strategy = strategy;
        this.seatIndex = seatIndex;
        this.points = 25000;
    }

    public String name() {
        return name;
    }

    public PlayerStrategy strategy() {
        return strategy;
    }

    public int seatIndex() {
        return seatIndex;
    }

    public Wind seatWind() {
        return seatWind;
    }

    public void setSeatWind(Wind seatWind) {
        this.seatWind = seatWind;
    }

    public int points() {
        return points;
    }

    public void addPoints(int delta) {
        this.points += delta;
    }

    public Hand hand() {
        return hand;
    }

    public boolean isRiichi() {
        return riichi;
    }

    public void declareRiichi(int turn, boolean asDoubleRiichi) {
        this.riichi = true;
        this.doubleRiichi = asDoubleRiichi;
        this.ippatsuActive = true;
        this.riichiDeclaredTurn = turn;
    }

    public boolean isDoubleRiichi() {
        return doubleRiichi;
    }

    public boolean isDoubleRiichiEligible() {
        return doubleRiichiEligible;
    }

    public void disqualifyDoubleRiichi() {
        this.doubleRiichiEligible = false;
    }

    public int riichiDeclaredTurn() {
        return riichiDeclaredTurn;
    }

    public boolean isIppatsuActive() {
        return ippatsuActive;
    }

    public void clearIppatsu() {
        this.ippatsuActive = false;
    }

    public List<Tile> discards() {
        return discards;
    }

    public List<Boolean> discardRiichiTile() {
        return discardRiichiTile;
    }

    public void addDiscard(Tile tile, boolean isRiichiTile) {
        discards.add(tile);
        discardRiichiTile.add(isRiichiTile);
    }

    public boolean isFuriten(List<Tile> waits) {
        for (Tile discarded : discards) {
            for (Tile wait : waits) {
                if (discarded.isSameKind(wait)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void resetForNewKyoku() {
        hand = new Hand();
        riichi = false;
        doubleRiichi = false;
        doubleRiichiEligible = true;
        ippatsuActive = false;
        riichiDeclaredTurn = -1;
        discards.clear();
        discardRiichiTile.clear();
    }
}
