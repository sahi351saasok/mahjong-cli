package sahi351.mahjong.game;

import sahi351.mahjong.tile.Tile;

public enum Wind {
    EAST(Tile.EAST),
    SOUTH(Tile.SOUTH),
    WEST(Tile.WEST),
    NORTH(Tile.NORTH);

    private final int tileRank;

    Wind(int tileRank) {
        this.tileRank = tileRank;
    }

    public int tileRank() {
        return tileRank;
    }

    public Wind next() {
        return values()[(ordinal() + 1) % 4];
    }

    public String label() {
        return switch (this) {
            case EAST -> "東";
            case SOUTH -> "南";
            case WEST -> "西";
            case NORTH -> "北";
        };
    }
}
