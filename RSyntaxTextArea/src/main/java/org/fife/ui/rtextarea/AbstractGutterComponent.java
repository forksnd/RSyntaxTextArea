/*
 * 02/17/2009
 *
 * AbstractGutterComponent.java - A component that can be displayed in a Gutter.
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rtextarea;

import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.View;


/**
 * A component that can be displayed in a {@link Gutter}.
 *
 * @author Robert Futrell
 * @version 1.0
 */
abstract class AbstractGutterComponent extends JPanel {

	/**
	 * The text area whose lines we are marking with icons.
	 */
	protected RTextArea textArea;

	/**
	 * The number of lines in the text area.
	 */
	protected int currentLineCount;

	private static Listener listener;


	/**
	 * Constructor.
	 *
	 * @param textArea The text area.
	 */
	AbstractGutterComponent(RTextArea textArea) {
		init(); // Called before setTextArea().
		setTextArea(textArea);
	}


	@Override
	public void addNotify() {
		super.addNotify();
		getListener().install(this);
	}


	/**
	 * Returns the bounds of a child view as a rectangle, since
	 * <code>View</code>s tend to use <code>Shape</code>.
	 *
	 * @param parent The parent view of the child whose bounds we're getting.
	 * @param line The index of the child view.
	 * @param editorRect Returned from the text area's
	 *        <code>getVisibleEditorRect</code> method.
	 * @return The child view's bounds.
	 */
	protected static Rectangle getChildViewBounds(View parent, int line,
										Rectangle editorRect) {
		Shape alloc = parent.getChildAllocation(line, editorRect);
		if (alloc==null) {
			// WrappedSyntaxView can have this when made so small it's
			// no longer visible
			return new Rectangle();
		}
		return alloc instanceof Rectangle ? (Rectangle)alloc :
										alloc.getBounds();
	}


	/**
	 * Returns the parent <code>Gutter</code> component.
	 *
	 * @return The parent <code>Gutter</code>.
	 */
	protected Gutter getGutter() {
		Container parent = getParent();
		return (parent instanceof Gutter) ? (Gutter)parent : null;
	}


	/**
	 * Returns the singleton instance of the listener for all gutter components.
	 *
	 * @return The singleton instance.
	 */
	private static Listener getListener() {
		if (listener == null) {
			listener = new Listener();
		}
		return listener;
	}


	/**
	 * Called when text is inserted to or removed from the text area.
	 * Implementations can take this opportunity to repaint, revalidate, etc.
	 *
	 * @param e The document event.
	 * @see #handleDocumentUpdated(RDocument, RDocument)
	 */
	abstract void handleDocumentEvent(DocumentEvent e);


	/**
	 * Called when the document is updated. This happens when an application
	 * calls {@code textArea.read(reader)}, for example.<p>
	 *
	 * The default implementation does nothing. Subclasses can override.
	 *
	 * @param oldDoc The old document, which may be {@code null}.
	 * @param newDoc The new document, which may be {@code null}.
	 * @see #handleDocumentEvent(DocumentEvent)
	 */
	void handleDocumentUpdated(RDocument oldDoc, RDocument newDoc) {
		// Do nothing
	}


	/**
	 * Called by the constructor before the text area is set.  This is a hook
	 * to allow subclasses to do any needed initialization.  The default
	 * implementation does nothing.
	 */
	protected void init() {
	}


	/**
	 * Called when the line heights of the text area change.  This is usually
	 * the result of one or more of the fonts in the editor changing.
	 */
	abstract void lineHeightsChanged();


	@Override
	public void removeNotify() {
		getListener().uninstall(this);
		super.removeNotify();
	}


	/**
	 * Sets the text area being displayed.  Subclasses can override, but
	 * should call the super implementation.
	 *
	 * @param textArea The text area.
	 */
	public void setTextArea(RTextArea textArea) {
		this.textArea = textArea;
		int lineCount = textArea==null ? 0 : textArea.getLineCount();
		if (currentLineCount!=lineCount) {
			currentLineCount = lineCount;
			repaint();
		}
	}


	static class Listener extends MouseAdapter {

		private boolean newArmed;

		void install(AbstractGutterComponent component) {
			component.addMouseListener(this);
			component.addMouseMotionListener(this);
		}

		public void mouseExited(MouseEvent e) {
			setArmed((AbstractGutterComponent)e.getComponent(), false);
		}

		public void mouseMoved(MouseEvent e) {
			setArmed((AbstractGutterComponent)e.getComponent(), true);
		}

		private void setArmed(AbstractGutterComponent component, boolean armed) {
			newArmed = armed;
			SwingUtilities.invokeLater(() -> {
				if (component.getGutter() != null && newArmed != component.getGutter().isArmed()) {
					component.getGutter().setArmed(newArmed);
				}
			});
		}

		void uninstall(AbstractGutterComponent component) {
			component.removeMouseListener(this);
			component.removeMouseMotionListener(this);
		}
	}
}
