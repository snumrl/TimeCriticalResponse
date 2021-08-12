package mrl.widget.app;

import java.util.ArrayList;
import java.util.HashMap;

import mrl.widget.app.Item.ItemDescription;
import mrl.widget.app.MainApplication.WindowPosition;
import mrl.widget.dockable.UndisposableTabControl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class ItemListModule extends Module {

	private UndisposableTabControl composite;
	private Tree tree;
	private ArrayList<Item> itemList = new ArrayList<Item>();
	private HashMap<Item, TreeItem> tItemMap = new HashMap<Item, TreeItem>();

	@Override
	protected void initializeImpl() {
//		tree = addWindow(new Tree(dummyParent(), SWT.BORDER | SWT.MULTI), WindowPosition.Left);
		
		Composite dummy = addWindow(new Composite(dummyParent(), SWT.NONE), WindowPosition.Left);
		CTabItem tabItem = app().getMainContainer().findTabItem(getName());
		dummy.dispose();
		composite = new UndisposableTabControl(tabItem, app().getMainContainer());
		tabItem.setControl(composite);
		
		tree = new Tree(composite, SWT.BORDER | SWT.MULTI);
		tree.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				for (TreeItem tItem : tree.getItems()) {
					((Item) tItem.getData()).setSelected(false);
				}
				TreeItem[] selection = tree.getSelection();
				for (TreeItem tItem : selection) {
					((Item) tItem.getData()).setSelected(true);
				}
			}
		});
	}
	
	public void addModifyListener(Listener listener){
		tree.addListener(SWT.Modify, listener);
	}
	

	public Item[] getSelectedItems() {
		TreeItem[] sList = tree.getSelection();
		if (sList == null) return new Item[0];
		Item[] items = new Item[sList.length];
		for (int i = 0; i < items.length; i++) {
			items[i] = (Item) sList[i].getData();
		}
		return items;
	}

	@SuppressWarnings("unchecked")
	public <E> E getSelectedData(Class<E> c) {
		Item[] selection = getSelectedItems();
		if (selection.length != 1)
			return null;
		Object data = selection[0].getData();
		if (data == null)
			return null;
		if (c.isAssignableFrom(data.getClass())) {
			return (E) data;
		} else {
			return null;
		}
	}

	public ArrayList<Item> getItemList() {
		return itemList;
	}

	private void addItem(Item item, TreeItem tItem) {
		tItem.setText(item.getLabel());
		tItem.setData(item);
		tItemMap.put(item, tItem);
		itemList.add(item);

		ArrayList<Item> children = item.getChidren();
		if (children != null) {
			for (Item child : children) {
				addItem(item, child);
			}
		}
		notifyModification();
	}
	
	private void notifyModification(){
		Event event = new Event();
		event.widget = tree;
		tree.notifyListeners(SWT.Modify, event);
	}

	public void addSingleItem(String label, Object data) {
		addSingleItem(label, data, null);
	}

	public void addSingleItem(String label, Object data,
			ItemDescription description) {
		Item old = getItemByLabel(label);
		if (old != null)
			removeItem(old);
		Item item = new Item(data);
		item.setDescription(description);
		item.setLabel(label);
		addItem(item);
//		item.setSelected(true);
//		tree.select(tItemMap.get(item));
	}

	public void addItem(Item item) {
		addItem(item, new TreeItem(tree, 0));
		showTabItem();
	}

	public void addItem(Item parent, Item item) {
		TreeItem pItem = tItemMap.get(parent);
		if (pItem == null)
			throw new RuntimeException();
		item.setParent(parent);
		parent.addChild(item);
		addItem(item, new TreeItem(pItem, 0));
		pItem.setExpanded(true);
		showTabItem();
	}

	public void removeItem(Item item) {
		TreeItem tItem = tItemMap.get(item);
		if (tItem == null)
			return;
		removeChildren(item);
		tItem.dispose();
		itemList.remove(item);
		if (item.getParent() != null) {
			item.getParent().removeChild(item);
		}
		notifyModification();
	}

	public void removeChildren(Item item) {
		ArrayList<Item> children = item.getChidren();
		if (children != null) {
			for (Item child : children) {
				removeItem(child);
			}
		}
	}

	public void removeItemByClass(Class<?> c) {
		Item[] items = itemList.toArray(new Item[0]);
		for (Item item : items) {
			if (c.isAssignableFrom(item.getClass())) {
				removeItem(item);
			}
		}
	}
	
	public void removeItemByName(String label) {
		Item item = getItemByLabel(label);
		if (item != null) {
			removeItem(item);
		}
	}

	public Item getItemByLabel(String label) {
		for (Item item : itemList) {
			if (item.getLabel() != null && item.getLabel().equals(label))
				return item;
		}
		return null;
	}
}
