package sahi351.mahjong.tile;

import java.util.Objects;

/**
 * 1枚の牌を表す不変な値オブジェクト。
 * 字牌の rank は 1=東 2=南 3=西 4=北 5=白 6=發 7=中 を表す。
 */
public final class Tile implements Comparable<Tile> {

    public static final int EAST = 1;
    public static final int SOUTH = 2;
    public static final int WEST = 3;
    public static final int NORTH = 4;
    public static final int HAKU = 5;
    public static final int HATSU = 6;
    public static final int CHUN = 7;

    private static final String[] HONOR_NAMES = {"", "東", "南", "西", "北", "白", "發", "中"};
    private static final String[] SUIT_LETTERS = {"m", "p", "s"};

    private final Suit suit;
    private final int rank;
    private final boolean redFive;

    public Tile(Suit suit, int rank, boolean redFive) {
        if (suit == Suit.JIHAI) {
            if (rank < 1 || rank > 7) {
                throw new IllegalArgumentException("字牌のrankは1-7: " + rank);
            }
            if (redFive) {
                throw new IllegalArgumentException("字牌は赤ドラになれない");
            }
        } else {
            if (rank < 1 || rank > 9) {
                throw new IllegalArgumentException("数牌のrankは1-9: " + rank);
            }
            if (redFive && rank != 5) {
                throw new IllegalArgumentException("赤ドラは5のみ");
            }
        }
        this.suit = suit;
        this.rank = rank;
        this.redFive = redFive;
    }

    public static Tile of(Suit suit, int rank) {
        return new Tile(suit, rank, false);
    }

    public static Tile redFive(Suit suit) {
        return new Tile(suit, 5, true);
    }

    public Suit suit() {
        return suit;
    }

    public int rank() {
        return rank;
    }

    public boolean isRedFive() {
        return redFive;
    }

    public boolean isHonor() {
        return suit == Suit.JIHAI;
    }

    public boolean isTerminal() {
        return !isHonor() && (rank == 1 || rank == 9);
    }

    public boolean isYaochuu() {
        return isHonor() || isTerminal();
    }

    public boolean isSimple() {
        return !isYaochuu();
    }

    public boolean isWindTile() {
        return isHonor() && rank >= EAST && rank <= NORTH;
    }

    public boolean isDragonTile() {
        return isHonor() && rank >= HAKU && rank <= CHUN;
    }

    /** ドラ表示牌からめくったドラ本体を返す。 */
    public Tile nextForDora() {
        if (suit == Suit.JIHAI) {
            if (rank >= EAST && rank <= NORTH) {
                int next = rank == NORTH ? EAST : rank + 1;
                return Tile.of(Suit.JIHAI, next);
            } else {
                int next = rank == CHUN ? HAKU : rank + 1;
                return Tile.of(Suit.JIHAI, next);
            }
        }
        int next = rank == 9 ? 1 : rank + 1;
        return Tile.of(suit, next);
    }

    /** 赤ドラかどうかを無視した通常牌としての比較値を返す。 */
    public boolean isSameKind(Tile other) {
        return this.suit == other.suit && this.rank == other.rank;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tile other)) return false;
        return suit == other.suit && rank == other.rank && redFive == other.redFive;
    }

    @Override
    public int hashCode() {
        return Objects.hash(suit, rank, redFive);
    }

    @Override
    public int compareTo(Tile other) {
        if (this.suit != other.suit) {
            return this.suit.compareTo(other.suit);
        }
        return Integer.compare(this.rank, other.rank);
    }

    @Override
    public String toString() {
        if (suit == Suit.JIHAI) {
            return HONOR_NAMES[rank];
        }
        String letter = SUIT_LETTERS[suit.ordinal()];
        int displayRank = redFive ? 0 : rank;
        return displayRank + letter;
    }
}
