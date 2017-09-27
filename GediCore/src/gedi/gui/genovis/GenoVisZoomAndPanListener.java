/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package gedi.gui.genovis;

import gedi.core.reference.LazyGenome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.GeneralUtils;

import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;

import javax.swing.JViewport;

/**
 * Listener that can be attached to a GenoVisViewer to implement Zoom and Pan functionality.
 *
 */
public class GenoVisZoomAndPanListener implements MouseListener, MouseMotionListener, MouseWheelListener {

	public static final double DEFAULT_ZOOM_MULTIPLICATION_FACTOR = 1.2;


	private double zoomMultiplicationFactor = DEFAULT_ZOOM_MULTIPLICATION_FACTOR;

	private GenoVisViewer viewer;

	private Point dragStartScreen;
	private double dragPixelPerBasepair;

	private boolean[] buttonPressedInViewer = new boolean[100];
	private boolean[] buttonPressedInLeftMargin = new boolean[100];


	public GenoVisZoomAndPanListener(GenoVisViewer viewer) {
		this(viewer,DEFAULT_ZOOM_MULTIPLICATION_FACTOR);
	}

	public GenoVisZoomAndPanListener(GenoVisViewer viewer, double zoomMultiplicationFactor) {
		this.viewer = viewer;
		this.zoomMultiplicationFactor = zoomMultiplicationFactor;
	}



	public void mouseClicked(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		if (e.isConsumed()) return;

		dragStartScreen = e.getPoint();
		if (dragStartScreen.getX()>viewer.getLeftMarginWidth()) {
			dragPixelPerBasepair = viewer.getLocationMapper().getPixelPerBasepair(dragStartScreen.getX()-viewer.getLeftMarginWidth());
			buttonPressedInViewer[e.getButton()] = true;
			if (buttonPressedInViewer[MouseEvent.BUTTON2])
				viewer.repaint();
		} 
		else
			buttonPressedInLeftMargin[e.getButton()] = true;
	}

	public void mouseReleased(MouseEvent e) {
		if (e.isConsumed()) return;

		buttonPressedInViewer[e.getButton()] = false;
		buttonPressedInLeftMargin[e.getButton()] = false;
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
		if (e.isConsumed()) return;

		if (buttonPressedInViewer[MouseEvent.BUTTON1]) {
			double dragStart = dragStartScreen.getX();
			double dragEnd = e.getPoint().getX();
			double dx = dragStart-dragEnd;
			dx = dx/dragPixelPerBasepair;
			
			boolean changed = false;
			if (Math.abs(dx)>=1) {
				if (dx>0) {
					reduceLeft((long)dx);
					extendRight((long)dx);
				}
				else {
					reduceRight(-(long)dx);
					extendLeft(-(long)dx);
				}
				dragStartScreen.x = e.getPoint().x;
				changed = true;
			}

			JViewport scroller = getScroller();
			if (scroller!=null) {
				dragStart = dragStartScreen.getY();
				dragEnd = e.getPoint().getY();
				double dy = dragStart-dragEnd;
				if (Math.abs(dy)>=1) {
					Point vp = scroller.getViewPosition();
					vp.translate(0,(int)dy);
					((SwingGenoVisViewer)viewer).scrollRectToVisible(new Rectangle(vp, scroller.getSize()));
	
				}
				changed = true;
			}
			
			if (changed){
				
				viewer.repaint();
			}

		}
	}

	private JViewport getScroller() {
		Container par = ((SwingGenoVisViewer)viewer).getParent();
		if (par instanceof JViewport)
			return (JViewport)par;
		return null;
	}

	private void extendRight(long right) {

		ReferenceSequence[] refs = viewer.getReference();
		GenomicRegion[] reg = viewer.getRegion();
		LazyGenome genome = viewer.getGenome();

		while (right>0) {
			int lastLength = genome.getLength(refs[refs.length-1].getName());
			int lastEnd = reg[reg.length-1].getEnd();
			if (lastEnd+right>lastLength) {
				// either take the next ref right of that or decrease addRight
				ReferenceSequence after = viewer.getGenome().getReferenceSequenceAfter(refs[reg.length-1]);
				reg[reg.length-1] = reg[reg.length-1].extendBack(lastLength-lastEnd);
				if (after==null) {
					right = 0;
				} else {
					right-=lastLength-lastEnd;
					refs = ArrayUtils.insertItemToArray(refs, refs.length, after);
					int start = 0;
					int end = Math.min(genome.getLength(after.getName()), GeneralUtils.checkedLongToInt(right));
					reg = ArrayUtils.insertItemToArray(reg, reg.length, new ArrayGenomicRegion(start,end));
					right-=end-start;
				}
			} else {
				// fits into current chromosome
				reg[reg.length-1] = reg[reg.length-1].extendBack(GeneralUtils.checkedLongToInt(right));
				right = 0;
			}
		}

		viewer.setLocation(refs,reg);
	}
	private void extendLeft(long left) {
		ReferenceSequence[] refs = viewer.getReference();
		GenomicRegion[] reg = viewer.getRegion();
		LazyGenome genome = viewer.getGenome();

		while (left>0) {
			if (reg[0].getStart()-left<0) {
				// either take the next ref left of that or decrease addLeft
				ReferenceSequence before = genome.getReferenceSequenceBefore(refs[0]);
				left-=reg[0].getStart();
				reg[0] = reg[0].extendFront(reg[0].getStart());
				if (before==null) {
					left = 0;
				} else {
					refs = ArrayUtils.insertItemToArray(refs, 0, before);
					int end = genome.getLength(before.getName());
					int start = Math.max(0,GeneralUtils.checkedLongToInt(end-left));
					reg = ArrayUtils.insertItemToArray(reg, 0, new ArrayGenomicRegion(start,end));
					left-=end-start;
				}
			} else {
				// fits into current chromosome
				reg[0] = reg[0].extendFront(GeneralUtils.checkedLongToInt(left));
				left = 0;
			}
		}

		viewer.setLocation(refs,reg);
	}

	private void reduceLeft(long left) {
		ReferenceSequence[] refs = viewer.getReference();
		GenomicRegion[] reg = viewer.getRegion();

		// truncate start
		long pos = 0;
		for (int i=0; i<refs.length; i++) {
			if (pos+reg[i].getTotalLength()>left) {
				refs = Arrays.copyOfRange(refs, i, refs.length);
				reg = Arrays.copyOfRange(reg, i, reg.length);
				reg[0] = reg[0].map(new ArrayGenomicRegion(GeneralUtils.checkedLongToInt(left-pos),reg[0].getTotalLength()));
				break;
			}
			pos+=reg[i].getTotalLength();
		}
		viewer.setLocation(refs,reg);
	}

	private void reduceRight(long right) {
		// truncate end
		ReferenceSequence[] refs = viewer.getReference();
		GenomicRegion[] reg = viewer.getRegion();
		long total = 0;
		for (int i=0; i<reg.length; i++)
			total += reg[i].getTotalLength();
		long newTotal = total-right;

		long pos = 0;
		for (int i=0; i<refs.length; i++) {
			if (pos+reg[i].getTotalLength()>newTotal) {
				refs = Arrays.copyOfRange(refs, 0, i+1);
				reg = Arrays.copyOfRange(reg, 0, i+1);
				reg[i] = reg[i].map(new ArrayGenomicRegion(0,GeneralUtils.checkedLongToInt(newTotal-pos)));
				break;
			}
			pos+=reg[i].getTotalLength();
		}
		viewer.setLocation(refs,reg);
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		if (e.isConsumed()) return;

		int wheelRotation = e.getWheelRotation();

		double f = (e.getPoint().getX()-viewer.getLeftMarginWidth())/(viewer.getScreenWidth()-viewer.getLeftMarginWidth());

		long total = 0;
		for (int i=0; i<viewer.getRegion().length; i++)
			total += viewer.getRegion()[i].getTotalLength();


		if (wheelRotation<0) {

			long newTotal = (long) (total/zoomMultiplicationFactor);
			if (newTotal==0) return;

			int newStart = (int) (f*(total-newTotal));


			long removeLeft = newStart;
			long removeRight = total-newStart-newTotal;

			reduceLeft(removeLeft);
			reduceRight(removeRight);


		}
		if (wheelRotation>0) {
			long addToTotal = (long) (total*(zoomMultiplicationFactor-1));
			long addLeft = (long) ((f)*addToTotal);
			long addRight = (long) ((1-f)*addToTotal);

			if (addLeft==0 && addRight==0) {
				if (f>0.5)
					addLeft++;
				else
					addRight++;
			}

			extendLeft(addLeft);
			extendRight(addRight);

		}

		//		// check full length for inner 
		//		for (int i=1; i<viewer.getRegion().length-1; i++) {
		//			if (viewer.getRegion()[i].getStart()!=0 || viewer.getRegion()[i].getEnd()!=viewer.getGenome().getLength(viewer.getReference()[i].getName()))
		//				System.out.println(viewer.getRegion()[i]+" "+viewer.getGenome().getLength(viewer.getReference()[i].getName()));
		//		}

		viewer.repaint();
	}




}

