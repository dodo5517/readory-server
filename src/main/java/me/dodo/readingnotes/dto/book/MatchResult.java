package me.dodo.readingnotes.dto.book;

// 매칭 결과 객체
public class MatchResult {
    private BookCandidate best;
    private double score;
    private boolean autoMatch;

    public MatchResult(BookCandidate best, double score, boolean autoMatch) {
        this.best = best;
        this.score = score;
        this.autoMatch = autoMatch;
    }

    public BookCandidate getBest() {
        return best;
    }

    public void setBest(BookCandidate best) {
        this.best = best;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public boolean isAutoMatch() {
        return autoMatch;
    }

    public void setAutoMatch(boolean autoMatch) {
        this.autoMatch = autoMatch;
    }
}