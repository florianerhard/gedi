package gedi.util.gui;

import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

/**
 * Use this if you don't want your {@link AffineTransform} to resize the line width!
 * @author flo
 *
 */
public class TransformedStroke implements Stroke
{
	private AffineTransform transform;
	private AffineTransform inverse;
	private Stroke stroke;


	public TransformedStroke(Stroke base, AffineTransform at)
	{
		this.transform = new AffineTransform(at);
		try {
			this.inverse = transform.createInverse();
		} catch (NoninvertibleTransformException e) {
			throw new RuntimeException(e);
		}
		this.stroke = base;
	}


	public Shape createStrokedShape(Shape s) {
		Shape sTrans = transform.createTransformedShape(s);
		Shape sTransStroked = stroke.createStrokedShape(sTrans);
		Shape sStroked = inverse.createTransformedShape(sTransStroked);
		return sStroked;
	}

}

