package pack;

// --- CANCELLATION POLICY (Class Diagram + State Machine Diagram) ---
// State Machine: REQUESTED->CANCELLED = no penalty
//                ACCEPTED->CANCELLED  = 25% penalty
//                IN_PROGRESS->CANCELLED = 50% penalty
public class CancellationPolicy {

    private static final int MAX_FREE_CANCELLATIONS = 3;
    private static final double AFTER_ACCEPTANCE_PENALTY = 0.25;
    private static final double IN_PROGRESS_PENALTY = 0.50;

    public static int getMaxFreeCancellations() {
        return MAX_FREE_CANCELLATIONS;
    }

    /**
     * Calculates penalty amount based on ride's current state (State Machine Diagram).
     */
    public static double calculatePenalty(Ride ride) {
        if (ride == null) return 0.0;
        switch (ride.getStatus()) {
            case REQUESTED:   return 0.0;
            case ACCEPTED:    return ride.getFareAmount() * AFTER_ACCEPTANCE_PENALTY;
            case IN_PROGRESS: return ride.getFareAmount() * IN_PROGRESS_PENALTY;
            default:          return 0.0;
        }
    }

    /**
     * Calculates refund amount = total fare - penalty.
     */
    public static double calculateRefund(Ride ride) {
        return Math.max(0, ride.getFareAmount() - calculatePenalty(ride));
    }
}
