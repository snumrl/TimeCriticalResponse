package mrl.widget.dockable;

import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Rectangle;

/**
 * 주어진 영역으로 Track Bounds만 설정해주고 실제 drop 동작은 하지 않는 DropTarget
 * 
 * @author whcjs
 */
public class BlankDropTarget implements IDropTarget{
	
	private Rectangle snapRectangle;
	
	public BlankDropTarget(Rectangle snapRectangle) {
		this.snapRectangle = snapRectangle;
	}

	public void drop() {
	}

	public Cursor getCursor() {
		return null;
	}

	public Rectangle getSnapRectangle() {
		return snapRectangle;
	}

}
