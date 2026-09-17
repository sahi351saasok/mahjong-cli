package sahi351.mahjong.ai;

import java.util.List;
import sahi351.mahjong.tile.Tile;

/**
 * CPUの思考ロジック。差し替え可能な設計とするためインタフェースとして定義する。
 */
public interface PlayerStrategy {

    /** ツモ後（14枚）の手牌から捨てる牌を選ぶ。 */
    Tile chooseDiscard(AiContext ctx);

    /** 指定した牌を捨てればテンパイになる状況で、リーチを宣言するか判断する。 */
    boolean wantsRiichi(AiContext ctx, Tile plannedDiscard);

    /** 他家の打牌に対する鳴き（チー・ポン・カン）の判断。PASSも選択肢に含まれる。 */
    CallOption decideNaki(AiContext ctx, Tile discardedTile, List<CallOption> legalOptions);

    /** 自摸番での暗槓を行うか判断する。 */
    boolean wantsAnkan(AiContext ctx, Tile kanTile);

    /** 自摸番での加槓を行うか判断する。 */
    boolean wantsKakan(AiContext ctx, Tile kanTile);
}
