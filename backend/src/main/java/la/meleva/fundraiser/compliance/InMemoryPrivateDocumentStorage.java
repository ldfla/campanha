package la.meleva.fundraiser.compliance;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Armazenamento exclusivamente DEMO; não deve ser ativado em produção. */
@Component
@Profile({"default", "test"})
class InMemoryPrivateDocumentStorage implements PrivateDocumentStorage {
    private final ConcurrentHashMap<String, byte[]> objects = new ConcurrentHashMap<>();
    public String store(String filename, String contentType, byte[] content) {
        String key = "private/" + UUID.randomUUID(); objects.put(key, content.clone()); return key;
    }
    public byte[] load(String objectKey) { var content = objects.get(objectKey); if (content == null) throw new IllegalArgumentException("Objeto inexistente"); return content.clone(); }
}
