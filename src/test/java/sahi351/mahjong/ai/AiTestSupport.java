package sahi351.mahjong.ai;

import java.util.ArrayList;
import java.util.List;
import sahi351.mahjong.game.Wind;
import sahi351.mahjong.hand.Hand;
import sahi351.mahjong.tile.Suit;
import sahi351.mahjong.tile.Tile;

/** 戦略テスト用の共通ヘルパー。 */
final class AiTestSupport {

    private AiTestSupport() {
    }

    static List<Tile> parse(String spec) {
        List<Tile> tiles = new ArrayList<>();
        List<Integer> pendingRanks = new ArrayList<>();
        for (char c : spec.toCharArray()) {
            if (Character.isDigit(c)) {
                pendingRanks.add(c - '0');
            } else {
                Suit suit = switch (c) {
                    case 'm' -> Suit.MANZU;
                    case 'p' -> Suit.PINZU;
                    case 's' -> Suit.SOUZU;
                    case 'z' -> Suit.JIHAI;
                    default -> throw new IllegalArgumentException("unknown suit: " + c);
                };
                for (int rank : pendingRanks) {
                    tiles.add(Tile.of(suit, rank));
                }
                pendingRanks.clear();
            }
        }
        return tiles;
    }

    static Hand handOf(String spec) {
        Hand hand = new Hand();
        for (Tile t : parse(spec)) {
            hand.addTile(t);
        }
        return hand;
    }

    static PlayerPublicView riichiOpponent(String name, int seatIndex, List<Tile> discards) {
        return new PlayerPublicView(name, seatIndex, Wind.SOUTH, 25000, discards, List.of(), true);
    }

    static PlayerPublicView quietOpponent(String name, int seatIndex, List<Tile> discards) {
        return new PlayerPublicView(name, seatIndex, Wind.SOUTH, 25000, discards, List.of(), false);
    }

    static PlayerPublicView opponentWithPoints(String name, int seatIndex, Wind seatWind, int points) {
        return new PlayerPublicView(name, seatIndex, seatWind, points, List.of(), List.of(), false);
    }

    static AiContext context(Hand hand, List<PlayerPublicView> others) {
        return context(hand, others, List.of());
    }

    static AiContext context(Hand hand, List<PlayerPublicView> others, List<Tile> doraIndicators) {
        return new AiContext(hand, Wind.EAST, Wind.EAST, 1, 0, 60, doraIndicators, others, 5, 25000);
    }

    static AiContext context(Hand hand, Wind ownSeatWind, Wind roundWind, int kyokuNumber,
                              int ownPoints, List<PlayerPublicView> others) {
        return new AiContext(hand, ownSeatWind, roundWind, kyokuNumber, 0, 60, List.of(), others, 5, ownPoints);
    }
}
