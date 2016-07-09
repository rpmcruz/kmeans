// Kapplet.java

import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Kapplet extends JApplet
{
	public void init() {
		Kmeans panel = new Kmeans();
		add(panel);
		panel.init(getWidth(), getHeight());
	}
}

