package sahi351.mahjong.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.hand.Meld;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

class TopPrizeStrategyTest {

    private final TopPrizeStrategy strategy = new TopPrizeStrategy();

    @Test
    void discardsSafeTileWhenProtectingASizableLead() {
        // 1位・2位との差20000点: 振り込み回避を最優先し、リーチ者への現物(1s)を切る。
        Hand hand = AiTestSupport.handOf("123m456m789m12p11s7z");
        PlayerPublicView riichiOpponent = new PlayerPublicView("敵", 1, Wind.SOUTH, 20000,
                List.of(Tile.of(Suit.SOUZU, 1)), List.of(), true);
        AiContext ctx = AiTestSupport.context(hand, Wind.EAST, Wind.EAST, 1, 40000, List.of(riichiOpponent));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.SOUZU, 1), discard);
    }

    @Test
    void usesEfficiencyWhenLeadIsNotBigEnough() {
        // 1位だが2位との差が10000点未満のため、牌効率を優先し7zを切る。
        Hand hand = AiTestSupport.handOf("123m456m789m12p11s7z");
        PlayerPublicView riichiOpponent = new PlayerPublicView("敵", 1, Wind.SOUTH, 20000,
                List.of(Tile.of(Suit.SOUZU, 1)), List.of(), true);
        AiContext ctx = AiTestSupport.context(hand, Wind.EAST, Wind.EAST, 1, 25000, List.of(riichiOpponent));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.JIHAI, 7), discard);
    }

    @Test
    void ignoresOpponentRiichiWhenRankIsSecondOrLower() {
        // 2位以下は牌効率を最優先し、他家リーチを無視して7zを切る。
        Hand hand = AiTestSupport.handOf("123m456m789m12p11s7z");
        PlayerPublicView riichiOpponent = new PlayerPublicView("敵", 1, Wind.SOUTH, 30000,
                List.of(Tile.of(Suit.SOUZU, 1)), List.of(), true);
        AiContext ctx = AiTestSupport.context(hand, Wind.EAST, Wind.EAST, 1, 20000, List.of(riichiOpponent));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.JIHAI, 7), discard);
    }

    @Test
    void keepsTheLoneYaochuuTileWhenFourthPlaceInSouthFour() {
        // 234m567m345s88p789p (テンパイ形、唯一の么九牌は9p)。
        // 通常の牌効率なら9pを切って両面待ちに受けるが、役満(国士無双)を目指すため
        // 9pを残し、余剰牌として重なっている8pを切る。
        Hand hand = AiTestSupport.handOf("234m567m345s88p789p");
        PlayerPublicView first = new PlayerPublicView("1位", 1, Wind.SOUTH, 40000, List.of(), List.of(), false);
        PlayerPublicView second = new PlayerPublicView("2位", 2, Wind.WEST, 30000, List.of(), List.of(), false);
        PlayerPublicView third = new PlayerPublicView("3位", 3, Wind.EAST, 25000, List.of(), List.of(), false);
        AiContext ctx = AiTestSupport.context(hand, Wind.NORTH, Wind.SOUTH, 4, 5000,
                List.of(first, second, third));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.PINZU, 8), discard);
    }

    @Test
    void doesNotAimForYakumanWhenNotSouthFour() {
        // 同じ4位でも南4局でなければ役満狙いにはならず、通常の牌効率(9p切り)で判断する。
        Hand hand = AiTestSupport.handOf("234m567m345s88p789p");
        PlayerPublicView first = new PlayerPublicView("1位", 1, Wind.SOUTH, 40000, List.of(), List.of(), false);
        PlayerPublicView second = new PlayerPublicView("2位", 2, Wind.WEST, 30000, List.of(), List.of(), false);
        PlayerPublicView third = new PlayerPublicView("3位", 3, Wind.EAST, 25000, List.of(), List.of(), false);
        AiContext ctx = AiTestSupport.context(hand, Wind.NORTH, Wind.SOUTH, 3, 5000,
                List.of(first, second, third));

        Tile discard = strategy.chooseDiscard(ctx);

        assertEquals(Tile.of(Suit.PINZU, 9), discard);
    }

    @Test
    void alwaysDeclaresRiichiAsDealerWhenNoOpponentHasRiichi() {
        // 親かつ他家未リーチなら、役の翻数に関わらず必ずリーチする（1翻のみでも）。
        Hand hand = AiTestSupport.handOf("123m234m456p789s5s1z");
        AiContext ctx = AiTestSupport.context(hand, Wind.EAST, Wind.EAST, 1, 25000, List.of());

        assertTrue(strategy.wantsRiichi(ctx, Tile.of(Suit.JIHAI, 1)));
    }

    @Test
    void nonDealerNeedsAtLeastThreeHanToRiichi() {
        // 子: 123m234m456p789s5sタンキ待ちはリーチのみで1翻のため、3翻未満でリーチしない。
        Hand hand = AiTestSupport.handOf("123m234m456p789s5s1z");
        AiContext ctx = AiTestSupport.context(hand, Wind.SOUTH, Wind.EAST, 1, 25000, List.of());

        assertFalse(strategy.wantsRiichi(ctx, Tile.of(Suit.JIHAI, 1)));
    }

    @Test
    void nonDealerRiichiWhenThreeHanAreConfirmed() {
        // 子: 234m567m678p22p34s1z: リーチ・ピンフ・タンヤオ・断ヤオで3翻以上確定。
        Hand hand = AiTestSupport.handOf("234m567m678p22p34s1z");
        AiContext ctx = AiTestSupport.context(hand, Wind.SOUTH, Wind.EAST, 1, 25000, List.of());

        assertTrue(strategy.wantsRiichi(ctx, Tile.of(Suit.JIHAI, 1)));
    }

    @Test
    void passesWhenNoYakuReachesManganLevel() {
        // 234m567m11p45p77z9s: 中をポンしてもテンパイ時の打点は満貫に届かない。
        Hand hand = AiTestSupport.handOf("234m567m11p45p77z9s");
        AiContext ctx = AiTestSupport.context(hand, List.of());
        Tile chun = Tile.of(Suit.JIHAI, 7);
        CallOption ponOption = new CallOption(CallType.PON, List.of(chun, chun));

        assertEquals(CallOption.PASS, strategy.decideNaki(ctx, chun, List.of(ponOption)));
    }

    @Test
    void doesNotDeclareAnkanWhenOnlyAtOneShanten() {
        // 1111m234m567m88p9p (門前1シャンテン、テンパイではない) -> しない
        Hand hand = AiTestSupport.handOf("1111m234m567m88p9p");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertFalse(strategy.wantsAnkan(ctx, Tile.of(Suit.MANZU, 1)));
    }

    @Test
    void declaresAnkanWhenMenzenAndAlreadyTenpai() {
        // 1111m234m567m789p (門前テンパイ、1mタンキ待ち) -> する
        Hand hand = AiTestSupport.handOf("1111m234m567m789p");
        AiContext ctx = AiTestSupport.context(hand, List.of());

        assertTrue(strategy.wantsAnkan(ctx, Tile.of(Suit.MANZU, 1)));
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
}
