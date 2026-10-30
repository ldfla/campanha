package la.meleva.fundraiser.participation;

import java.util.List;

public record ParticipationAllocation(String requestPublicId, List<AssignedNumber> numbers) {
    public record AssignedNumber(String entryPublicId, String seriesCode, int numberValue, String formattedNumber) {}
}
