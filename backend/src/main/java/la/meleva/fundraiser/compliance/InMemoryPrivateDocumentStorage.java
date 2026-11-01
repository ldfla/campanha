package la.meleva.fundraiser.compliance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Armazenamento em memória; padrão local/DEMO quando não há object storage configurado. */
@Component
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "memory", matchIfMissing = true)
class InMemoryPrivateDocumentStorage implements PrivateDocumentStorage {
    private final ConcurrentHashMap<String, byte[]> objects = new ConcurrentHashMap<>();
    public String store(String filename, String contentType, byte[] content) {
        String key = "private/" + UUID.randomUUID(); objects.put(key, content.clone()); return key;
    }
    public byte[] load(String objectKey) { var content = objects.get(objectKey); if (content == null) throw new IllegalArgumentException("Objeto inexistente"); return content.clone(); }
}
