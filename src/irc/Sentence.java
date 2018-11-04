package irc;

import jvn.JvnObjectInterceptor;
import jvn.LockType;

public interface Sentence {
    @JvnObjectInterceptor(lockType = LockType.write)
    public void write(String text);

    @JvnObjectInterceptor(lockType = LockType.read)
    public String read();
}
