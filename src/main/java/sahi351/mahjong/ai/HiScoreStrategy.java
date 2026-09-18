package sahi351.mahjong.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.tile.Tile;

/**
 * 要件定義書（高打点戦略）に基づく思考ロジック。
 * 打牌: 牌効率よりも3翻以上の高打点を狙って選択する。他家リーチ時は安全牌を切って降りる。
 * リーチ: 2翻以上の役が確定している場合のみリーチする。
 * 鳴き: 満貫以上の役が確定している場合のみ鳴く。
 * 暗槓: イーシャンテンもしくはテンパイしている場合に行う。
 * 加槓: しない。
 */
public final class HiScoreStrategy implements PlayerStrategy {

    @Override
    public Tile chooseDiscard(AiContext ctx) {
        List<Tile> concealed = ctx.ownHand().concealedTiles();
        int meldCount = ctx.ownHand().melds().size();
        List<Tile> visible = AiSupport.collectVisibleTiles(ctx);
        List<Tile> doraTileKinds = AiSupport.doraTileKinds(ctx);
        List<TileEfficiencyEvaluator.Candidate> candidates =
                TileEfficiencyEvaluator.evaluate(concealed, meldCount, visible);

        if (ctx.anyOpponentRiichi()) {
            Comparator<TileEfficiencyEvaluator.Candidate> defend = Comparator
                    .comparingInt((TileEfficiencyEvaluator.Candidate c) -> -AiSupport.riichiSafetyScore(c.discard(), ctx))
                    .thenComparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                    .thenComparingInt(c -> -c.ukeire())
                    .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                            ctx.ownSeatWind(), ctx.roundWind()));
            return candidates.stream().min(defend)
                    .map(TileEfficiencyEvaluator.Candidate::discard)
                    .orElse(concealed.get(concealed.size() - 1));
        }

        int minShanten = candidates.stream()
                .mapToInt(TileEfficiencyEvaluator.Candidate::shanten)
                .min().orElse(0);

        if (minShanten == 0) {
            Comparator<TileEfficiencyEvaluator.Candidate> byValue = Comparator
                    .comparingInt((TileEfficiencyEvaluator.Candidate c) -> -tenpaiHan(c, ctx))
                    .thenComparingInt(c -> -c.ukeire());
            return candidates.stream()
                    .filter(c -> c.shanten() == 0)
                    .min(byValue)
                    .map(TileEfficiencyEvaluator.Candidate::discard)
                    .orElse(concealed.get(concealed.size() - 1));
        }

        Comparator<TileEfficiencyEvaluator.Candidate> byPotentialValue = Comparator
                .comparingInt((TileEfficiencyEvaluator.Candidate c) -> -retainedValue(c, concealed, doraTileKinds, ctx))
                .thenComparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                .thenComparingInt(c -> -c.ukeire());

        return candidates.stream().min(byPotentialValue)
                .map(TileEfficiencyEvaluator.Candidate::discard)
                .orElse(concealed.get(concealed.size() - 1));
    }

    private int tenpaiHan(TileEfficiencyEvaluator.Candidate c, AiContext ctx) {
        List<Tile> sub = new ArrayList<>(ctx.ownHand().concealedTiles());
        sub.remove(c.discard());
        Integer han = HandValueEstimator.bestHanForTenpai(sub, ctx.ownHand().melds(),
                ctx.roundWind(), ctx.ownSeatWind(), ctx.doraIndicators(), true);
        return han == null ? -1 : han;
    }

    private int retainedValue(TileEfficiencyEvaluator.Candidate c, List<Tile> concealed,
                               List<Tile> doraTileKinds, AiContext ctx) {
        List<Tile> remaining = new ArrayList<>(concealed);
        remaining.remove(c.discard());
        int value = 0;
        for (Tile t : remaining) {
            value += TileEfficiencyEvaluator.keepValue(t, doraTileKinds, ctx.ownSeatWind(), ctx.roundWind());
        }
        return value;
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
        Integer han = HandValueEstimator.bestHanForTenpai(sub, ctx.ownHand().melds(),
                ctx.roundWind(), ctx.ownSeatWind(), ctx.doraIndicators(), true);
        return han != null && han >= 2;
    }

    @Override
    public CallOption decideNaki(AiContext ctx, Tile discardedTile, List<CallOption> legalOptions) {
        int currentShanten = ctx.ownHand().shanten();
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
            int resultingShanten = ShantenCalculator.shanten(resultingConcealed, meldCount + 1);
            if (resultingShanten >= currentShanten) {
                continue;
            }
            List<Meld> meldsAfter = new ArrayList<>(ctx.ownHand().melds());
            meldsAfter.add(AiSupport.buildHypotheticalMeld(option, discardedTile));
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

    @Override
    public boolean wantsAnkan(AiContext ctx, Tile kanTile) {
        int shanten = ctx.ownHand().shanten();
        if (shanten != 0 && shanten != 1) {
            return false;
        }
        return AiSupport.resultingShantenAfterAnkan(ctx.ownHand(), kanTile) <= shanten;
    }

    @Override
    public boolean wantsKakan(AiContext ctx, Tile kanTile) {
        return false;
    }
}
