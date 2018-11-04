/***
 * SentenceImpl class : used for representing the text exchanged between users
 * during a chat application
 * Contact: 
 *
 * Authors: 
 */

package irc;

public class SentenceImpl implements java.io.Serializable, Sentence {
    private String data;

    public SentenceImpl() {
        data = "";
    }

    public void write(String text) {
        data = text;
    }

    public String read() {
        return data;
    }
}
