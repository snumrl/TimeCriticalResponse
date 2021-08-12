package mrl.motion.neural.agility.measure;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

import mrl.widget.app.MainApplication;
import mrl.widget.app.MainApplication.WindowPosition;
import mrl.widget.app.Module;

public class DistributionViewerModule extends Module{

	@Override
	protected void initializeImpl() {
		double[][] agility_15 = {
				{ 13.363363363363364, 0.4809682209134276, 13.0, 14.0,},
				{ 13.195195195195195, 0.396350893739253, 13.0, 14.0,},
				{ 13.048048048048049, 0.2859641046754922, 12.0, 14.0,},
				{ 13.033033033033034, 0.17872283503142905, 13.0, 14.0,},
			};
			double[][] agility_25 = {
				{ 21.45045045045045, 1.408289948084262, 18.0, 23.0,},
				{ 21.783783783783782, 1.4040187199119403, 18.0, 25.0,},
				{ 22.48948948948949, 0.4998895169623065, 22.0, 23.0,},
				{ 22.38138138138138, 0.6943888460821193, 18.0, 23.0,},
			};
			double[][] agility_35 = {
				{ 31.86786786786787, 1.7278727102021239, 26.0, 34.0,},
				{ 29.54954954954955, 4.074542820549782, 18.0, 34.0,},
				{ 32.43543543543544, 0.5252251393760738, 31.0, 33.0,},
				{ 30.942942942942942, 2.544449246834656, 19.0, 33.0,},
			};
			double[][] agility_45 = {
//					{ 41.207207207207205, 4.608616653136122, 28.0, 60.0,},
				{ 41.207207207207205, 4.608616653136122, 28.0, 44.0,},
				{ 36.663636363636364, 6.251221368264063, 20.0, 44.0,},
				{ 39.78012048192771, 3.145540348154777, 32.0, 43.0,},
				{ 39.84939759036145, 6.444029149941471, 19.0, 44.0,},
			};
			double[][] agility_55 = {
				{ 51.693693693693696, 2.8531404944291157, 34.0, 55.0,},
				{ 48.153614457831324, 6.088495388215463, 31.0, 55.0,},
				{ 44.05722891566265, 5.906376136691562, 33.0, 53.0,},
				{ 50.43843843843844, 3.2047206115607256, 32.0, 55.0,},
			};
		String[] labels = {
			"1",
			"2",
			"3",
			"4",
//			"punch",
//			"jump backspin",
//			"side flip",
			
		};
		addViewer(labels, agility_15, 15);
		addViewer(labels, agility_25, 25);
		addViewer(labels, agility_35, 35);
		addViewer(labels, agility_45, 45);
		addViewer(labels, agility_55, 55);
		
		addMenu("&Menu", "&Test\tCtrl+C", SWT.MOD1 + 'C', new Runnable() {
			@Override
			public void run() {
				CTabItem item = app().getMainContainer().findShowingItem();
				DistributionlViewer viewer = (DistributionlViewer) item.getControl();
				Display display = dummyParent().getDisplay();
				Canvas shell = viewer.getCanvas();
				Rectangle sBounds = viewer.getShell().getBounds();
                final Image image = new Image(display, shell.getBounds());
                GC gc = new GC(image);
                viewer.paintControl(gc);
//				GC gc = new GC(display);
//                gc.copyArea(image, shell.getBounds().x, shell.getBounds().y);
                gc.dispose();
                
                final Clipboard clipboard = new Clipboard(display);
//                Insets insets = source.getInsets();
//                int w = source.getWidth() - insets.left - insets.right;
//                int h = source.getHeight() - insets.top - insets.bottom;
//                ChartTransferable selection = new ChartTransferable(source
//                        .getChart(), w, h, source.getMinimumDrawWidth(), source.getMinimumDrawHeight(), source
//                        .getMaximumDrawWidth(), source.getMaximumDrawHeight(), true);
//
//                Image image = new Image(menu.getDisplay(),ImageUtils.convertToSWT(selection.getBufferedImage()));
//                if (image != null) {
//                    ImageLoader imageLoader = new ImageLoader(); 
//                    imageLoader.data = new ImageData[] { image.getImageData() }; 
//                    imageLoader.save("/tmp/graph.jpg", SWT.IMAGE_JPEG); // fails 
//                    ImageTransfer imageTransfer = ImageTransfer.getInstance();
//                    clipboard.setContents(new Object[]{image.getImageData()}, 
//                            new Transfer[]{imageTransfer}, DND.CLIPBOARD | DND.SELECTION_CLIPBOARD);
//                }
                
                ImageTransfer imageTransfer = ImageTransfer.getInstance();
                clipboard.setContents(new Object[] {image.getImageData()}, new Transfer[]{imageTransfer});
			} 
		});
	}
	
	private DistributionlViewer addViewer(String[] labels, double[][] dataList, double limit) {
		DistributionlViewer viewer = app().addWindow(new DistributionlViewer(dummyParent()), "dViewer_" + limit, WindowPosition.Main);
		viewer.setDistribution(labels, dataList);
		viewer.maxValue = 60;
		viewer.limitValue = limit;
		viewer.showYAxis = limit == 55d;
		return viewer;
	}

	public static void main(String[] args) {
		MainApplication.run(new DistributionViewerModule());
	}
	
}
