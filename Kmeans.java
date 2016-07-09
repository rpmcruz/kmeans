// Kmeans.java
// (C) 2013 GNU GPL Ricardo Cruz <ricardo.pdm.cruz@gmail.com>

/*

Algoritmo:

1 Escolher k pontos para atuarem como centros de cluster
2 Repetir até que não haja mudança nos centros de cluster
3	Alocar cada ponto ao cluster cujo centro é o mais
 	próximo
4	Assegurar que cada cluster tenha pelo menos um ponto
5	Substituir os centros de cluster com a média dos
 	elementos em seu cluster
6 fim Repetir

*/

import java.util.Random;
import java.util.LinkedList;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

@SuppressWarnings("serial")
public class Kmeans extends JPanel implements Runnable
{
	public class KmeansModel
	{
		static final int NPOINTS = 10000;
		static final int GAUSS_SD = 50;

		int points[][];
		int pointsk[];
		int kmeans[][];
		int gaussmeans[][] = null;
		boolean done;

		void distUniform(int width, int height) {
			Random rand = new Random();
			points = new int[NPOINTS][2];
			pointsk = new int[NPOINTS];
			for(int i = 0; i < NPOINTS; i++) {
				points[i][0] = rand.nextInt(width);
				points[i][1] = rand.nextInt(height);
				pointsk[i] = -1;
			}
		}

		private int gaussInt(Random rand, int mean, int sd, int min, int max) {
			int n = (int)(rand.nextGaussian() * sd);
			return Math.min(max, Math.max(min, n + mean));
		}

		// Gaussiana faz mais sentido para pôr os dados mais agrupados e
		// ver o algoritmo a funcionar

		void distGaussian(int w, int h, int nclusters) {
			Random rand = new Random();
			gaussmeans = new int[nclusters][2];
			for(int i = 0; i < nclusters; i++) {
				gaussmeans[i][0] = rand.nextInt(w-GAUSS_SD*2) + GAUSS_SD;
				gaussmeans[i][1] = rand.nextInt(h-GAUSS_SD*2) + GAUSS_SD;
			}
			points = new int[NPOINTS][2];
			pointsk = new int[NPOINTS];
			for(int i = 0; i < NPOINTS; i++) {
				int k = i % nclusters;
				points[i][0] = gaussInt(rand, gaussmeans[k][0], GAUSS_SD, 0, w);
				points[i][1] = gaussInt(rand, gaussmeans[k][1], GAUSS_SD, 0, h);
				pointsk[i] = -1;
			}
		}
		
		/* nclusters and nkernels may differ in that nclusters is how many
		   clusters you actually want to create, while nkernels is how many
		   clusters the kmeans algorithm will assume */
		
		public void init(int nclusters, int width, int height) {
			done = false;
			//distUniform(width, height);
			distGaussian(width, height, nclusters);
			setKernels(nclusters);
		}

		public void setKernels(int nkernels) {
			kmeans = new int[nkernels][2];		
			for(int i = 0; i < nkernels; i++) {
				pointsk[i] = i;
				kmeans[i][0] = points[i][0];
				kmeans[i][1] = points[i][1];
			}
		}
		
		public boolean step() {
			done = true;

			// alocar cada ponto ao cluster cujo centro é o mais próximo

			for(int p = 0; p < NPOINTS; p++) {
				int min_dist = Integer.MAX_VALUE;
				int min_k = 0;
				for(int k = 0; k < kmeans.length; k++) {
					int dx = kmeans[k][0] - points[p][0];
					int dy = kmeans[k][1] - points[p][1];
					int dist = dx*dx + dy*dy;
					if(dist < min_dist) {
						min_dist = dist;
						min_k = k;
					}
				}
				if(pointsk[p] != min_k)
					done = false;
				pointsk[p] = min_k;
			}

			// assegurar que cada cluster tenha pelo menos um ponto

			int kpoints[] = new int[kmeans.length];
			for(int k = 0; k < kpoints.length; k++)
				kpoints[k] = 0;
			for(int p = 0; p < NPOINTS; p++) {
				int k = pointsk[p];
				kpoints[k]++;
			}
			for(int k = 0; k < kpoints.length; k++)
				while(kpoints[k] == 0) {
					Random rand = new Random();
					int p = rand.nextInt(kpoints.length);
					if(kpoints[pointsk[p]] > 1) {
						pointsk[p] = k;
						kpoints[k]++;
					}
				}

			// substituir os centros de cluster com a média dos elementos
			// em seu cluster

			for(int k = 0; k < kmeans.length; k++) {
				kpoints[k] = 0;
				kmeans[k][0] = 0;
				kmeans[k][1] = 0;
			}

			for(int p = 0; p < NPOINTS; p++) {
				int k = pointsk[p];
				kpoints[k]++;
				kmeans[k][0] += points[p][0];
				kmeans[k][1] += points[p][1];
			}
			for(int k = 0; k < kmeans.length; k++) {
				kmeans[k][0] /= kpoints[k];
				kmeans[k][1] /= kpoints[k];
			}
			
			return done;
		}

		/* We'll calculate the error of the algorithm by the distance
		   between the kmeans points and the generic points. */

		// using the Silhouette method:
		// http://en.wikipedia.org/wiki/Silhouette_(clustering)

		private long distpoints(int p1, int p2) {
			long dx = points[p1][0] - points[p2][0];
			long dy = points[p1][1] - points[p2][1];
			return dx*dx + dy*dy;
		}

		private long distkpoints(int p, int k) {
			long dist = 0;
			for(int p_ = 0; p_ < NPOINTS; p_++)
				if(pointsk[p_] == k)
					dist += distpoints(p, p_);
			return dist;
		}

		private long a(int p) {
			return distkpoints(p, pointsk[p]);
		}

		public int closestk(int p) {
			long distmin = Long.MAX_VALUE;
			int kmin = 0;
			for(int k = 0; k < kmeans.length; k++) {
				if(k != pointsk[p]) {
					long dx = kmeans[k][0] - points[p][0];
					long dy = kmeans[k][1] - points[p][1];
					long dist = dx*dx + dy*dy;
					if(dist < distmin) {
						distmin = dist;
						kmin = k;
					}
				}
			}
			return kmin;
		}

		private long b(int p) {
			return distkpoints(p, closestk(p));
		}

		private double error(int i) {
			double a = a(i);
			double b = b(i);
			return (b-a) / Math.max(a,b);
		}

		public double error() {
			double error = 0;
			for(int p = 0; p < NPOINTS; p++)
				error += error(p);
			return error / NPOINTS;
		}


/*
		// pela distância -- não dá ...
		public long error() {
			long error = 0;
			for(int p = 0; p < points.length; p++) {
				int k = pointsk[p];
				long dx = kmeans[k][0] - points[p][0];
				long dy = kmeans[k][1] - points[p][1];
				error += dx*dx + dy*dy;
			}
			return error;
		}
		*/
	}

	public class KmeansView extends JPanel
	{
		final Color COLORS[] = {
			Color.WHITE, Color.YELLOW, Color.ORANGE, Color.PINK, Color.RED,
				Color.GREEN, Color.BLUE, new Color(255,128,0),
				new Color(255,0,255),
		};

		private Color getColor(int k) {
			int clen = COLORS.length-1;
			return COLORS[(k%clen)+1];
		}

		public void paint(Graphics g) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());

/*			for(int i = 0; i < NCLUSTERS; i++) {
				g.setColor(new Color(40, 40, 40));
				int x = gaussmeans[i][0], y = gaussmeans[i][1];
				int w = GAUSS_SD*2;
				g.fillOval(x, y, w, w);
			}*/
			
			for(int i = 0; i < model.points.length; i++) {
				int x = model.points[i][0], y = model.points[i][1];
				g.setColor(getColor(model.pointsk[i]));
				g.fillOval(x, y, 4, 4);
			}
			for(int i = 0; i < model.kmeans.length; i++) {
				g.setColor(Color.CYAN);
				int x = model.kmeans[i][0], y = model.kmeans[i][1];
				int sz = 3;
				for(int lw = 0; lw <= 3; lw++) {
					g.drawLine(x-sz+lw, y-sz, x+sz+lw, y+sz);
					g.drawLine(x-sz+lw, y+sz, x+sz+lw, y-sz);
				}
			}
		}
	}

	/* Ricardo: I lost so much time playing with java layout managers...
	   will just implement my own (loosely based on gtk) */

	public class MyBoxLayout implements LayoutManager
	{
		class Constrains {
			boolean stretch;
			Component comp;
			Constrains(Component comp, boolean stretch) {
				this.comp = comp;
				this.stretch = stretch;
			}
		}
	
		private LinkedList <Constrains> comps = new LinkedList <Constrains>();
		private int axis;  // can be BoxLayout.X_AXIS or Y_AXIS
		private int nstretch = 0;
		private int spacing;
	
		MyBoxLayout(int axis, int spacing) {
			this.axis = axis;
			this.spacing = spacing;
		}

		private void add(Component comp, boolean stretch) {
			comps.add(new Constrains(comp, stretch));
			if(stretch)
				nstretch++;
		}
		
		public void layoutContainer(Container parent) {
			Dimension pdim = preferredLayoutSize(parent);

			if(axis == BoxLayout.X_AXIS) {
				int swidth = 0, x = 0;
				int pwidth = getWidth() - pdim.width;
				if(nstretch > 0)
					swidth = pwidth / nstretch;
				for(Constrains c : comps) {
					int width;
					if(c.stretch)
						width = swidth;
					else
						width = c.comp.getPreferredSize().width;
					c.comp.setBounds(x, 0, width, parent.getHeight());
					x += width + spacing;
				}
			}
			else {  // Y_AXIS
				int sheight = 0, y = 0;
				int pheight = getHeight() - pdim.height;
				if(nstretch > 0)
					sheight = pheight / nstretch;
				for(Constrains c : comps) {
					int height;
					if(c.stretch)
						height = sheight;
					else
						height = c.comp.getPreferredSize().height;
					c.comp.setBounds(0, y, parent.getWidth(), height);
					y += height + spacing;
				}
			}
		}

		public Dimension preferredLayoutSize(Container parent) {
			int width = 0, height = 0;
			for(Constrains c : comps) {
				Dimension d = c.comp.getPreferredSize();
				if(axis == BoxLayout.X_AXIS) {
					if(!c.stretch)
						width += d.width;
					height = Math.max(height, d.height);
				}
				else {  // Y_AXIS
					if(!c.stretch)
						height += d.height;
					width = Math.max(width, d.width);
				}
			}
			if(axis == BoxLayout.X_AXIS)
				width += spacing * (comps.size()-1);
			else  // Y_AXIS
				height += spacing * (comps.size()-1);
			return new Dimension(width, height);
		}

		public Dimension minimumLayoutSize(Container parent)
		{ return preferredLayoutSize(parent); }

		public void addLayoutComponent(String name, Component comp)
		{ add(comp, name.compareTo("s") == 0); }

		// not implemented
		public void removeLayoutComponent(Component comp) {}
	}

	public class Status extends JPanel implements ActionListener, ChangeListener
	{
		JSlider slider;
		JButton button;
		JLabel slider_label;
		JLabel label;
		Font font;

		Status() {
			setLayout(new MyBoxLayout(BoxLayout.X_AXIS, 4));
			slider = new JSlider(1, 10, 4);
			slider.addChangeListener(this);
			button = new JButton("Start");
			button.addActionListener(this);
			label = new JLabel();
			slider_label = new JLabel("4");
			add(label, "s");
			add(slider, "s");
			add(slider_label, "");
			add(button, "");
			font = label.getFont();
			setBackground(Color.WHITE);
			slider.setBackground(Color.WHITE);
		}
		
		public void stateChanged(ChangeEvent ev) {
			label.setText("");
			reset();
			slider_label.setText(String.valueOf(slider.getValue()));
		}
		
		public void actionPerformed(ActionEvent ev) {
			label.setText("");
			start();
		}
		
		public void setEnabled(boolean enabled) {
			slider.setEnabled(enabled);
			button.setEnabled(enabled);
		}
		
		public int getGaussClusters() {
			return slider.getValue();
		}
		
		public void setMessage(String text, boolean highlight) {
			label.setText(text);
			if(highlight) {
				label.setFont(font.deriveFont(Font.BOLD));
				label.setForeground(Color.RED);
			}
			else {
				label.setFont(font);
				label.setForeground(null);
			}
		}
	}

	KmeansModel model;
	KmeansView view;
	Status status;

	public void init(int width, int height) {
/*		try {
			UIManager.setLookAndFeel(
				UIManager.getSystemLookAndFeelClassName());
		} catch(Exception ex) {}*/

		setLayout(new MyBoxLayout(BoxLayout.Y_AXIS, 0));
		status = new Status();
		model = new KmeansModel();
		view = new KmeansView();
		model.init(4, width, height);
		add(view, "s");
		add(status, "");
	}

	public void reset() {
		model.init(status.getGaussClusters(), getWidth(), getHeight());
		view.repaint();
	}

	public void start() {
		Thread t = new Thread(this);
		t.start();
	}

	public double runSimulation(int kernels) {
		final int PAUSE = 500;
		model.setKernels(kernels);
		while(!model.step()) {
			view.repaint();
			try { Thread.sleep(PAUSE); } catch(Exception ex) {}
		}
		status.setMessage("error: (estimating)", false);
		return model.error();
	}

	public void run() {
		status.setEnabled(false);

		if(model.done) {
			reset();
			try { Thread.sleep(1000); } catch(Exception ex) {}
		}

		double error = 0;
		int kernels;
		for(kernels = 2; ; kernels++) {
			status.setMessage("kernels: " + kernels, false);
			double e = runSimulation(kernels);
			status.setMessage("error: " + (e), false);
			try { Thread.sleep(1500); } catch(Exception ex) {}
			if(e < error)
				break;
			error = e;
		}
		
		kernels = kernels-1;
		model.setKernels(kernels);
		while(!model.step()) ;
		view.repaint();
		status.setMessage("kernels: " + kernels, true);

		status.setEnabled(true);
	}

	public static void main(String [] args) {
	    System.out.println("main!");
		JFrame frame = new JFrame("Kmeans");
		Kmeans panel = new Kmeans();
		frame.add(panel);
		frame.setSize(350, 350);
		frame.setVisible(true);
		frame.setResizable(false);
		panel.init(350, 350);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
}

