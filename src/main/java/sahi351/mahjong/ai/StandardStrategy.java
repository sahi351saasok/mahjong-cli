package sahi351.mahjong.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.tile.Tile;

/**
 * 要件定義書に基づく標準的なCPU思考ロジック。
 * 打牌: 牌効率最優先 → 高い役作りを優先 → 他家リーチ時は安全牌優先で降りる。
 * リーチ: テンパイ かつ 満貫以上が見込める場合のみ即リーチ。
 * 鳴き: 一向聴かつ満貫以上が見込める場合のみ、最速でテンパイに近づく鳴きを行う。
 */
public final class StandardStrategy implements PlayerStrategy {

    @Override
    public Tile chooseDiscard(AiContext ctx) {
        List<Tile> concealed = ctx.ownHand().concealedTiles();
        int meldCount = ctx.ownHand().melds().size();
        List<Tile> visible = collectVisibleTiles(ctx);
        List<Tile> doraTileKinds = doraTileKinds(ctx);

        List<TileEfficiencyEvaluator.Candidate> candidates =
                TileEfficiencyEvaluator.evaluate(concealed, meldCount, visible);

        boolean defending = ctx.anyOpponentRiichi();
        Comparator<TileEfficiencyEvaluator.Candidate> comparator;
        if (defending) {
            comparator = Comparator
                    .comparingInt((TileEfficiencyEvaluator.Candidate c) -> -safetyScore(c.discard(), ctx))
                    .thenComparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                    .thenComparingInt(c -> -c.ukeire())
                    .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                            ctx.ownSeatWind(), ctx.roundWind()));
        } else {
            comparator = Comparator
                    .comparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                    .thenComparingInt(c -> -c.ukeire())
                    .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                            ctx.ownSeatWind(), ctx.roundWind()));
        }

        return candidates.stream().min(comparator)
                .map(TileEfficiencyEvaluator.Candidate::discard)
                .orElse(concealed.get(concealed.size() - 1));
    }

    private int safetyScore(Tile tile, AiContext ctx) {
        int score = 0;
        for (PlayerPublicView p : ctx.others()) {
            if (!p.riichi()) {
                continue;
            }
            boolean genbutsu = p.discards().stream().anyMatch(d -> d.isSameKind(tile));
            if (genbutsu) {
                score += 100;
            } else if (tile.isHonor()) {
                score += 1;
            }
        }
        return score;
    }

    private List<Tile> collectVisibleTiles(AiContext ctx) {
        List<Tile> visible = new ArrayList<>(ctx.ownHand().allTiles());
        visible.addAll(ctx.doraIndicators());
        for (PlayerPublicView p : ctx.others()) {
            visible.addAll(p.discards());
            for (Meld m : p.melds()) {
                visible.addAll(m.tiles());
            }
        }
        return visible;
    }

    private List<Tile> doraTileKinds(AiContext ctx) {
        List<Tile> result = new ArrayList<>();
        for (Tile indicator : ctx.doraIndicators()) {
            result.add(indicator.nextForDora());
        }
        return result;
    }

    @Override
    public boolean wantsRiichi(AiContext ctx, Tile plannedDiscard) {
        if (!ctx.ownHand().isMenzen()) {
            return false;
        }
        List<Tile> sub = new ArrayList<>(ctx.ownHand().concealedTiles());
        sub.remove(plannedDiscard);
        if (ShantenCalculator.shanten(sub, ctx.ownHand().melds().size()) != 0) {
            return false;
        }
        Integer best = HandValueEstimator.bestScoreForTenpai(sub, ctx.ownHand().melds(),
                ctx.roundWind(), ctx.ownSeatWind(), ctx.doraIndicators(), true);
        return best != null && best >= HandValueEstimator.MANGAN_BASE_POINTS;
    }

    @Override
    public CallOption decideNaki(AiContext ctx, Tile discardedTile, List<CallOption> legalOptions) {
        int currentShanten = ctx.ownHand().shanten();
        if (currentShanten != 1) {
            return CallOption.PASS;
        }
        List<Tile> concealed = ctx.ownHand().concealedTiles();
        int meldCount = ctx.ownHand().melds().size();

        CallOption best = null;
        int bestShanten = Integer.MAX_VALUE;
        for (CallOption option : legalOptions) {
            if (option.type() == CallType.PASS) {
                continue;
            }
            List<Tile> resultingConcealed = new ArrayList<>(concealed);
            for (Tile used : option.tilesToUse()) {
                resultingConcealed.remove(used);
            }
            int resultingMeldCount = meldCount + 1;
            int resultingShanten = ShantenCalculator.shanten(resultingConcealed, resultingMeldCount);
            if (resultingShanten >= currentShanten) {
                continue;
            }
            List<Meld> meldsAfter = new ArrayList<>(ctx.ownHand().melds());
            meldsAfter.add(buildHypotheticalMeld(option, discardedTile));
            Integer potential = HandValueEstimator.bestReachableAfterOneDiscard(
                    resultingConcealed, meldsAfter, ctx.roundWind(), ctx.ownSeatWind(),
                    ctx.doraIndicators(), false);
            if (potential == null || potential < HandValueEstimator.MANGAN_BASE_POINTS) {
                continue;
            }
            if (resultingShanten < bestShanten) {
                bestShanten = resultingShanten;
                best = option;
            }
        }
        return best == null ? CallOption.PASS : best;
    }

    private Meld buildHypotheticalMeld(CallOption option, Tile calledTile) {
        List<Tile> tiles = new ArrayList<>(option.tilesToUse());
        tiles.add(calledTile);
        return switch (option.type()) {
            case CHI -> Meld.chi(tiles, calledTile, -1);
            case PON -> Meld.pon(tiles, calledTile, -1);
            case KAN -> Meld.minkan(tiles, calledTile, -1);
            case PASS -> throw new IllegalArgumentException("PASSはMeldにできない");
        };
    }

    @Override
    public boolean wantsAnkan(AiContext ctx, Tile kanTile) {
        Hand hand = ctx.ownHand();
        int currentShanten = ShantenCalculator.shanten(hand.concealedTiles(), hand.melds().size());
        List<Tile> sub = new ArrayList<>(hand.concealedTiles());
        int removed = 0;
        for (int i = sub.size() - 1; i >= 0 && removed < 4; i--) {
            if (sub.get(i).isSameKind(kanTile)) {
                sub.remove(i);
                removed++;
            }
        }
        int resultingShanten = ShantenCalculator.shanten(sub, hand.melds().size() + 1);
        return resultingShanten <= currentShanten;
    }

    @Override
    public boolean wantsKakan(AiContext ctx, Tile kanTile) {
        return true;
    }
}
