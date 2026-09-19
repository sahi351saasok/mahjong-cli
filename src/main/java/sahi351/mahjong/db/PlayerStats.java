package sahi351.mahjong.db;

/**
 * プレイヤーの通算成績。
 */
public record PlayerStats(double totalPoints, double avgRank, double winRate,
                           double nakiRate, double avgScore, double riichiRate) {
}
