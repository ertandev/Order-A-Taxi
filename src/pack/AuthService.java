package pack;

import javax.swing.JOptionPane;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * AuthService — Sequence Diagram: "1. Register & Login" bölümü.
 * OTP: Mock implementasyon (JOptionPane ile SMS simülasyonu).
 */
public class AuthService {

    private final UserRepository   userRepo;
    private final DriverRepository driverRepo;
    private final TaxiManager      manager; // in-memory lists için

    // email → otp kodu (geçici bellekte tutulur)
    private final Map<String, String> otpStore = new HashMap<>();
    private final Random random = new Random();

    public AuthService(TaxiManager manager) {
        this.manager    = manager;
        this.userRepo   = new UserRepository();
        this.driverRepo = new DriverRepository();
    }

    // ── OTP GÖNDER (Mock SMS — JOptionPane) ──────────────────────────────────
    /**
     * Sequence Diagram: register(name, email, pass, phone) → sendSingleNumber()
     * 6 haneli kod üretir, JOptionPane ile gösterir (Twilio olmadan simüle eder).
     */
    public String sendOTP(String email) {
        String code = String.format("%06d", random.nextInt(999999));
        otpStore.put(email, code);

        // SMS simülasyonu — diyalogda göster
        JOptionPane.showMessageDialog(
            null,
            "📱 SMS Simulation\n\nYour OTP Code: " + code + "\n\nEnter this code to verify.",
            "OTP Sent",
            JOptionPane.INFORMATION_MESSAGE
        );
        return code; // test için de dönülür
    }

    // ── OTP DOGRULA ───────────────────────────────────────────────────────────
    /**
     * Sequence Diagram: verifyOTP(phone, code) → User registered
     */
    public boolean verifyOTP(String email, String inputCode) {
        String stored = otpStore.get(email);
        if (stored != null && stored.equals(inputCode.trim())) {
            otpStore.remove(email); // tek kullanımlık
            return true;
        }
        return false;
    }

    // ── PASSENGER KAYIT ───────────────────────────────────────────────────────
    public Passenger registerPassenger(String name, String email, String password, String phone) {
        Passenger p = new Passenger(name, email, password, phone);
        manager.registerPassenger(p);  // in-memory + DB
        return p;
    }

    // ── DRIVER KAYIT ──────────────────────────────────────────────────────────
    public Driver registerDriver(String name, String email, String password, String phone,
                                  String vehicleModel, String plateNumber, VehicleType serviceType) {
        Driver d = new Driver(name, email, password, phone, vehicleModel, plateNumber, serviceType);
        manager.registerDriver(d);     // in-memory + DB
        return d;
    }

    // ── GIRIS ─────────────────────────────────────────────────────────────────
    public User login(String email, String password) {
        return manager.login(email, password);
    }

    // ── SIFRE SIFIRLA (Use Case: "Reset Password") ────────────────────────────
    /**
     * Sequence Diagram: Forgot Password → OTP → resetPassword(email, newPass)
     * Adımlar: sendOTP() → verifyOTP() → resetPassword()
     */
    public boolean resetPassword(String email, String newPassword) {
        boolean done = manager.resetPassword(email, newPassword);
        if (done) {
            new UserRepository().updatePassword(email, newPassword); // DB sync
        }
        return done;
    }

    // ── OTP TEMIZLE (iptal için) ───────────────────────────────────────────────
    public void clearOTP(String email) {
        otpStore.remove(email);
    }
}
