package application.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

public class Config {
	public static final String SETTING_VOLUME = "volume";
	public static float defaultVolume = 50;
	
	private static final int Name = 0;
	private static final int Value = 1;

	static File configFile;
	private static Config instance;
	private BufferedWriter writer;
	private BufferedReader reader;
	
	private HashMap<String, Float> configs = new HashMap<>();

	Config() {
		instance = this;
		configFile = new File("setting.cfg");

		try {			
			if (!configFile.exists()) {
				FileWriter fWriter = new FileWriter(configFile);
				writer = new BufferedWriter(fWriter);
				writer.flush();
				writer.close();
			}
			
			loadSetting();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Cannot load setting.");
		}
	}

	private void loadSetting() {
		String line;
		int count = 0;
		try {			
			FileReader f = null;
			f = new FileReader(configFile);
			reader = new BufferedReader(f);
			
			line = reader.readLine();
			while (line != null) {
				count++;
				String[] config = line.split(":");
				Float value = Float.parseFloat(config[Value]);
				configs.put(config[Name], value);
				line = reader.readLine();
			}
			System.out.println("count = " + count);
			if(count == 0) {
				initSetting();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			reader.close();
			
			FileWriter fWriter = new FileWriter(configFile);
			writer = new BufferedWriter(fWriter);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initSetting() {
		System.out.println("init");
		setValue(SETTING_VOLUME, defaultVolume);
	}

	public static Config getInstance() {
		if (instance == null)
			return new Config();
		return instance;
	}

	public Float getValue(String key) {
		return configs.get(key);
	}
	
	public void setValue(String key, float value) {
		Float f = configs.replace(key, value);
		if(f == null) {
			System.out.println("put new");
			configs.put(key, value);
		}
		else {
			System.out.println("update");
		}
	}

	public void dispose() {
		try {
			if (writer != null) {
				saveConfig();
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void saveConfig() throws IOException {
		for(Entry<String, Float> cfg :configs.entrySet()) {
			writer.write(cfg.getKey() + ":" + cfg.getValue() + "\n");
		}
		writer.flush();
	}
}
