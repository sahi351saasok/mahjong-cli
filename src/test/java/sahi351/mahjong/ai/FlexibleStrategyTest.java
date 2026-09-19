package sahi351.mahjong.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

class FlexibleStrategyTest {

    private final FlexibleStrategy strategy = new FlexibleStrategy();

    // ---- 捨て牌: 国士無双 ----

    @Test
    void aimsForKokushiWhenNineOrMoreYaochuuKindsAreHeld() {
        // 么九牌が9種類: 国士無双を狙い、么九牌以外のうち最も重なっている3mを切る。
        // （牌効率では順子候補の2mを切る）
        Hand hand = AiTestSupport.handOf("12339m1459p19s123z");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.MANZU, 3), discard);
    }

    @Test
    void foldsWithSafeTileWhenAimingKokushiWithoutTenpaiAndOpponentRiichi() {
        // 国士無双狙いで未テンパイのときに他家がリーチした場合は、現物(9m)を切って降りる。
        Hand hand = AiTestSupport.handOf("12339m1459p19s123z");
        PlayerPublicView riichiOpponent = AiTestSupport.riichiOpponent("敵", 1, List.of(Tile.of(Suit.MANZU, 9)));
        AiContext ctx = AiTestSupport.context(hand, List.of(riichiOpponent));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.MANZU, 9), discard);
    }

    @Test
    void keepsAimingForKokushiWhenTenpaiEvenIfOpponentRiichi() {
        // 国士無双13面待ちのテンパイ形では、他家リーチでも降りずに5mを切る。
        Hand hand = AiTestSupport.handOf("19m19p19s1234567z5m");
        PlayerPublicView riichiOpponent = AiTestSupport.riichiOpponent("敵", 1, List.of(Tile.of(Suit.MANZU, 1)));
        AiContext ctx = AiTestSupport.context(hand, List.of(riichiOpponent));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.MANZU, 5), discard);
    }

    // ---- 捨て牌: 混一色 ----

    @Test
    void aimsForHonitsuWhenEightOrMoreTilesOfOneSuitAreHeld() {
        // 萬子が8枚: 牌効率では孤立した2zを切るところを、他の種類の数牌(3p,5p,8s)から切る。
        Hand hand = AiTestSupport.handOf("23456799m35p8s112z");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.SOUZU, 8), discard);
    }

    // ---- 捨て牌: 四暗刻 ----

    @Test
    void aimsForSuuankouByKeepingTriplets() {
        // 暗刻2つ(555s, 222z)・対子2つ(44m, 77p): 牌効率なら77pを崩すところだが、
        // 暗刻・対子を崩さず単独の牌(1p,2p,5p,6p)から切る。
        Hand hand = AiTestSupport.handOf("44m125677p555s222z");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        Tile discard = strategy.chooseDiscard(ctx);

        List<Tile> singles = List.of(Tile.of(Suit.PINZU, 1), Tile.of(Suit.PINZU, 2),
                Tile.of(Suit.PINZU, 5), Tile.of(Suit.PINZU, 6));
        assertTrue(singles.contains(discard), "単独牌を切るべきだが " + discard + " を切った");
    }

    // ---- 捨て牌: 七対子 ----

    @Test
    void aimsForChiitoitsuAndKeepsYaochuuTileAsTheWait() {
        // 対子6つ + 8p + 9s: どちらを切っても七対子テンパイだが、待ちを么九牌(9s)にするため8pを切る。
        Hand hand = AiTestSupport.handOf("1155m33778p44669s");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.PINZU, 8), discard);
    }

    // ---- 捨て牌: それ以外 ----

    @Test
    void usesTileEfficiencyWhenNoAimApplies() {
        // どの狙いにも当てはまらない形では牌効率で9pを切り、両面待ちに受ける。
        Hand hand = AiTestSupport.handOf("234m567m345s88p789p");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.PINZU, 9), discard);
    }

    @Test
    void doesNotFoldOutsideKokushiEvenIfOpponentRiichi() {
        // 国士無双狙い以外では他家リーチでも降りず、牌効率で9pを切る（1sは現物ではない）。
        Hand hand = AiTestSupport.handOf("234m567m345s88p789p");
        PlayerPublicView riichiOpponent = AiTestSupport.riichiOpponent("敵", 1, List.of(Tile.of(Suit.MANZU, 2)));
        AiContext ctx = AiTestSupport.context(hand, List.of(riichiOpponent));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.PINZU, 9), discard);
    }

    // ---- リーチ ----

    @Test
    void declaresRiichiWhenThreeOrMoreWaitTilesRemain() {
        // 5z単騎待ち: 自身の目から見て残り3枚。
        Hand hand = AiTestSupport.handOf("123m456m789m123p5z9s");
        AiContext ctx = AiTestSupport.context(hand, Wind.SOUTH, Wind.EAST, 1, 25000, List.of());

        assertTrue(strategy.wantsRiichi(ctx, Tile.of(Suit.SOUZU, 9)));
    }

    @Test
    void doesNotDeclareRiichiAsNonDealerWhenFewerThanThreeWaitTilesRemain() {
        // 5z単騎待ちだが他家が2枚切っているため残り1枚。子はリーチしない。
        Hand hand = AiTestSupport.handOf("123m456m789m123p5z9s");
        PlayerPublicView opponent = AiTestSupport.quietOpponent("敵", 1,
                List.of(Tile.of(Suit.JIHAI, 5), Tile.of(Suit.JIHAI, 5)));
        AiContext ctx = AiTestSupport.context(hand, Wind.SOUTH, Wind.EAST, 1, 25000, List.of(opponent));

        assertFalse(strategy.wantsRiichi(ctx, Tile.of(Suit.SOUZU, 9)));
    }

    @Test
    void declaresRiichiAsDealerWhenNoOpponentHasRiichiEvenWithFewWaitTiles() {
        Hand hand = AiTestSupport.handOf("123m456m789m123p5z9s");
        PlayerPublicView opponent = AiTestSupport.quietOpponent("敵", 1,
                List.of(Tile.of(Suit.JIHAI, 5), Tile.of(Suit.JIHAI, 5)));
        AiContext ctx = AiTestSupport.context(hand, Wind.EAST, Wind.EAST, 1, 25000, List.of(opponent));

        assertTrue(strategy.wantsRiichi(ctx, Tile.of(Suit.SOUZU, 9)));
    }

    @Test
    void doesNotDeclareRiichiAsDealerWhenOpponentHasRiichiAndFewWaitTiles() {
        Hand hand = AiTestSupport.handOf("123m456m789m123p5z9s");
        PlayerPublicView opponent = new PlayerPublicView("敵", 1, Wind.SOUTH, 25000,
                List.of(Tile.of(Suit.JIHAI, 5), Tile.of(Suit.JIHAI, 5)), List.of(), true);
        AiContext ctx = AiTestSupport.context(hand, Wind.EAST, Wind.EAST, 1, 25000, List.of(opponent));

        assertFalse(strategy.wantsRiichi(ctx, Tile.of(Suit.SOUZU, 9)));
    }

    @Test
    void doesNotDeclareRiichiWhenNotTenpai() {
        // 1sを切ると対子が崩れてテンパイしない（7zを切ればテンパイ）。
        Hand hand = AiTestSupport.handOf("123m456m789m12p11s7z");
        AiContext ctx = AiTestSupport.context(hand, Wind.EAST, Wind.EAST, 1, 25000, List.of());

        assertFalse(strategy.wantsRiichi(ctx, Tile.of(Suit.SOUZU, 1)));
    }

    @Test
    void doesNotDeclareRiichiWhenHandIsOpen() {
        Hand hand = AiTestSupport.handOf("123m456m789m123p9s");
        hand.addMeld(Meld.pon(
                List.of(Tile.of(Suit.PINZU, 5), Tile.of(Suit.PINZU, 5)), Tile.of(Suit.PINZU, 5), 2));
        AiContext ctx = AiTestSupport.context(hand, Wind.EAST, Wind.EAST, 1, 25000, List.of());

        assertFalse(strategy.wantsRiichi(ctx, Tile.of(Suit.SOUZU, 9)));
    }

    // ---- 鳴き ----

    private static final Tile CHUN = Tile.of(Suit.JIHAI, 7);

    private static List<PlayerPublicView> opponentsWithPoints(int points) {
        return List.of(
                AiTestSupport.opponentWithPoints("A", 1, Wind.SOUTH, points),
                AiTestSupport.opponentWithPoints("B", 2, Wind.WEST, points),
                AiTestSupport.opponentWithPoints("C", 3, Wind.NORTH, points));
    }

    @Test
    void passesWhenOnlyOneHanIsConfirmedBeforeSouthThree() {
        // 中をポンするとテンパイするが役は中の1翻のみ。2翻以上が必要なため鳴かない。
        Hand hand = AiTestSupport.handOf("234m567m11p45p77z9s");
        AiContext ctx = AiTestSupport.context(hand, List.of());
        CallOption pon = new CallOption(CallType.PON, List.of(CHUN, CHUN));

        assertEquals(CallOption.PASS, strategy.decideNaki(ctx, CHUN, List.of(pon)));
    }

    @Test
    void callsWithOneHanWhenTopInSouthThree() {
        // 南3局以降かつ1位: 1翻以上の役が確定するため鳴く。
        Hand hand = AiTestSupport.handOf("234m567m11p45p77z9s");
        AiContext ctx = AiTestSupport.context(hand, Wind.EAST, Wind.SOUTH, 3, 40000, opponentsWithPoints(20000));
        CallOption pon = new CallOption(CallType.PON, List.of(CHUN, CHUN));

        assertEquals(pon, strategy.decideNaki(ctx, CHUN, List.of(pon)));
    }

    @Test
    void passesWithOneHanWhenNotTopInSouthThree() {
        Hand hand = AiTestSupport.handOf("234m567m11p45p77z9s");
        AiContext ctx = AiTestSupport.context(hand, Wind.EAST, Wind.SOUTH, 3, 20000, opponentsWithPoints(30000));
        CallOption pon = new CallOption(CallType.PON, List.of(CHUN, CHUN));

        assertEquals(CallOption.PASS, strategy.decideNaki(ctx, CHUN, List.of(pon)));
    }

    @Test
    void passesWithOneHanWhenTopBeforeSouthThree() {
        Hand hand = AiTestSupport.handOf("234m567m11p45p77z9s");
        AiContext ctx = AiTestSupport.context(hand, Wind.EAST, Wind.SOUTH, 2, 40000, opponentsWithPoints(20000));
        CallOption pon = new CallOption(CallType.PON, List.of(CHUN, CHUN));

        assertEquals(CallOption.PASS, strategy.decideNaki(ctx, CHUN, List.of(pon)));
    }

    @Test
    void callsWhenTwoHanAreConfirmed() {
        // 發がドラ表示牌 → 中がドラ。中ポンで 役牌1 + ドラ3 の4翻が確定するため鳴く。
        Hand hand = AiTestSupport.handOf("234m567m11p45p77z9s");
        AiContext ctx = AiTestSupport.context(hand, List.of(), List.of(Tile.of(Suit.JIHAI, 6)));
        CallOption pon = new CallOption(CallType.PON, List.of(CHUN, CHUN));

        assertEquals(pon, strategy.decideNaki(ctx, CHUN, List.of(pon)));
    }

    // ---- 暗槓・加槓 ----

    @Test
    void declaresAnkanWhenMenzenAndTenpai() {
        Hand hand = AiTestSupport.handOf("1111m234m567m789p");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertTrue(strategy.wantsAnkan(ctx, Tile.of(Suit.MANZU, 1)));
    }

    @Test
    void doesNotDeclareAnkanWhenNotTenpai() {
        Hand hand = AiTestSupport.handOf("1111m234m567m88p9p");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertFalse(strategy.wantsAnkan(ctx, Tile.of(Suit.MANZU, 1)));
    }

    @Test
    void doesNotDeclareAnkanWhenHandIsOpen() {
        Hand hand = AiTestSupport.handOf("1111m234m567m88p");
        hand.addMeld(Meld.pon(
                List.of(Tile.of(Suit.PINZU, 9), Tile.of(Suit.PINZU, 9)), Tile.of(Suit.PINZU, 9), 2));
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertFalse(strategy.wantsAnkan(ctx, Tile.of(Suit.MANZU, 1)));
    }

    @Test
    void neverDeclaresKakan() {
        Hand hand = AiTestSupport.handOf("123m456m789m123p11s");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertFalse(strategy.wantsKakan(ctx, Tile.of(Suit.PINZU, 5)));
    }

    // ---- レジストリ ----

    @Test
    void isRegisteredAsFlexible() {
        assertInstanceOf(FlexibleStrategy.class, new StrategyRegistry().create("flexible"));
    }
}
