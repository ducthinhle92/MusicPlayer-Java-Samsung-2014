package application.controller;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public abstract class AbstractScreen {
	private Parent root;
	private boolean initialized = false;
	
	protected boolean paused = false;
	protected Stage stage;

	public AbstractScreen(Stage primaryStage) {
		this.stage = primaryStage;
		root = (Parent) primaryStage.getScene().getRoot();
	}
	
	/**
	 * Start the controller
	 */
	public void start() {
		if(!initialized) {
			initialize();
			initialized = true;
		}
		
		paused = false;
		show();
	}
	
	public void show() {
	}

	public void pause() {
		paused = true;
	}
	
	public void resume() {
		paused = false;
	}
	
	/**
	 * Where to initialize components
	 */
	protected void initialize() {
		
	}
	
	public Node findNodeById(String id) {
		return traverseParent(root, id);
	}

	private Node traverseParent(Node _root, String id) {
		SplitPane splitPane = null;
		Pane pane = null;
		Group group = null;
		ToolBar toolBar = null;
		
		if(_root.getId() != null && _root.getId().equals(id))
			return _root;
		
		try {
			pane = (Pane) _root;
			return searchParent(pane, id);
		} catch(ClassCastException e) {
		}
		
		try {
			splitPane = (SplitPane) _root;
			return searchParent(splitPane, id);			
		} catch(ClassCastException e) {
		}
		
		try {
			group = (Group) _root;
			return searchParent(group, id);
		} catch(ClassCastException e) {
		}
		
		try {
			toolBar = (ToolBar) _root;
			return searchParent(toolBar, id);
		} catch(ClassCastException e) {
		}
		
		// this is a leaf node
		return null;
	}
	
	private Node searchParent(ToolBar _root, String id) {
		for(Node node : _root.getItems()) {
			if(node.getId() != null && node.getId().equals(id)) {
				return node;
			}
			else {
				Node temp = traverseParent(node, id);
				if(temp != null)
					return temp;
			}
		}
		return null;
	}

	private Node searchParent(Group _root, String id) {
		for(Node node : _root.getChildren()) {
			if(node.getId() != null && node.getId().equals(id)) {
				return node;
			}
			else {
				Node temp = traverseParent(node, id);
				if(temp != null)
					return temp;
			}
		}
		return null;
	}

	private Node searchParent(SplitPane _root, String id) {
		for(Node node : _root.getItems()) {
			if(node.getId() != null && node.getId().equals(id)) {
				return node;
			}
			else {
				Node temp = traverseParent(node, id);
				if(temp != null)
					return temp;
			}
		}
		return null;
	}

	private Node searchParent(Pane _root, String id) {
		for(Node node : _root.getChildren()) {
			if(node.getId() != null && node.getId().equals(id)) {
				return node;
			}
			else {
				Node temp = traverseParent(node, id);
				if(temp != null)
					return temp;
			}
		}
		return null;
	}
}
