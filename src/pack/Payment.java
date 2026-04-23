package pack;

import java.time.LocalDateTime;
import java.util.UUID;

// --- PAYMENT CLASS (Class Diagram + Object Diagram) ---
public class Payment {
    private UUID id = UUID.randomUUID();
    private double amount;
    private PaymentMethod method;
    private boolean isPaid;
    private String transactionId;
    private LocalDateTime paidAt;

    public Payment(double amount, PaymentMethod method) {
        this.amount = amount;
        this.method = method;
        this.isPaid = false;
        this.transactionId = "";
    }

    // Called after Stripe/Cash confirmation
    public void markPaid(String txnId) {
        this.isPaid = true;
        this.transactionId = txnId;
        this.paidAt = LocalDateTime.now();
    }

    public void markRefunded() {
        this.isPaid = false;
        this.transactionId = "REFUNDED";
    }

    public UUID getId() { return id; }
    public double getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public boolean isPaid() { return isPaid; }
    public String getTransactionId() { return transactionId; }
    public LocalDateTime getPaidAt() { return paidAt; }

    @Override
    public String toString() {
        String shortId = id.toString().substring(0, 8).toUpperCase();
        return String.format("PAY-%s | %.2f TL | %s | %s",
                shortId, amount, method,
                isPaid ? "PAID (" + transactionId + ")" : "PENDING");
    }
}
