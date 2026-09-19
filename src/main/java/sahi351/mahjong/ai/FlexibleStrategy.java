package sahi351.mahjong.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.hand.MeldType;
import sahi351.mahjong.hand.ShantenCalculator;
import sahi351.mahjong.hand.TileIndex;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

/**
 * 要件定義書（臨機応変戦略）に基づく思考ロジック。
 * 打牌: 手牌の形に応じて 国士無双 → 混一色 → 四暗刻 → 七対子 の順に狙う。いずれにも当てはまらなければ牌効率を優先する。
 *       国士無双狙いの場合のみ、自身がテンパイしていないときの他家リーチに対して安全牌を切って降りる。
 * リーチ: 待ち牌が3枚以上残っていると見込める場合、もしくは親かつ他家未リーチの場合にリーチする。
 * 鳴き: 南3局以降かつ1位なら1翻以上、それ以外は2翻以上の役が確定し、シャンテン数を下げられる場合に鳴く。
 * 暗槓: 門前でテンパイしている場合に行う（リーチ中の暗刻の槓子化もこれに含まれる）。
 * 加槓: しない。
 */
public final class FlexibleStrategy implements PlayerStrategy {

    private static final int KOKUSHI_MIN_YAOCHUU_KINDS = 9;
    private static final int HONITSU_MIN_TILES = 8;
    private static final int SUUANKOU_MIN_ANKOU = 2;
    private static final int SUUANKOU_MIN_TOITSU = 2;
    private static final int CHIITOI_MIN_TOITSU = 4;
    private static final int RIICHI_MIN_WAIT_TILES = 3;

    @Override
    public Tile chooseDiscard(AiContext ctx) {
        Hand hand = ctx.ownHand();
        List<Tile> concealed = hand.concealedTiles();
        int meldCount = hand.melds().size();
        List<Tile> visible = AiSupport.collectVisibleTiles(ctx);
        List<Tile> doraTileKinds = AiSupport.doraTileKinds(ctx);
        List<TileEfficiencyEvaluator.Candidate> candidates =
                TileEfficiencyEvaluator.evaluate(concealed, meldCount, visible);
        Comparator<TileEfficiencyEvaluator.Candidate> efficiency = efficiencyComparator(ctx, doraTileKinds);
        int[] counts = TileIndex.toCounts(concealed);

        if (isAimingKokushi(meldCount, counts)) {
            if (ctx.anyOpponentRiichi() && hand.shanten() > 0) {
                return pick(candidates, defendComparator(ctx, doraTileKinds), concealed);
            }
            return AiSupport.chooseKokushiDiscard(concealed);
        }

        Suit honitsuSuit = honitsuSuit(hand);
        if (honitsuSuit != null) {
            List<TileEfficiencyEvaluator.Candidate> offSuit = candidates.stream()
                    .filter(c -> !c.discard().isHonor() && c.discard().suit() != honitsuSuit)
                    .toList();
            if (!offSuit.isEmpty()) {
                return pick(offSuit, efficiency, concealed);
            }
        }

        if (isAimingSuuankou(hand, counts)) {
            List<TileEfficiencyEvaluator.Candidate> singles = candidates.stream()
                    .filter(c -> counts[TileIndex.of(c.discard())] == 1)
                    .toList();
            if (!singles.isEmpty()) {
                return pick(singles, efficiency, concealed);
            }
        }

        if (meldCount == 0 && countToitsu(counts) >= CHIITOI_MIN_TOITSU) {
            int[] visibleCounts = TileIndex.toCounts(visible);
            return pick(candidates, chiitoiComparator(counts, visibleCounts, ctx, doraTileKinds), concealed);
        }

        return pick(candidates, efficiency, concealed);
    }

    private Tile pick(List<TileEfficiencyEvaluator.Candidate> candidates,
                      Comparator<TileEfficiencyEvaluator.Candidate> comparator, List<Tile> concealed) {
        return candidates.stream().min(comparator)
                .map(TileEfficiencyEvaluator.Candidate::discard)
                .orElse(concealed.get(concealed.size() - 1));
    }

    private Comparator<TileEfficiencyEvaluator.Candidate> efficiencyComparator(AiContext ctx,
                                                                              List<Tile> doraTileKinds) {
        return Comparator
                .comparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                .thenComparingInt(c -> -c.ukeire())
                .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                        ctx.ownSeatWind(), ctx.roundWind()));
    }

    private Comparator<TileEfficiencyEvaluator.Candidate> defendComparator(AiContext ctx,
                                                                          List<Tile> doraTileKinds) {
        return Comparator
                .comparingInt((TileEfficiencyEvaluator.Candidate c) -> -AiSupport.riichiSafetyScore(c.discard(), ctx))
                .thenComparingInt(TileEfficiencyEvaluator.Candidate::shanten)
                .thenComparingInt(c -> -c.ukeire())
                .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                        ctx.ownSeatWind(), ctx.roundWind()));
    }

    /**
     * 七対子のシャンテンが最も進む牌を切る。同じシャンテンなら么九牌を残し（待ちを么九牌にするため）、
     * 次に場に見えている枚数が多い牌（待ちとして残りにくい牌）を切る。
     */
    private Comparator<TileEfficiencyEvaluator.Candidate> chiitoiComparator(int[] counts, int[] visibleCounts,
                                                                           AiContext ctx, List<Tile> doraTileKinds) {
        return Comparator
                .comparingInt((TileEfficiencyEvaluator.Candidate c) -> chiitoiShantenAfterDiscard(counts, c.discard()))
                .thenComparingInt(c -> c.discard().isYaochuu() ? 1 : 0)
                .thenComparingInt(c -> 4 - visibleCounts[TileIndex.of(c.discard())])
                .thenComparingInt(c -> TileEfficiencyEvaluator.keepValue(c.discard(), doraTileKinds,
                        ctx.ownSeatWind(), ctx.roundWind()));
    }

    private int chiitoiShantenAfterDiscard(int[] counts, Tile discard) {
        int idx = TileIndex.of(discard);
        counts[idx]--;
        int shanten = ShantenCalculator.chiitoiShanten(counts);
        counts[idx]++;
        return shanten;
    }

    private boolean isAimingKokushi(int meldCount, int[] counts) {
        if (meldCount != 0) {
            return false;
        }
        int kinds = 0;
        for (int idx = 0; idx < TileIndex.SIZE; idx++) {
            if (counts[idx] > 0 && TileIndex.toTile(idx).isYaochuu()) {
                kinds++;
            }
        }
        return kinds >= KOKUSHI_MIN_YAOCHUU_KINDS;
    }

    /** 手牌（副露を含む）に同じ種類の数牌が8枚以上あり、他の種類の数牌を副露していなければ、その種類を返す。 */
    private Suit honitsuSuit(Hand hand) {
        int[] suitCounts = new int[Suit.values().length];
        for (Tile t : hand.allTiles()) {
            if (!t.isHonor()) {
                suitCounts[t.suit().ordinal()]++;
            }
        }
        Suit best = null;
        for (Suit suit : Suit.values()) {
            if (suit == Suit.JIHAI) {
                continue;
            }
            if (best == null || suitCounts[suit.ordinal()] > suitCounts[best.ordinal()]) {
                best = suit;
            }
        }
        if (best == null || suitCounts[best.ordinal()] < HONITSU_MIN_TILES) {
            return null;
        }
        for (Meld meld : hand.melds()) {
            for (Tile t : meld.tiles()) {
                if (!t.isHonor() && t.suit() != best) {
                    return null;
                }
            }
        }
        return best;
    }

    private boolean isAimingSuuankou(Hand hand, int[] counts) {
        if (!hand.isMenzen()) {
            return false;
        }
        int ankou = (int) hand.melds().stream().filter(m -> m.type() == MeldType.ANKAN).count();
        int toitsu = 0;
        for (int count : counts) {
            if (count >= 3) {
                ankou++;
            } else if (count == 2) {
                toitsu++;
            }
        }
        return ankou >= SUUANKOU_MIN_ANKOU && toitsu >= SUUANKOU_MIN_TOITSU;
    }

    private int countToitsu(int[] counts) {
        int toitsu = 0;
        for (int count : counts) {
            if (count == 2) {
                toitsu++;
            }
        }
        return toitsu;
    }

    @Override
    public boolean wantsRiichi(AiContext ctx, Tile plannedDiscard) {
        if (!ctx.ownHand().isMenzen()) {
            return false;
        }
        List<Tile> sub = new ArrayList<>(ctx.ownHand().concealedTiles());
        sub.remove(plannedDiscard);
        int meldCount = ctx.ownHand().melds().size();
        if (ShantenCalculator.shanten(sub, meldCount) != 0) {
            return false;
        }
        if (ctx.isDealer() && !ctx.anyOpponentRiichi()) {
            return true;
        }
        List<Tile> waits = HandValueEstimator.findWaits(sub, meldCount);
        int[] visibleCounts = TileIndex.toCounts(AiSupport.collectVisibleTiles(ctx));
        int totalWaitTiles = 0;
        for (Tile wait : waits) {
            totalWaitTiles += 4 - visibleCounts[TileIndex.of(wait)];
        }
        return totalWaitTiles >= RIICHI_MIN_WAIT_TILES;
    }

    @Override
    public CallOption decideNaki(AiContext ctx, Tile discardedTile, List<CallOption> legalOptions) {
        int requiredHan = requiredHanForCall(ctx);
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
            Integer han = HandValueEstimator.bestHanReachableAfterOneDiscard(
                    resultingConcealed, meldsAfter, ctx.roundWind(), ctx.ownSeatWind(),
                    ctx.doraIndicators(), false);
            if (han == null || han < requiredHan) {
                continue;
            }
            if (resultingShanten < bestShanten) {
                bestShanten = resultingShanten;
                best = option;
            }
        }
        return best == null ? CallOption.PASS : best;
    }

    /** 南3局以降かつ1位の場合は1翻、それ以外は2翻以上の役が確定していることを鳴きの条件とする。 */
    private int requiredHanForCall(AiContext ctx) {
        return isSouthThreeOrLater(ctx) && AiSupport.ownRank(ctx) == 1 ? 1 : 2;
    }

    private boolean isSouthThreeOrLater(AiContext ctx) {
        return ctx.roundWind().ordinal() > Wind.SOUTH.ordinal()
                || (ctx.roundWind() == Wind.SOUTH && ctx.kyokuNumber() >= 3);
    }

    @Override
    public boolean wantsAnkan(AiContext ctx, Tile kanTile) {
        Hand hand = ctx.ownHand();
        return hand.isMenzen() && hand.isTenpai()
                && AiSupport.resultingShantenAfterAnkan(hand, kanTile) <= 0;
    }

    @Override
    public boolean wantsKakan(AiContext ctx, Tile kanTile) {
        return false;
    }
}
