package la.meleva.fundraiser.drawing;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import la.meleva.fundraiser.shared.ErrorCode;

/** Envelope AES-GCM: uma chave de dados independente é protegida pela chave configurada. */
final class SurpriseSecretCipher {
    private static final int NONCE_SIZE=12;
    private static final int KEY_SIZE=32;
    private static final int WRAPPED_KEY_SIZE=48;
    private final SecretKeySpec masterKey;
    private final SecureRandom random;

    SurpriseSecretCipher(String encodedKey,SecureRandom random) {
        Objects.requireNonNull(encodedKey,"encodedKey"); this.random=Objects.requireNonNull(random,"random");
        try { byte[] key=Base64.getDecoder().decode(encodedKey); if(key.length!=KEY_SIZE) throw new IllegalArgumentException(); this.masterKey=new SecretKeySpec(key,"AES"); Arrays.fill(key,(byte)0); }
        catch(IllegalArgumentException ex){throw new IllegalStateException("surprise.encryption_key.invalid",ex);}
    }

    String encrypt(String campaignId,SurpriseProof.Secret secret) {
        Objects.requireNonNull(campaignId,"campaignId"); Objects.requireNonNull(secret,"secret");
        byte[] dataKey=new byte[KEY_SIZE]; byte[] wrappingNonce=new byte[NONCE_SIZE]; byte[] contentNonce=new byte[NONCE_SIZE];
        random.nextBytes(dataKey); random.nextBytes(wrappingNonce); random.nextBytes(contentNonce);
        try {
            byte[] aad=("surprise-envelope-v1:"+campaignId).getBytes(StandardCharsets.UTF_8);
            var wrapping=cipher(Cipher.ENCRYPT_MODE,masterKey,wrappingNonce,aad);
            byte[] wrappedKey=wrapping.doFinal(dataKey);
            var content=cipher(Cipher.ENCRYPT_MODE,new SecretKeySpec(dataKey,"AES"),contentNonce,aad);
            String plain=secret.saltHex()+":"+secret.numbers().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
            byte[] encrypted=content.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            var buffer=java.nio.ByteBuffer.allocate(1+NONCE_SIZE+WRAPPED_KEY_SIZE+NONCE_SIZE+encrypted.length);
            return Base64.getEncoder().encodeToString(buffer.put((byte)1).put(wrappingNonce).put(wrappedKey).put(contentNonce).put(encrypted).array());
        } catch(GeneralSecurityException ex){throw new IllegalStateException("surprise.encryption.failed",ex);}
        finally {Arrays.fill(dataKey,(byte)0);}
    }

    SurpriseProof.Secret decrypt(String campaignId,String payload) {
        Objects.requireNonNull(campaignId,"campaignId"); Objects.requireNonNull(payload,"payload"); byte[] dataKey=null;
        try {
            byte[] bytes=Base64.getDecoder().decode(payload); if(bytes.length<1+NONCE_SIZE+WRAPPED_KEY_SIZE+NONCE_SIZE+16 || bytes[0]!=1) throw new IllegalArgumentException();
            var buffer=java.nio.ByteBuffer.wrap(bytes); buffer.get(); byte[] wrappingNonce=new byte[NONCE_SIZE]; byte[] wrappedKey=new byte[WRAPPED_KEY_SIZE]; byte[] contentNonce=new byte[NONCE_SIZE];
            buffer.get(wrappingNonce).get(wrappedKey).get(contentNonce); byte[] encrypted=new byte[buffer.remaining()]; buffer.get(encrypted);
            byte[] aad=("surprise-envelope-v1:"+campaignId).getBytes(StandardCharsets.UTF_8);
            dataKey=cipher(Cipher.DECRYPT_MODE,masterKey,wrappingNonce,aad).doFinal(wrappedKey);
            var plain=new String(cipher(Cipher.DECRYPT_MODE,new SecretKeySpec(dataKey,"AES"),contentNonce,aad).doFinal(encrypted),StandardCharsets.UTF_8).split(":",2);
            if(plain.length!=2) throw new IllegalArgumentException();
            return new SurpriseProof.Secret(Arrays.stream(plain[1].split(",")).map(Integer::valueOf).toList(),plain[0]);
        } catch(GeneralSecurityException | IllegalArgumentException ex){throw ErrorCode.DRAW_SURPRISE_COMMITMENT_FAILED.toException();}
        finally {if(dataKey!=null) Arrays.fill(dataKey,(byte)0);}
    }

    private Cipher cipher(int mode,SecretKeySpec key,byte[] nonce,byte[] aad) throws GeneralSecurityException {
        var cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(mode,key,new GCMParameterSpec(128,nonce)); cipher.updateAAD(aad); return cipher;
    }
}
