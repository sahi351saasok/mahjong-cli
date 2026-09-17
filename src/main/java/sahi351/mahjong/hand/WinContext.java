package sahi351.mahjong.hand;

import java.util.List;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.tile.Tile;

/**
 * 和了判定・役判定・点数計算に必要な文脈情報。
 */
public final class WinContext {
    private final Hand hand;
    private final Tile winningTile;
    private final boolean tsumo;
    private final Wind roundWind;
    private final Wind seatWind;
    private final boolean riichi;
    private final boolean doubleRiichi;
    private final boolean ippatsu;
    private final boolean haitei;
    private final boolean houtei;
    private final boolean rinshan;
    private final boolean chankan;
    private final boolean tenhou;
    private final boolean chiihou;
    private final List<Tile> doraIndicators;
    private final List<Tile> uraDoraIndicators;

    public WinContext(Hand hand, Tile winningTile, boolean tsumo, Wind roundWind, Wind seatWind,
                       boolean riichi, boolean doubleRiichi, boolean ippatsu,
                       boolean haitei, boolean houtei, boolean rinshan, boolean chankan,
                       boolean tenhou, boolean chiihou,
                       List<Tile> doraIndicators, List<Tile> uraDoraIndicators) {
        this.hand = hand;
        this.winningTile = winningTile;
        this.tsumo = tsumo;
        this.roundWind = roundWind;
        this.seatWind = seatWind;
        this.riichi = riichi;
        this.doubleRiichi = doubleRiichi;
        this.ippatsu = ippatsu;
        this.haitei = haitei;
        this.houtei = houtei;
        this.rinshan = rinshan;
        this.chankan = chankan;
        this.tenhou = tenhou;
        this.chiihou = chiihou;
        this.doraIndicators = List.copyOf(doraIndicators);
        this.uraDoraIndicators = List.copyOf(uraDoraIndicators);
    }

    public Hand hand() {
        return hand;
    }

    public Tile winningTile() {
        return winningTile;
    }

    public boolean isTsumo() {
        return tsumo;
    }

    public boolean isRon() {
        return !tsumo;
    }

    public Wind roundWind() {
        return roundWind;
    }

    public Wind seatWind() {
        return seatWind;
    }

    public boolean isRiichi() {
        return riichi;
    }

    public boolean isDoubleRiichi() {
        return doubleRiichi;
    }

    public boolean isIppatsu() {
        return ippatsu;
    }

    public boolean isHaitei() {
        return haitei;
    }

    public boolean isHoutei() {
        return houtei;
    }

    public boolean isRinshan() {
        return rinshan;
    }

    public boolean isChankan() {
        return chankan;
    }

    public boolean isTenhou() {
        return tenhou;
    }

    public boolean isChiihou() {
        return chiihou;
    }

    public boolean isDealer() {
        return seatWind == Wind.EAST;
    }

    public List<Tile> doraIndicators() {
        return doraIndicators;
    }

    public List<Tile> uraDoraIndicators() {
        return uraDoraIndicators;
    }
}
