package la.meleva.fundraiser.notification;

/** Canal de entrega. Implementações não recebem dados pessoais: só o id público do destinatário e o template. */
public interface NotificationTransport {
    void send(NotificationMessage message);

    record NotificationMessage(String recipientPublicId, NotificationTemplate template) { }
}
