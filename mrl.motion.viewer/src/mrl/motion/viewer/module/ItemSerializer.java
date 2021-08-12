package mrl.motion.viewer.module;

import java.io.File;
import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import mrl.motion.data.MotionData;
import mrl.motion.data.parser.BVHParser;
import mrl.motion.data.parser.BVHWriter;
import mrl.util.FileUtil;
import mrl.util.ObjectSerializer;
import mrl.util.Utils;
import mrl.widget.app.Item;
import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;

public class ItemSerializer extends Module{
	
	public static String BALL_LABEL = "ball_p3d";

	public static void saveToFile(ItemListModule module, String folder) {
		ArrayList<Item> itemList = module.getItemList();
		new File(folder).mkdirs();
		ArrayList<ItemClassDescription> descList = new ArrayList<ItemClassDescription>();
		for (Item item : itemList) {
			if (item.getData() == null) continue;
			String cName = item.getData().getClass().getCanonicalName();
			descList.add(new ItemClassDescription(item));
			
			String fileName = folder + "\\" + item.getLabel() + ".dat";
			if (cName.equals(MotionData.class.getCanonicalName())) {
				BVHWriter bw = new BVHWriter();
				bw.write(new File(fileName), (MotionData)item.getData());
			} else if (item.getLabel().equals(BALL_LABEL)){
				@SuppressWarnings("unchecked")
				TimeBasedList<Point3d> list = (TimeBasedList<Point3d>) item.getData();
				String[] ballTrajectory = new String[list.size()];
				for (int i = 0; i < ballTrajectory.length; i++) {
					Point3d p = list.get(i);
					if (p == null) p = new Point3d(-100000, -100000, -100000);
					ballTrajectory[i] = Utils.toString(p.x, p.y, p.z);
				}
				FileUtil.writeAsString(ballTrajectory, fileName);
			} else {
				FileUtil.writeObject(item.getData(), fileName);
			}
		}
		ObjectSerializer.save(ItemClassDescription.class, descList, new File(folder + "\\desc.txt"));
	}
	
	public static void loadFromFile(ItemListModule module, String folder) {
		System.out.println("load from file :: " + folder);
		ArrayList<ItemClassDescription> descList = ObjectSerializer.load(ItemClassDescription.class, new File(folder + "\\desc.txt"));
		for (ItemClassDescription p : descList) {
			String label = p.label;
			String cName = p.className;
			String fileName = folder + "\\" + label + ".dat";
			System.out.println("load :: " + Utils.toString(label, cName, fileName));
			Object data;
			if (cName.equals(MotionData.class.getCanonicalName())) {
				data = new BVHParser().parse(fileName);
			} else if (label.equals(BALL_LABEL)){
				TimeBasedList<Point3d> ballTrajectory = new TimeBasedList<Point3d>();
				for (double[] pp : FileUtil.readDoubleFromString(fileName)){
					Point3d p3d = new Point3d(pp[0], pp[1], pp[2]);
					ballTrajectory.add(p3d);
				}
				data = ballTrajectory;
			} else {
				data = FileUtil.readObject(fileName);
			}
			ItemDescription desc = null;
			if (!Double.isNaN(p.size)) {
				Vector3d color = null;
				if (!Double.isNaN(p.colorX)) {
					color = new Vector3d(p.colorX, p.colorY, p.colorZ);
				}
				desc = new ItemDescription(color, p.size);
			}
			module.addSingleItem(label, data, desc);
		}
	}
	
	public static class ItemClassDescription{
		public String label;
		public String className;
		public double colorX, colorY, colorZ;
		public double size;
		
		public ItemClassDescription(){
		}
		public ItemClassDescription(Item item) {
			this.label = item.getLabel();
			this.className = item.getData().getClass().getCanonicalName();
			
			colorX = colorY = colorZ = size = Double.NaN;
			
			ItemDescription desc = item.getDescription();
			if (desc != null) {
				Vector3d c = desc.color;
				if (c != null) {
					colorX = c.x;
					colorY = c.y;
					colorZ = c.z;
				}
				size = desc.size;
			}
		}
	}
	
	@Override
	protected void initializeImpl() {
		app().getModule(MainViewerModule.class);
		app().getModule(ItemListModule.class);
	}
	
	public static void main(String[] args) {
		MainApplication.run(new ItemSerializer());
	}
}
