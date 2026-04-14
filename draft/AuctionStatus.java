public enum AuctionStatus {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED;

    public boolean canTransitionTo(AuctionStatus nextStatus) {
        if (nextStatus == null) {
            return false;
        }

        return switch (this) {
            case OPEN -> nextStatus == RUNNING || nextStatus == CANCELED;
            case RUNNING -> nextStatus == FINISHED || nextStatus == CANCELED;
            case FINISHED -> nextStatus == PAID;
            case PAID, CANCELED -> false;
        };
    }

    public boolean allowsBidding() {
        return this == RUNNING;
    }

    public boolean isTerminal() {
        return this == PAID || this == CANCELED;
    }
}
