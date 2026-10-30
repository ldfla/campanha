package la.meleva.fundraiser.identity;

public interface ContactVerificationProvider {
    void sendVerification(String phoneNumber);
    boolean verify(String phoneNumber, String code);
}
