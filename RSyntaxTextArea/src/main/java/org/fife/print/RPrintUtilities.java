/*
 * 11/14/2003
 *
 * RPrintUtilities.java - A collection of static methods useful for printing
 * text from Swing text components.
 *
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.print;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.print.PageFormat;
import java.awt.print.Printable;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Segment;
import javax.swing.text.TabExpander;
import javax.swing.text.Utilities;



/**
 * A collection of static methods useful for printing text from Swing text components.
 *
 * @author Robert Futrell
 * @version 1.0
 */
public abstract class RPrintUtilities {

	private static int currentDocLineNumber;		// The line number in the document we are currently on.
	private static int numDocLines;				// The number of lines in the current document.
	private static Element rootElement;			// The first Element (line) in the current document.

	// The characters at which to break a line if implementing word wrap.
	private static final char [] BREAK_CHARS = { ' ', '\t', ',', '.', ';', '?', '!' };

	// These variables are 'global' because RPrintTabExpander uses them.

	/**
	 * The x-offset (for the page margin) when printing.
	 */
	private static int xOffset;

	/**
	 * The length of a tab, in spaces.
	 */
	private static int tabSizeInSpaces;

	/**
	 * The metrics of the font currently being used to print.
	 */
	private static FontMetrics fm;


	/**
	 * Returns the position closest to, but before, position <code>maxCharsPerLine</code> in
	 * <code>line</code> of one of the chars in <code>breakChars</code>, or simply returns
	 * <code>maxCharsPerLine-1</code> if none of the <code>breakChars</code> comes before
	 * that position.  This position represents the logical line break for this <code>java.lang.String</code>
	 * if it is being printed in a monospaced font when lines can only be <code>maxCharsPerLine</code>
	 * characters long.
	 *
	 * @param line The text being printed.
	 * @param maxCharsPerLine Only up-to this many characters from
	 *        <code>line</code> can be printed on one line.
	 * @return The logical position at which to stop printing <code>line</code>
	 *         to simulate word wrap.
	 */
	private static int getLineBreakPoint(String line, final int maxCharsPerLine) {

		int breakPoint = -1;
		for (char breakChar : BREAK_CHARS) {
			int breakCharPos = line.lastIndexOf(breakChar, maxCharsPerLine - 1);
			if (breakCharPos > breakPoint) {
				breakPoint = breakCharPos;
			}
		}

		return breakPoint==-1 ? maxCharsPerLine-1 : breakPoint;

	}


	/**
	 * Prints a <code>Document</code> using a monospaced font, and does no word wrapping (ie,
	 * words will wrap mid-word to the next line).  This method is expected to be called from
	 * Printable 'print(Graphics g)' functions.
	 *
	 * @param g The graphics context to write to.
	 * @param doc The <code>javax.swing.text.Document</code> to print.
	 * @param fontSize the point size to use for the monospaced font.
	 * @param pageIndex The page number to print.
	 * @param pageFormat The format to print the page with.
	 * @param tabSize The number of spaces to expand tabs to.
	 * @return One of the constants from {@code Printable}.
	 * @see #printDocumentMonospacedWordWrap
	 */
	public static int printDocumentMonospaced(Graphics g, Document doc, int fontSize, int pageIndex,
							PageFormat pageFormat, int tabSize) {

		g.setColor(Color.BLACK);
		g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));

		// Initialize our static variables (these are used by our tab expander below).
		tabSizeInSpaces = tabSize;
		fm = g.getFontMetrics();

		// Create our tab expander.
		//RPrintTabExpander tabExpander = new RPrintTabExpander();

		// Get width and height of characters in this monospaced font.
		int fontWidth = fm.charWidth('w');	// Any character will do as font is monospaced.
		int fontHeight = fm.getHeight();

		int maxCharsPerLine = (int)pageFormat.getImageableWidth() / fontWidth;
		int maxLinesPerPage = (int)pageFormat.getImageableHeight() / fontHeight;

		final int startingLineNumber = maxLinesPerPage * pageIndex;

		// The (x,y) coordinate to print at (in pixels, not characters).
		// Since y is the baseline of where we'll start printing (not the top-left
		// corner), we offset it by the font's ascent ( + 1 just for good measure).
		xOffset = (int)pageFormat.getImageableX();
		int y = (int)pageFormat.getImageableY() + fm.getAscent() + 1;

		// A counter to keep track of the number of lines that WOULD HAVE been
		// printed if we were printing all lines.
		int numPrintedLines = 0;

		// Keep going while there are more lines in the document.
		currentDocLineNumber = 0;						// Line number of the document we're currently on
		rootElement = doc.getDefaultRootElement();		// To shorten accesses in our loop.
		numDocLines = rootElement.getElementCount();	// The number of lines in our document.
		while (currentDocLineNumber<numDocLines) {

			// Get the line we are going to print.
			String curLineString;
			Element currentLine = rootElement.getElement(currentDocLineNumber);
			int startOffs = currentLine.getStartOffset();
			try {
				curLineString = doc.getText(startOffs, currentLine.getEndOffset()-startOffs);
			} catch (BadLocationException ble) { // Never happens
				ble.printStackTrace();
				return Printable.NO_SUCH_PAGE;
			}

			// Get rid of newlines, because they end up as boxes if you don't; this is a monospaced font.
			curLineString = curLineString.replaceAll("\n", "");

			// Replace tabs with how many spaces they should be.
			if (tabSizeInSpaces == 0) {
				curLineString = curLineString.replaceAll("\t", "");
			}
			else {
				int tabIndex = curLineString.indexOf('\t');
				while (tabIndex > -1) {
					int spacesNeeded = tabSizeInSpaces - (tabIndex % tabSizeInSpaces);
                    StringBuilder stringBuilder = new StringBuilder();
					for (int i=0; i<spacesNeeded; i++) {
						stringBuilder.append(" ");
					}
					// Note that "\t" is actually a regex for this method.
					curLineString = curLineString.replaceFirst("\t", stringBuilder.toString());
					tabIndex = curLineString.indexOf('\t');
				}
			}

			// If this document line is too long to fit on one printed line on the page,
			// break it up into multiple lines.
			while (curLineString.length() > maxCharsPerLine) {

				numPrintedLines++;
				if (numPrintedLines > startingLineNumber) {
						g.drawString(curLineString.substring(0,maxCharsPerLine), xOffset,y);
						y += fontHeight;
						if (numPrintedLines==startingLineNumber+maxLinesPerPage) {
							return Printable.PAGE_EXISTS;
						}
	 			}

				curLineString = curLineString.substring(maxCharsPerLine);

			}

			currentDocLineNumber += 1; // We have printed one more line from the document.

			numPrintedLines++;
			if (numPrintedLines>startingLineNumber) {
				g.drawString(curLineString, xOffset,y);
				y += fontHeight;
				if (numPrintedLines==startingLineNumber+maxLinesPerPage) {
					return Printable.PAGE_EXISTS;
				}
			}


		}

		// Now, the whole document has been "printed."  Decide if this page had any text on it or not.
		if (numPrintedLines > startingLineNumber) {
			return Printable.PAGE_EXISTS;
		}
		return Printable.NO_SUCH_PAGE;

	}


	/**
	 * Prints a <code>Document</code> using a monospaced font, word wrapping on
	 * the characters ' ', '\t', '\n', ',', '.', and ';'.  This method is
	 * expected to be called from Printable 'print(Graphics g)' functions.
	 *
	 * @param g The graphics context to write to.
	 * @param doc The <code>javax.swing.text.Document</code> to print.
	 * @param fontSize the point size to use for the monospaced font.
	 * @param pageIndex The page number to print.
	 * @param pageFormat The format to print the page with.
	 * @param tabSize The number of spaces to expand tabs to.
	 * @return One of the constants from {@code Printable}.
	 * @see #printDocumentMonospaced
	 */
	public static int printDocumentMonospacedWordWrap(Graphics g, Document doc,
								int fontSize, int pageIndex,
								PageFormat pageFormat, int tabSize) {

		g.setColor(Color.BLACK);
		g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));

		// Initialize our static variables (these are used by our tab expander below).
		tabSizeInSpaces = tabSize;
		fm = g.getFontMetrics();

		// Create our tab expander.
		//RPrintTabExpander tabExpander = new RPrintTabExpander();

		// Get width and height of characters in this monospaced font.
		int fontWidth = fm.charWidth('w'); 	// Any character will do here, since font is monospaced.
		int fontHeight = fm.getHeight();

		int maxCharsPerLine = (int)pageFormat.getImageableWidth() / fontWidth;
		int maxLinesPerPage = (int)pageFormat.getImageableHeight() / fontHeight;

		final int startingLineNumber = maxLinesPerPage * pageIndex;

		// The (x,y) coordinate to print at (in pixels, not characters).
		// Since y is the baseline of where we'll start printing (not the top-left
		// corner), we offset it by the font's ascent ( + 1 just for good measure).
		xOffset = (int)pageFormat.getImageableX();
		int y = (int)pageFormat.getImageableY() + fm.getAscent() + 1;

		// A counter to keep track of the number of lines that WOULD HAVE been
		// printed if we were printing all lines.
		int numPrintedLines = 0;

		// Keep going while there are more lines in the document.
		currentDocLineNumber = 0;						// Line number of the document we're currently on
		rootElement = doc.getDefaultRootElement();		// To shorten accesses in our loop.
		numDocLines = rootElement.getElementCount();	// The number of lines in our document.
		while (currentDocLineNumber<numDocLines) {

			// Get the line we are going to print.
			String curLineString;
			Element currentLine = rootElement.getElement(currentDocLineNumber);
			int startOffs = currentLine.getStartOffset();
			try {
				curLineString = doc.getText(startOffs, currentLine.getEndOffset()-startOffs);
			} catch (BadLocationException ble) { // Never happens
				ble.printStackTrace();
				return Printable.NO_SUCH_PAGE;
			}

			// Remove newlines, because they end up as boxes if you don't; this is a monospaced font.
			curLineString = curLineString.replaceAll("\n", "");

			// Replace tabs with how many spaces they should be.
			if (tabSizeInSpaces == 0) {
				curLineString = curLineString.replaceAll("\t", "");
			}
			else {
				int tabIndex = curLineString.indexOf('\t');
				while (tabIndex > -1) {
					int spacesNeeded = tabSizeInSpaces - (tabIndex % tabSizeInSpaces);
                    StringBuilder stringBuilder = new StringBuilder();
					for (int i=0; i<spacesNeeded; i++) {
						stringBuilder.append(" ");
					}
					// Note that "\t" is actually a regex for this method.
					curLineString = curLineString.replaceFirst("\t", stringBuilder.toString());
					tabIndex = curLineString.indexOf('\t');
				}
			}

			// If this document line is too long to fit on one printed line on the page,
			// break it up into multiple lines.
			while (curLineString.length() > maxCharsPerLine) {

				int breakPoint = getLineBreakPoint(curLineString, maxCharsPerLine) + 1;

				numPrintedLines++;
				if (numPrintedLines > startingLineNumber) {
						g.drawString(curLineString.substring(0,breakPoint), xOffset,y);
						y += fontHeight;
						if (numPrintedLines==startingLineNumber+maxLinesPerPage) {
							return Printable.PAGE_EXISTS;
						}
	 			}

				curLineString = curLineString.substring(breakPoint);

			}

			currentDocLineNumber += 1; // We have printed one more line from the document.

			numPrintedLines++;
			if (numPrintedLines>startingLineNumber) {
				g.drawString(curLineString, xOffset,y);
				y += fontHeight;
				if (numPrintedLines==startingLineNumber+maxLinesPerPage) {
					return Printable.PAGE_EXISTS;
				}
			}


		}

		// Now, the whole document has been "printed."  Decide if this page had any text on it or not.
		if (numPrintedLines > startingLineNumber) {
			return Printable.PAGE_EXISTS;
		}
		return Printable.NO_SUCH_PAGE;

	}


	/**
	 * Prints a <code>Document</code> using the specified font, word wrapping
	 * on the characters ' ', '\t', '\n', ',', '.', and ';'.  This method is
	 * expected to be called from Printable 'print(Graphics g)' functions.
	 *
	 * @param g The graphics context to write to.
	 * @param textComponent The <code>javax.swing.text.JTextComponent</code>
	 *        whose text you're printing.
	 * @param font The font to use for printing.  If <code>null</code>, then
	 *        <code>textComponent</code>'s font is used.
	 * @param pageIndex The page number to print.
	 * @param pageFormat The format to print the page with.
	 * @param tabSize The number of spaces to convert tabs to.
	 * @return One of the constants from {@code Printable}.
	 */
	public static int printDocumentWordWrap(Graphics g, JTextComponent textComponent,
										Font font, int pageIndex,
										PageFormat pageFormat,
										int tabSize) {

		// Initialize our graphics object.
		g.setColor(Color.BLACK);
		g.setFont(font!=null ? font : textComponent.getFont());

		// Initialize our static variables (these are used by our tab expander below).
		tabSizeInSpaces = tabSize;
		fm = g.getFontMetrics();
		int fontHeight = fm.getHeight();

		final int lineLengthInPixels = (int)pageFormat.getImageableWidth();
		final int maxLinesPerPage = (int)pageFormat.getImageableHeight() / fontHeight;

		final int startingLineNumber = maxLinesPerPage * pageIndex;

		// Create our tab expander.
		RPrintTabExpander tabExpander = new RPrintTabExpander();

		// The (x,y) coordinate to print at (in pixels, not characters).
		// Since y is the baseline of where we'll start printing (not the top-left
		// corner), we offset it by the font's ascent ( + 1 just for good measure).
		xOffset = (int)pageFormat.getImageableX();
		int y = (int)pageFormat.getImageableY() + fm.getAscent() + 1;

		// A counter to keep track of the number of lines that WOULD HAVE been
		// printed if we were printing all lines.
		int numPrintedLines = 0;

		// Keep going while there are more lines in the document.
		Document doc = textComponent.getDocument();
		rootElement = doc.getDefaultRootElement();
		numDocLines = rootElement.getElementCount();		// The number of lines in our document.
		currentDocLineNumber = 0;					// The line number of the document we're currently on.
		int startingOffset = 0;						// Used when a line is so long it has to be wrapped.
		while (currentDocLineNumber<numDocLines) {

			Segment currentLineSeg = new Segment();

			// Get the current line (as an Element), and its starting and ending offset in doc.
			Element currentLine  = rootElement.getElement(currentDocLineNumber);
			int currentLineStart = currentLine.getStartOffset();
			int currentLineEnd   = currentLine.getEndOffset();

			// Put the chars of this line in currentLineSeg, but only starting at our desired offset
			// (because this line may be the second part of a wrapped line, so we'd start after the part
			// that has already been printed).
			try {
				doc.getText(currentLineStart+startingOffset, currentLineEnd-(currentLineStart+startingOffset),
							currentLineSeg);
			} catch (BadLocationException ble) {
				ble.printStackTrace();
				return Printable.NO_SUCH_PAGE;
			}

			// Remove any spaces and/or tabs from the end of the segment (would cause problems if you left 'em).
			currentLineSeg = removeEndingWhitespace(currentLineSeg);

			// Figure out how long the line is, in pixels.
			int currentLineLengthInPixels = Utilities.getTabbedTextWidth(currentLineSeg, fm, 0, tabExpander, 0);

			//System.err.println("'" + currentLineSeg + "' - " + currentLineLengthInPixels + "/" +
			// LINE_LENGTH_IN_PIXELS);
			// If it'll fit by itself on a printed line, great.
			if (currentLineLengthInPixels <= lineLengthInPixels) {
				currentDocLineNumber += 1; 	// We (will) have printed one more line from the document.
				startingOffset = 0;			// Start at the first character in the new document line.
			}

			// If it doesn't fit on a printed line by itself (i.e., it needs to be wrapped)...
			else {

				// Loop while the current line is too long to fit on a printed line.
				int currentPos = -1;
				while (currentLineLengthInPixels > lineLengthInPixels) {

					//System.err.println("'" + currentLineSeg + "' - " + currentLineLengthInPixels + "/" +
					// LINE_LENGTH_IN_PIXELS);

					// Remove any spaces and/or tabs from the end of the segment (would cause problems if you left 'em).
					currentLineSeg = removeEndingWhitespace(currentLineSeg);

					// currentPos will be the last position in the current text of a "line break character."
					currentPos = -1;
					String currentLineString = currentLineSeg.toString();
					for (char breakChar : BREAK_CHARS) {
						// "+1" below so we include the character on the line.
						int pos = currentLineString.lastIndexOf(breakChar) + 1;
						//if (pos>-1 && pos>currentPos)
						//	currentPos = pos;
						if (pos > 0 && pos > currentPos && pos != currentLineString.length()) {
							currentPos = pos;
						}
					}

					// If we didn't find a line break character, we'll simply break the line at
					// the last character that fits on a printed line.
					// So here, we set currentPos to be the position of the last character that fits
					// on the current printed line.
					if (currentPos == -1) {

						// Fix currentLineSeg so that it contains exactly enough text to fit in
						// LINE_LENGTH_IN_PIXELS pixels...
						currentPos = 0;
						do {
							currentPos++;
							try {
								doc.getText(currentLineStart+startingOffset, currentPos, currentLineSeg);
							} catch (BadLocationException ble) {
								ble.printStackTrace();
								return Printable.NO_SUCH_PAGE;
							}
							currentLineLengthInPixels = Utilities.
								getTabbedTextWidth(currentLineSeg, fm, 0, tabExpander, 0);
						} while (currentLineLengthInPixels <= lineLengthInPixels);
						currentPos--;

					}

					try {
						doc.getText((currentLineStart+startingOffset), currentPos, currentLineSeg);
					} catch (BadLocationException ble) {
						ble.printStackTrace();
						return Printable.NO_SUCH_PAGE;
					}

					currentLineLengthInPixels = Utilities.getTabbedTextWidth(currentLineSeg, fm, 0, tabExpander, 0);
				} // End of while (currentLineLengthInPixels > LINE_LENGTH_IN_PIXELS).

				startingOffset += currentPos;	// Where to start (offset from line's start), since this line wraps.

			} // End of else.

			numPrintedLines++;
			if (numPrintedLines>startingLineNumber) {
				//g.drawString(currentLineSeg.toString(), xOffset,y);
				Utilities.drawTabbedText(currentLineSeg, xOffset,y, g, tabExpander, 0);
				y += fontHeight;
				if (numPrintedLines==startingLineNumber+maxLinesPerPage) {
					return Printable.PAGE_EXISTS;
				}
			}

		}

		// Now, the whole document has been "printed."  Decide if this page had any text on it or not.
		if (numPrintedLines > startingLineNumber) {
			return Printable.PAGE_EXISTS;
		}
		return Printable.NO_SUCH_PAGE;

	}


	/**
	 * Removes any spaces or tabs from the end of the segment.
	 *
	 * @param segment The segment from which to remove tailing whitespace.
	 * @return <code>segment</code> with trailing whitespace removed.
	 */
	private static Segment removeEndingWhitespace(Segment segment) {
		int toTrim = 0;
		char currentChar = segment.setIndex(segment.getEndIndex()-1);
		while ((currentChar==' ' || currentChar=='\t') && currentChar!=Segment.DONE) {
			toTrim++;
			currentChar = segment.previous();
		}
		String stringVal = segment.toString();
		String newStringVal = stringVal.substring(0,stringVal.length()-toTrim);
		return new Segment(newStringVal.toCharArray(), 0, newStringVal.length());
	}


	/**
	 * A tab expander for the document currently being printed with the
	 * font being used for the printing.
	 */
	private static final class RPrintTabExpander implements TabExpander {

		@Override
		public float nextTabStop(float x, int tabOffset) {
			if (tabSizeInSpaces == 0) {
				return x;
			}
			int tabSizeInPixels = tabSizeInSpaces * fm.charWidth(' ');
			int tabCount = (((int) x) - xOffset) / tabSizeInPixels;
			return xOffset + ((tabCount + 1f) * tabSizeInPixels);
		}

	}


}
