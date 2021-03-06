package application.controller;

import java.util.ArrayList;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.MediaFile;

import org.jaudiotagger.audio.exceptions.CannotWriteException;

import application.FXMLController;
import application.resource.R;
import application.utility.LyricGatherService;
import application.view.ButtonEffector;
import application.view.listener.MediaListener;

@SuppressWarnings({ "unchecked" })
public class PlayScreen extends AbstractScreen implements MediaListener {

	public static final ObservableList<String> lyric = FXCollections
			.observableArrayList();
	protected static final int MODE_EDIT = 0;
	protected static final int MODE_UPDATE = 1;
	protected static final int TO_EDIT_AREA = 2;
	private static final int TO_UPDATE_AREA = 3;
	
	private Button btnShowLyric;
	private StackPane lyricWrapper;
	private SplitPane playPaneSpliter;
	private SplitPane lyricComponent;
	private double dividerPos;
	private ListView<String> listViewLyric;
	private TextField txtTitle;
	private TextField txtArtist;
	private TextArea txtAreaLyric;
	private Button btnEdit;
	protected int lyricMode;
	private StackPane lyricBox;
	private Button btnUpdateLyric;
	private boolean updatingLyric = false;
	private boolean loadedLyric = false;
	private MediaFile currentMedia = null;
	private LyricGatherService liveUpdater;
	private ObservableList<String> backupLyric;
	private Label lbInfo, playTime;
	private VBox mainBackground;
	private Pane controlPane;
	private MediaFile pendingSaveFile = null;

	private EventHandler<WorkerStateEvent> onSuccessHandler = new EventHandler<WorkerStateEvent>() {

		@Override
		public void handle(WorkerStateEvent arg0) {
			System.out.println("Lyric updated successfully");
			ArrayList<String> result = liveUpdater.getResult();
			for(String s : result) {
				lyric.add(s);
			}
			finishGetLiveLyric();
		}
	};
	private EventHandler<WorkerStateEvent> onFailedHandler = new EventHandler<WorkerStateEvent>() {
		@Override
		public void handle(WorkerStateEvent arg0) {
			// recover the last lyric
			if (backupLyric != null) {
				lyric.clear();
				for (String s : backupLyric)
					lyric.add(s);
			}
			finishGetLiveLyric();
		}
	};

	public PlayScreen(Stage primaryStage) {
		super(primaryStage);
	}

	@Override
	protected void initialize() {		
		// retrieve references from FXML controller
		controlPane = FXMLController.getInstance().controlPane;
		lbInfo = FXMLController.getInstance().lbInfo;
		playTime = FXMLController.getInstance().playTime;
		mainBackground = FXMLController.getInstance().mainBackground;		
		
		// setup the album art image
		AnchorPane viewPlay = (AnchorPane) findNodeById("viewPlay");
		ImageView albumArt = (ImageView) findNodeById("albumArt");
		Image imgAlbum = new Image(R.getImage("album_art.png"));
		albumArt.setImage(imgAlbum);
		albumArt.setFitWidth(imgAlbum.getWidth());
		albumArt.setFitHeight(imgAlbum.getHeight());
		viewPlay.widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				albumArt.setLayoutX((newValue.doubleValue()	- 
						albumArt.getFitWidth())/2);
			}
		});		
		viewPlay.heightProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				albumArt.setLayoutY((newValue.doubleValue() 
						- albumArt.getFitHeight())/2);
			}
		});

		playPaneSpliter = (SplitPane) findNodeById("splitPlayPane");
		lyricWrapper = (StackPane) findNodeById("lyricWrapper");
		lyricComponent = (SplitPane) findNodeById("lyricComp");
		lyricBox = (StackPane) findNodeById("lyricBox");
		listViewLyric = (ListView<String>) findNodeById("lvLyric");
		txtTitle = (TextField) findNodeById("txtTitle");
		txtArtist = (TextField) findNodeById("txtArtist");
		btnEdit = (Button) findNodeById("btnEdit");
		btnUpdateLyric = (Button) findNodeById("btnUpdateLyric");
		btnShowLyric = (Button) findNodeById("btnShowLyric");
		
		ButtonEffector.addEffect(btnShowLyric);		
		txtAreaLyric = new TextArea();

		listViewLyric.setItems(lyric);
		lyricWrapper.getChildren().clear();
		playPaneSpliter.setDividerPositions(0, 1);
		lyricComponent.setVisible(false);
		dividerPos = playPaneSpliter.getDividerPositions()[0];		
		
		FXMLController.getInstance().addMediaListener(this);
		addEventHandler();
		
		setLyricMode(MODE_UPDATE);
	}

	@Override
	public void show() {
		validateLyric();
		controlPane.setStyle(R.styles.control_pane_play);
		mainBackground.setStyle(R.styles.background_play);
		lbInfo.setStyle(R.styles.label_info_play);
		playTime.setStyle(R.styles.label_time_play);
	}

	private void validateLyric() {
		MediaFile audio = FXMLController.getInstance().getCurrentMedia();		
		if (!loadedLyric &&	audio != null && lyricComponent.isVisible()) {
			loadedLyric = true;
			txtTitle.setText(audio.getTitle());
			txtArtist.setText(audio.getArtist());

			String strLyric = audio.getLyric();
			if (strLyric != null && strLyric.length() > 0) {
				loadLyric(strLyric);
			} else {
				lyric.clear();
			}
		}
	}

	protected void onClickShowLyric() {
		if (lyricComponent.isVisible()) {
			// hide lyric box
			lyricComponent.setVisible(false);
			lyricWrapper.getChildren().clear();
			dividerPos = playPaneSpliter.getDividerPositions()[0];
			playPaneSpliter.setDividerPositions(0, 1);
		} else {
			// show lyric box
			lyricComponent.setVisible(true);
			playPaneSpliter.setDividerPosition(0, dividerPos);
			lyricWrapper.getChildren().add(lyricComponent);
			
			validateLyric();
		}
	}

	private void addEventHandler() {
		btnShowLyric.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				onClickShowLyric();
			}
		});

		lyricWrapper.heightProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable,
					Number oldValue, Number newValue) {
				btnShowLyric.setLayoutY((newValue.doubleValue() - btnShowLyric
						.getHeight()) / 2);
			}
		});

		btnEdit.setOnMouseClicked(new EventHandler<MouseEvent>() {

			@Override
			public void handle(MouseEvent event) {
				if (lyricMode == MODE_EDIT) {
					// save button clicked
					onSaveClicked();
				} else if (lyricMode == MODE_UPDATE) {
					onEditClicked();
				}
			}
		});

		btnUpdateLyric.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if (lyricMode == MODE_EDIT) {
					onCancelEditLyric();
				} else {
					if (!updatingLyric)
						onUpdateLyricClicked();
					else {
						onCancelGetLyric();
					}
				}
			}
		});

		ChangeListener<String> textChangeListener 
											= new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable,
					String oldValue, String newValue) {
				btnEdit.setDisable(false);
			}
		};
		txtAreaLyric.textProperty().addListener(textChangeListener);		
		txtTitle.textProperty().addListener(textChangeListener);		
		txtArtist.textProperty().addListener(textChangeListener);
	}

	protected void onEditClicked() {
		if(FXMLController.getInstance().getCurrentMedia() == null) {
			return;
		}
		setLyricMode(MODE_EDIT);
		currentMedia = FXMLController.getInstance().getCurrentMedia();

		if (lyric.size() > 0) {
			// backup and copy lyric to edit area
			backupLyric();
			copyLyric(TO_EDIT_AREA);
		}
	}

	protected void onSaveClicked() {
		copyLyric(TO_UPDATE_AREA);
		saveMediaFile(currentMedia);
		setLyricMode(MODE_UPDATE);
		synchronizeMedia();
	}

	protected void onCancelEditLyric() {
		setLyricMode(MODE_UPDATE);
		synchronizeMedia();
	}

	protected void onCancelGetLyric() {
		if (liveUpdater != null)
			liveUpdater.cancel();
		if (backupLyric != null) {
			lyric.clear();
			for (String s : backupLyric)
				lyric.add(s);
		}
		finishGetLiveLyric();
	}

	protected void onUpdateLyricClicked() {
		String title = txtTitle.getText().trim();
		String artist = txtArtist.getText().trim();
		if (title == null || title.length() == 0 || artist == null
				|| artist.length() == 0) {
			return;
		}
		
		currentMedia = FXMLController.getInstance().getCurrentMedia();

		// reset lyric box and disable control components
		updatingLyric = true;
		btnEdit.setDisable(true);
		btnUpdateLyric.setText(R.strings.cancel);
		// backup the lyric in case of failure
		backupLyric();
		// now, clear the lyric box
		lyric.clear();

		liveUpdater = new LyricGatherService(title, artist);
		liveUpdater.setOnSucceeded(onSuccessHandler);
		liveUpdater.setOnFailed(onFailedHandler);
		liveUpdater.start();
	}
	
	protected void copyLyric(int target) {
		if (target == TO_EDIT_AREA) {
			int count = 0;
			String strLyric = lyric.get(0);
			for (String line : lyric) {
				count++;
				if (count == 1)
					continue;
				strLyric += "\n" + line;
			}
			txtAreaLyric.setText(strLyric);
		} 
		else { // target == TO_UPDATE_AREA
			String strLyric = txtAreaLyric.getText();
			lyric.clear();
			if(strLyric.length() == 0) return;
			String[] lines = strLyric.split("\n");
			for(String line : lines) lyric.add(line);
		}
	}

	private void backupLyric() {
		if (lyric.size() > 0) {
			backupLyric = FXCollections.observableArrayList();
			for (String s : lyric)
				backupLyric.add(s);
		} else
			backupLyric = null;
	}
	
	private void loadLyric(String strLyric) {
		String[] lines = strLyric.split("\n");
		if (lyricMode == MODE_UPDATE) {
			listViewLyric.getItems().clear();			
			for (String line : lines) {
				lyric.add(line);
			}
		}
		else {
			StringBuilder builder = new StringBuilder();
			builder.append(lines[0]);
			for(int i=0; i<lines.length; i++)
				builder.append("\n" + lines[i]);
			txtAreaLyric.clear();
			txtAreaLyric.setText(builder.toString());
		}
	}

	private void finishGetLiveLyric() {
		btnUpdateLyric.setText(R.strings.update_lyric);
		btnEdit.setDisable(false);
		updatingLyric = false;
		saveMediaFile(currentMedia);
		
		// update lyric content to synchronize with current media
		synchronizeMedia();
	}

	/**
	 * synchronize the current media being update/edit lyric with
	 * the current playing media.
	 */
	private void synchronizeMedia() {
		MediaFile nowPlay = FXMLController.getInstance().getCurrentMedia();
		if(!nowPlay.getPath().equals(currentMedia.getPath())) {
			currentMedia = nowPlay;
			txtTitle.setText(nowPlay.getTitle());
			txtArtist.setText(nowPlay.getArtist());
			setLyricMode(MODE_UPDATE);
			
			String strLyric = nowPlay.getLyric();
			if (strLyric != null && strLyric.length() > 0) {
				loadLyric(strLyric);
			} else {
				lyric.clear();
			}
		}
	}

	protected void setLyricMode(int mode) {
		lyricMode = mode;
		if (lyricMode == MODE_EDIT) {
			lyricBox.getChildren().clear();
			lyricBox.getChildren().add(txtAreaLyric);
			btnEdit.setText(R.strings.save);
			btnUpdateLyric.setText(R.strings.cancel);
			btnEdit.setDisable(true);
			txtTitle.setEditable(true);
			txtArtist.setEditable(true);
		} else {
			lyricBox.getChildren().clear();
			lyricBox.getChildren().add(listViewLyric);
			btnEdit.setText(R.strings.edit_lyric);
			btnUpdateLyric.setText(R.strings.update_lyric);
			btnEdit.setDisable(false);
			txtTitle.setEditable(false);
			txtArtist.setEditable(false);
		}
	}

	/**
	 * Save the lyric to according media, base on the media was being played
	 * at the time begin edit/update state is started
	 * @param media
	 */
	protected void saveMediaFile(MediaFile media) {
		media.setTitle(txtTitle.getText());
		media.setArtist(txtArtist.getText());	
		
		// save lyric
		if(lyricMode == MODE_EDIT) {
			media.setLyric(txtAreaLyric.getText());
		}
		else {
			if(lyric.size() == 0)
				return; // nothing to save
			
			StringBuilder strLyric = new StringBuilder();			
			strLyric.append(lyric.get(0));
			for(int i=1; i<lyric.size(); i++) {
				strLyric.append("\n" + lyric.get(i));
			}
			media.setLyric(strLyric.toString());
		}		
		
		pendingSaveFile = media;
	}

	@Override
	public void onMediaChanged() {
		if(lyricMode != MODE_EDIT && !updatingLyric) {	
			loadedLyric = false;
			validateLyric();
		}
		
		try {
			if(pendingSaveFile != null) {
				pendingSaveFile.saveFile();
			}
		} catch (CannotWriteException e) {
			e.printStackTrace();
		}
		pendingSaveFile = null;
	}
}
