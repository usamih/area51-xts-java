/* 
 * Copyright (c) 1998-2016 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, 
 * and/or its subsidiaries and/or its affiliates and/or their licensors. Use, reproduction, transfer, 
 * publication or disclosure is prohibited except as specifically provided for in your License Agreement 
 * with Software AG.
 */

package com.softwareag.adabas.xts.test;

import java.awt.*;

/**
 * This class implements a table object. A table object is a graphical component
 * which has a fixed number of columns, but a varying number of rows. The user
 * specifies the number of columns on creation of an instance, or using the
 * <i>clear</i> method. The table will adjust the column widths to accomodate
 * the biggest field in the column.
 ** 
 * @version 2.1.0.4
 **/
public final class Table extends Panel implements LayoutManager {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2581207533617094192L;
	private int columns;
	private TableElement head = null;
	private TableElement tail = null;
	private TableElement newhead = null;
	private int xpad = 0;
	private int ypad = 0;
	private boolean hline = false;
	private boolean vline = false;
	private int[] colwidth;
	private int[] rowheight;
	private int hilite = -1;
	private int components = 0;
	private int maxX = 100;
	private int maxY = 100;
	private int x_offset = 0;
	private int y_offset = 0;
	private int rows = -1;
	private Image img = null;
	private int imageWidth = -1;
	private int imageHeight = -1;
	private int locked = 0;

	static TableElement free = null;

	/**
	 * Constructor. Assumes padding factors of zero.
	 ** 
	 * @param cols
	 *            number of columns in the table.
	 **/
	public Table(final int cols) {
		this(cols, 0);
	}

	/**
	 * Constructor.
	 ** 
	 * @param cols
	 *            number of columns in the table.
	 ** @param xpad
	 *            the number of pixels padding factor between columns.
	 **/
	public Table(final int cols, final int xpad) {
		this(cols, xpad, 0);
	}

	/**
	 * Constructor.
	 ** 
	 * @param cols
	 *            number of columns in the table.
	 ** @param xpad
	 *            the number of pixels padding factor between columns.
	 ** @param ypad
	 *            the number of pixels padding factor between rows.
	 **/
	public Table(final int cols, final int xpad, final int ypad) {
		this.xpad = xpad;
		this.ypad = ypad;
		columns = cols;
		super.setLayout(this);
	}

	/**
	 * Clear the table. The Table is hidden until <i>allow_paint()</i> is
	 * invoked.
	 ** 
	 * @param cols
	 *            new number of columns in the table.
	 **/
	public void clear(final int cols) {
		columns = cols;
		clear();
	}

	/**
	 * Clear the table. The Table is hidden until <i>allow_paint()</i> is
	 * invoked.
	 **/
	public synchronized void clear() {
		locked++;
		if (locked > 1) {
			try {
				wait(1000);
			} catch (InterruptedException ie) {
				locked = 1;
			}
		}
		tail = null;
	}

	/**
	 * allow painting. Must be called after <i>clear()</i> to make the table
	 * visible.
	 **/
	public void allow_paint() {
		setVisible(false);

		TableElement t;

		while (head != null) {
			if (head.value instanceof Component) {
				t = newhead;
				while (t != null && t.value != head.value) {
					t = t.next;
				}
				if (t != null) {
					t.must_add = false;
				} else {
					super.remove((Component) head.value);
				}
			}

			t = head;
			head = head.next;
			free(t);
		}

		hilite = -1;
		x_offset = 0;
		y_offset = 0;
		components = 0;

		t = newhead;
		while (t != null) {
			components++;
			if (t.must_add) {
				super.add((Component) t.value);
			}
			t = t.next;
		}

		head = newhead; // take the new lot
		rows = -1;
		Container c = getParent();
		if (c != null && c.isVisible()) {
			layoutContainer(c);
		}
		setVisible(true);
		synchronized (this) {
			locked--;
			if (locked > 0) {
				notify();
			}
		}
	}

	/**
	 * Set vertical lines on or off.
	 ** 
	 * @param b
	 *            true=on, false=off.
	 **/
	public void vlines(final boolean b) {
		vline = b;
	}

	/**
	 * Set horizontal lines on or off.
	 ** 
	 * @param b
	 *            true=on, false=off.
	 **/
	public void hlines(final boolean b) {
		hline = b;
	}

	/**
	 * Add a string to the table. The next available cell will be used.
	 ** 
	 * @param s
	 *            the string to add.
	 **/
	public void add(final String s) {
		add(s, false);
	}

	/**
	 * Add a string to the table. The next available cell will be used.
	 ** 
	 * @param s
	 *            the string to add.
	 ** @param rightjust
	 *            indicates whether the string should be right justified in the
	 *            field.
	 **/
	public void add(final String s, final boolean rightjust) {
		addObject(s, false, true, true).right = rightjust;
	}

	/**
	 * Add a component to the table. The next available cell will be used.
	 ** 
	 * @param c
	 *            the component to add.
	 **/
	public Component add(final Component c) {
		return add(c, false);
	}

	/**
	 * Add a component to the table. The next available cell will be used.
	 ** 
	 * @param c
	 *            the component to add.
	 ** @param leave
	 *            true=keep component preferred size, false=stretch component to
	 *            fit cell.
	 **/
	public Component add(final Component c, final boolean leave) {
		addObject(c, leave, true, leave);
		return c;
	}

	/**
	 * Add a component to the table. The next available cell will be used.
	 ** 
	 * @param c
	 *            the component to add.
	 ** @param leave
	 *            true=keep component preferred size, false=stretch component to
	 *            fit cell.
	 ** @param movex
	 *            true=move component in x-plane, false=do not move component in
	 *            x-plane.
	 ** @param movey
	 *            true=move component in y-plane, false=do not move component in
	 *            y-plane.
	 **/
	public Component add(final Component c, final boolean leave,
			final boolean movex, final boolean movey) {
		addObject(c, leave, movex, movey);
		return c;
	}

	private TableElement addObject(final Object o, final boolean leave,
			final boolean movex, final boolean movey) {
		TableElement t = newTableElement();
		t.must_add = (o instanceof Component); // means - must add
		t.value = o;
		if (tail == null) {
			newhead = t;
		} else {
			tail.next = t;
		}
		tail = t;
		t.next = null;
		t.expand = !leave;
		t.movex = movex;
		t.movey = movey;
		components++;
		if (locked > 0) {
			rows = -1;
		}
		return t;
	}

	/** get column widths. **/
	public int[] getColumnWidths() {
		return colwidth;
	}

	/**
	 * Set a row hilited. Usually used when a mouse clicked.
	 ** 
	 * @param x
	 *            the x position of the mouse relative to this component.
	 ** @param y
	 *            the y position of the mouse relative to this component.
	 ** @param min
	 *            the first row to consider to hiliting.
	 ** @param max
	 *            the last row to consider for hiliting.
	 **/
	public int setHiLite(final int x, final int y, final int min, final int max) {
		int oldhilite = hilite;
		hilite = 0;
		int y1 = rowheight[0] + ypad - y_offset;
		for (int i = 1; y1 < y && i < rowheight.length; i++) {
			hilite++;
			y1 += rowheight[i] + ypad;
		}
		if (hilite < min || hilite > max) {
			hilite = -1;
		}
		if (oldhilite != -1) {
			repaint();
		} else {
			if (hilite != -1) {
				Graphics g = getGraphics();
				if (g != null) {
					paint(g);
				}
			}
		}
		return hilite;
	}

	/**
	 * Add an offset to the current offset. Used to smooth scroll a table.
	 ** 
	 * @param x_delta
	 *            the number of pixels offset in the columns.
	 ** @param y_delta
	 *            the number of pixels offset in the rows.
	 **/
	public void addOffset(final int x_delta, final int y_delta) {
		if (x_delta != 0) {
			setXoffset(x_offset + x_delta);
		}
		if (y_delta != 0) {
			setYoffset(y_offset + y_delta);
		}
	}

	/** Set column offset. **/
	public synchronized void setXoffset(final int x) {
		TableElement t = head;
		while (t != null) {
			if (t.value instanceof Component && t.movex) {
				Component cp = (Component) t.value;
				Point p = cp.getLocation();
				p.x += (x_offset - x);
				cp.setLocation(p);
			}
			t = t.next;
		}
		x_offset = x;
	}

	/** Set row offset. **/
	public synchronized void setYoffset(final int y) {
		TableElement t = head;
		while (t != null) {
			if (t.value instanceof Component && t.movey) {
				Component cp = (Component) t.value;
				Point p = cp.getLocation();
				p.y += (y_offset - y);
				cp.setLocation(p);
			}
			t = t.next;
		}
		y_offset = y;
	}

	public void update(final Graphics g) {
		paint(g);
	}

	// ////////////////////////////////////////////////////////////////////
	public void paint(final Graphics g) { // new
											// Throwable().printStackTrace(System.out);
		if (head == null) {
			return;
		}
		FontMetrics fm = g.getFontMetrics();
		int row;
		int[] rowheight;
		int[] colwidth;
		int comp;
		do {
			comp = components; // get a snapshot
			getMetrics(fm);
			row = rows;
			rowheight = this.rowheight;
			colwidth = this.colwidth; // narrow the window somewhat
		} while (comp != components);

		Color fg = g.getColor();

		Rectangle clip = g.getClipBounds(); // find out where we are
		if (clip == null) {
			clip = new Rectangle(0, 0, maxX + 2, maxY + 2);
		}
		if (clip.width > (maxX + 2)) {
			clip.width = maxX + 2;
		}
		if (clip.height > (maxY + 2)) {
			clip.height = maxY + 2;
		}

		if (imageWidth != clip.width || imageHeight != clip.height) {
			img = createImage(clip.width, clip.height);
			imageWidth = clip.width;
			imageHeight = clip.height;
		}

		Graphics g2 = img.getGraphics();
		g2.setColor(getBackground());
		g2.fillRect(0, 0, clip.width, clip.height);
		int clip_end = y_offset + clip.height + 1;

		try {
			if (hilite != -1 && !(row < hilite)) {
				g2.setColor(new Color(127, 127, 127));
				int ytot = 0;
				for (int i = 0; i < hilite; i++) {
					ytot += rowheight[i] + ypad;
				}
				if (x_offset == 0) {
					g2.fillRect(1, ytot - y_offset, maxX, rowheight[hilite]
							+ ypad);
				} else {
					g2.fillRect(0, ytot - y_offset, maxX, rowheight[hilite]
							+ ypad);
				}
			}

			g2.setColor(fg);

			TableElement t = head;
			int x = xpad / 2;
			int y = ypad / 2;
			int col = 0;
			row = -1;
			int delta = fm.getLeading() + fm.getAscent() - fm.getHeight() / 2
					- y_offset;

			int nextrow = 0;
			while (t != null) {
				if (col == 0) {
					row++;
					if (nextrow > clip_end) {
						break; // past end, stop now
					}
					nextrow += rowheight[row] + ypad;
				}
				if (nextrow > (y_offset - 1)) { // before the beginning?

					if (t.value instanceof String) {
						if (t.right) {
							int xx = x - x_offset + colwidth[col]
									- fm.stringWidth((String) t.value);
							g2.drawString((String) t.value, xx, y + delta
									+ rowheight[row] / 2);
						} else {
							g2.drawString((String) t.value, x - x_offset, y
									+ delta + rowheight[row] / 2);
						}
					} else {
						if (t.value instanceof Container) {
							Rectangle r = ((Component) t.value).getBounds();
							Graphics gg = g2
									.create(r.x, r.y, r.width, r.height);
							((Component) t.value).paintAll(gg);
							gg.dispose();
						}
					}
				}

				x += colwidth[col] + xpad;
				col = (++col) % columns;
				if (col == 0) {
					if (hline && y == ypad / 2 && y_offset == 0) {
						g2.drawLine(0, 0, x - xpad / 2, 0);
					}
					y += rowheight[row] + ypad;
					if (hline && nextrow > (y_offset - 1)) {
						g2.drawLine(0, y - ypad / 2 - y_offset, x - xpad / 2, y
								- ypad / 2 - y_offset);
					}
					x = xpad / 2;
				}
				t = t.next;
			}

			if (col != 0) {
				y += rowheight[row] + ypad;
			}

			if (vline) {
				x = -x_offset;
				if (clip_end > maxY) {
					clip_end = maxY;
				}
				for (int i = 0; i < columns; i++) {
					g2.drawLine(x, 0, x, clip_end - y_offset);
					x += colwidth[i] + xpad;
				}
				g2.drawLine(x, 0, x, clip_end - y_offset);
			}

			g2.dispose();

			g.drawImage(img, 0, 0, this);
		} catch (ArrayIndexOutOfBoundsException aiob) {
		}
	}

	private synchronized void getMetrics(final FontMetrics fm) {
		if (rows > 0) {
			return;
		}
		int[] rowheight = new int[(components - 1) / columns + 1];
		int[] colwidth = new int[columns];
		int col = 0;
		int width;
		int height;
		int row = -1;
		TableElement t = head;

		try {
			while (t != null) {
				if (col == 0) {
					row++;
				}
				if (t.value instanceof String) {
					width = fm.stringWidth((String) t.value);
					height = fm.getHeight();
				} else {
					Dimension d = ((Component) t.value).getPreferredSize();
					width = d.width;
					height = d.height;
				}
				if (colwidth[col] < width) {
					colwidth[col] = width;
				}
				if (rowheight[row] < height) {
					rowheight[row] = height;
				}
				col = (++col) % columns;
				t = t.next;
			}

			maxY = 0;
			for (int i = row; i > -1; i--) {
				maxY += rowheight[i] + ypad;
			}
			maxX = 0;
			for (col = columns - 1; col > -1; col--) {
				maxX += colwidth[col] + xpad;
			}

			// setSize(max_x+2,max_y+2);

			this.rowheight = rowheight;
			this.colwidth = colwidth;
			rows = row;
		} catch (Exception e) {
		}
	}

	private void layout(final FontMetrics fm) {
		getMetrics(fm);
		TableElement t = head;
		int x = xpad / 2;
		int y = ypad / 2;
		int col = 0;
		int row = -1;
		// int delta=fm.getLeading()+fm.getAscent();

		while (t != null) {
			if (col == 0) {
				row++;
			}
			if (!(t.value instanceof String)) {
				Component cp = (Component) t.value;
				Rectangle r;
				if (!t.expand) {
					Dimension d = cp.getPreferredSize();
					r = new Rectangle(x, y, d.width, d.height);
				} else if (cp instanceof Checkbox
						&& ((Checkbox) cp).getLabel().compareTo("") == 0) {
					int off = (colwidth[col] + xpad - cp.getPreferredSize().width) / 2;
					r = new Rectangle(x + off, y, colwidth[col] - off,
							rowheight[row]);
				} else {
					r = new Rectangle(x, y, colwidth[col], rowheight[row]);
				}
				if (!r.equals(cp.getBounds())) {
					cp.setBounds(r);
					cp.invalidate();
				}
			}
			x += colwidth[col] + xpad;
			col = (++col) % columns;
			if (col == 0) {
				y += rowheight[row] + ypad;
				x = xpad / 2;
			}
			t = t.next;
		}
	}

	public void addLayoutComponent(final String name, final Component comp) {
	}

	public void removeLayoutComponent(final Component comp) {
	}

	public void layoutContainer(final Container parent) {
		layout(getFontMetrics(getFont()));
	}

	public Dimension preferredLayoutSize(final Container parent) {
		getMetrics(getFontMetrics(getFont()));
		return new Dimension(maxX + 2, maxY + 2);
	}

	public Dimension minimumLayoutSize(final Container parent) {
		getMetrics(getFontMetrics(getFont()));
		return new Dimension(maxX, maxY);
	}

	public Dimension getPreferredSize() {
		getMetrics(getFontMetrics(getFont()));
		return new Dimension(maxX + 2, maxY + 2);
	}

	synchronized TableElement newTableElement() {
		TableElement t = free;
		if (t != null) {
			free = t.next;
		} else {
			t = new TableElement();
		}
		return t;
	}

	static synchronized void free(final TableElement t) {
		t.next = free;
		free = t;
	}

	static class TableElement {
		TableElement next = null;
		Object value;
		boolean expand;
		boolean movey;
		boolean movex;
		boolean right;
		boolean must_add;
	}

}
