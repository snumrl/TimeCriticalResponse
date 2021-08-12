package mrl.motion.position;

import java.util.ArrayList;

import javax.vecmath.Point3d;

import mrl.motion.data.Contact;

public class PositionResultMotion extends ArrayList<PositionResultMotion.PositionFrame>{

	private static final long serialVersionUID = -7189418500072866426L;

	
	public static class PositionFrame extends ArrayList<Point3d[]>{

		private static final long serialVersionUID = 8805170119114288843L;
		
		public Contact footContact = new Contact(false, false);
		public Contact ballContact = new Contact(false, false);
	}
}
