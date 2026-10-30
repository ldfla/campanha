package la.meleva.fundraiser.identity.dto;

import la.meleva.fundraiser.identity.User;
import java.time.Instant;
import java.util.List;

public record UserResponse(
        String id,
        String firstName,
        String surname,
        String phoneNumber,
        List<String> roles,
        Instant createdAt
) {
    public static UserResponse from(User user, List<String> roles) {
        return new UserResponse(
                user.getPublicId(),
                user.getDisplayName(),
                user.getSurname(),
                maskPhone(user.getPhoneNumber()),
                roles,
                user.getCreatedAt()
        );
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) return "****";
        int visibleSuffix = 4;
        String suffix = phone.substring(phone.length() - visibleSuffix);
        String prefix = phone.substring(0, phone.length() - visibleSuffix);
        return prefix.replaceAll("[\\d]", "*") + suffix;
    }
}
