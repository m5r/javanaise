/***
 * Irc class : simple implementation of a chat using JAVANAISE
 * Contact: 
 *
 * Authors: 
 */

package irc;

import java.awt.*;
import java.awt.event.*;

import jvn.JvnProxy;

public class Irc {
    public TextArea text;
    public TextField data;
    Frame frame;
    Sentence sentence;

    /**
     * main method
     * create a JVN object nammed IRC for representing the Chat application
     **/
    public static void main(String argv[]) {
        try {
            Sentence sentence = (Sentence) JvnProxy.get("IRC", new SentenceImpl());

            new Irc(sentence);
        } catch (Exception e) {
            System.out.println("IRC problem : " + e.getMessage());
        }
    }

    /**
     * IRC Constructor
     *
     * @param sentence the JVN object representing the Chat
     **/
    public Irc(Sentence sentence) {
        this.sentence = sentence;
        frame = new Frame();
        frame.setLayout(new GridLayout(1, 1));
        text = new TextArea(10, 60);
        text.setEditable(false);
        text.setForeground(Color.red);
        frame.add(text);
        data = new TextField(40);
        frame.add(data);
        Button read_button = new Button("read");
        read_button.addActionListener(new readListener(this));
        frame.add(read_button);
        Button write_button = new Button("write");
        write_button.addActionListener(new writeListener(this));
        frame.add(write_button);
        frame.setSize(545, 201);
        text.setBackground(Color.black);
        frame.setVisible(true);
    }
}

/**
 * Internal class to manage user events (read) on the CHAT application
 **/
class readListener implements ActionListener {
    Irc irc;

    public readListener(Irc i) {
        irc = i;
    }

    /**
     * Management of user events
     **/
    public void actionPerformed(ActionEvent e) {
        // invoke the method
        String s = irc.sentence.read();

        // display the read value
        irc.data.setText(s);
        irc.text.append(s + "\n");
    }
}

/**
 * Internal class to manage user events (write) on the CHAT application
 **/
class writeListener implements ActionListener {
    Irc irc;

    public writeListener(Irc i) {
        irc = i;
    }

    /**
     * Management of user events
     **/
    public void actionPerformed(ActionEvent e) {
        // get the value to be written from the buffer
        String s = irc.data.getText();

        // invoke the method
        irc.sentence.write(s);
    }
}
