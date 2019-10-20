package com.velocitypowered.natives.encryption;

import com.google.common.base.Preconditions;
import com.velocitypowered.natives.util.BufferPreference;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

public class JavaVelocityCipher implements VelocityCipher {

  public static final VelocityCipherFactory FACTORY = new VelocityCipherFactory() {
    @Override
    public VelocityCipher forEncryption(SecretKey key) throws GeneralSecurityException {
      return new JavaVelocityCipher(true, key);
    }

    @Override
    public VelocityCipher forDecryption(SecretKey key) throws GeneralSecurityException {
      return new JavaVelocityCipher(false, key);
    }
  };

  private final Cipher cipher;
  private boolean disposed = false;

  private JavaVelocityCipher(boolean encrypt, SecretKey key) throws GeneralSecurityException {
    this.cipher = Cipher.getInstance("AES/CFB8/NoPadding");
    this.cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key,
        new IvParameterSpec(key.getEncoded()));
  }

  @Override
  public void process(ByteBuf source) {
    ensureNotDisposed();
    Preconditions.checkArgument(source.hasArray(), "No source array");

    int inBytes = source.readableBytes();

    try {
      cipher.update(source.array(), source.arrayOffset(), inBytes, source.array(),
          source.arrayOffset());
    } catch (ShortBufferException ex) {
      /* This _really_ shouldn't happen - AES CFB8 will work in place.
         If you run into this, that means that for whatever reason the Java Runtime has determined
         that the output buffer needs more bytes than the input buffer. When we are working with
         AES-CFB8, the output size is equal to the input size. See the problem? */
      throw new AssertionError("Cipher update did not operate in place and requested a larger "
              + "buffer than the source buffer");
    }
  }

  @Override
  public void dispose() {
    disposed = true;
  }

  private void ensureNotDisposed() {
    Preconditions.checkState(!disposed, "Object already disposed");
  }

  @Override
  public BufferPreference preferredBufferType() {
    return BufferPreference.HEAP_REQUIRED;
  }
}
