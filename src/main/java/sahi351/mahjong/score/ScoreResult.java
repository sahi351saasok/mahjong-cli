package sahi351.mahjong.score;

import java.util.List;
import sahi351.mahjong.yaku.YakuResult;

/**
 * 和了の点数計算結果。
 */
public final class ScoreResult {
    private final List<YakuResult> yakuList;
    private final int han;
    private final int fu;
    private final ScoreTier tier;
    private final int basePoints;
    private final boolean dealer;
    private final boolean tsumo;
    private final int ronPayment;
    private final int tsumoDealerPayment;
    private final int tsumoNonDealerPayment;
    private final int honba;
    private final int honbaBonus;

    public ScoreResult(List<YakuResult> yakuList, int han, int fu, ScoreTier tier, int basePoints,
                        boolean dealer, boolean tsumo, int ronPayment,
                        int tsumoDealerPayment, int tsumoNonDealerPayment, int honba) {
        this.yakuList = List.copyOf(yakuList);
        this.han = han;
        this.fu = fu;
        this.tier = tier;
        this.basePoints = basePoints;
        this.dealer = dealer;
        this.tsumo = tsumo;
        this.honba = honba;
        int perHonba = 100;
        this.honbaBonus = honba * (tsumo ? perHonba : perHonba * 3);
        this.ronPayment = ronPayment + (tsumo ? 0 : honba * perHonba * 3);
        this.tsumoDealerPayment = tsumoDealerPayment + (tsumo ? honba * perHonba : 0);
        this.tsumoNonDealerPayment = tsumoNonDealerPayment + (tsumo ? honba * perHonba : 0);
    }

    public List<YakuResult> yakuList() {
        return yakuList;
    }

    public int han() {
        return han;
    }

    public int fu() {
        return fu;
    }

    public ScoreTier tier() {
        return tier;
    }

    public int basePoints() {
        return basePoints;
    }

    public boolean isDealer() {
        return dealer;
    }

    public boolean isTsumo() {
        return tsumo;
    }

    public int ronPayment() {
        return ronPayment;
    }

    public int tsumoDealerPayment() {
        return tsumoDealerPayment;
    }

    public int tsumoNonDealerPayment() {
        return tsumoNonDealerPayment;
    }

    public int totalPoints() {
        if (tsumo) {
            if (dealer) {
                return tsumoNonDealerPayment * 3;
            }
            return tsumoDealerPayment + tsumoNonDealerPayment * 2;
        }
        return ronPayment;
    }
}
