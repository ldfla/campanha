package la.meleva.fundraiser.compliance;

public interface PrivateDocumentStorage {
    String store(String filename, String contentType, byte[] content);
    byte[] load(String objectKey);
}
