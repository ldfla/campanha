package la.meleva.fundraiser.drawing;

/** Publicado na transação em que a apuração é publicada. */
public record DrawPublishedEvent(Long campaignId, Long drawId) { }
