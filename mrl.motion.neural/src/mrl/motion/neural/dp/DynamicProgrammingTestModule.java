package mrl.motion.neural.dp;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import mrl.motion.data.MDatabase;
import mrl.motion.data.Motion;
import mrl.motion.data.MotionAnnotation;
import mrl.motion.data.MotionAnnotation.MotionAnnValueGetter;
import mrl.motion.data.MotionData;
import mrl.motion.dp.DynamicProgramming;
import mrl.motion.viewer.module.MainViewerModule;
import mrl.util.Configuration;
import mrl.util.Utils;
import mrl.widget.ValueSlider;
import mrl.widget.app.ItemListModule;
import mrl.widget.app.ListViewerModule;
import mrl.widget.app.ListViewerModule.ListListener;
import mrl.widget.app.MainApplication;
import mrl.widget.app.Module;
import mrl.widget.app.SliderModule;

public class DynamicProgrammingTestModule extends Module{
	
	private DynamicProgramming dp;
	private ValueSlider slider;
	

	@Override
	protected void initializeImpl() {
		String dataFolder = "danceCard\\dc_action";
		Configuration.setDataFolder(dataFolder);
		MDatabase database = MDatabase.load();
		dp = new DynamicProgramming(database);
		dp.load("..\\mrl.motion.data\\dpCache.dat");
		
		getModule(MainViewerModule.class);
		ListViewerModule listMoudle = getModule(ListViewerModule.class);
		
		ArrayList<MotionAnnotation> annList = new ArrayList<MotionAnnotation>();
		for (MotionAnnotation ann : database.getTransitionAnnotations()) {
			if (ann.interactionFrame > 0) {
				annList.add(ann);
			}
		}
		listMoudle.setItems(annList, new ListListener<MotionAnnotation>(){
			@Override
			public String[] getColumnHeaders() {
				return new String[] { "file", "interactionFrame", "type"};	
			}

			@Override
			public String[] getTableValues(MotionAnnotation item) {
				return Utils.toStringArrays(item.file, item.interactionFrame, item.type);
			}
			@Override
			public void onItemSelection(MotionAnnotation item) {
				System.out.println("selected :: " + item);
				int mIndex = database.findMotion(item.file, item.interactionFrame).motionIndex;
				updateSelection(mIndex, slider.getSelection());
			}
		});
		
		SliderModule sModule = getModule(SliderModule.class);
		sModule.setDefaultRange(10, 60);
		sModule.setSliderValues(new int[] { 30 });
		slider = sModule.getSliderList().get(0);
		slider.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
			}
		});
	}
	
	private void updateSelection(int motionIndex, int time) {
		ArrayList<Motion> trace = dp.backtrace(motionIndex, time);
		getModule(ItemListModule.class).addSingleItem("motion", new MotionData(trace));
		getModule(MainViewerModule.class).replay();
	}
	
	public static void main(String[] args) {
		MainApplication.run(new DynamicProgrammingTestModule());
	}

}
