package plugin_Name;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.WaitForUserDialog;
import ij.io.FileInfo;

/**
 * Stores data about basic processing settings such as type of input (e.g. # of
 * channel), input and output file section etc. Provides GUI to choose settings
 * and methods to implement the methods to retrieves file names and paths and
 * stores the values.
 * 
 * @author sebas
 * 
 */
public class ProcessSettings {

	// -------------------- Raw Process Settings

	boolean valid = false; // input is valid

	// add necessary Settings and defaults here

	static final String[] taskVariant = { "active image in FIJI", "all images open in FIJI", "manual file selection",
			"list (txt)", "pattern matching" };
	String selectedTaskVariant = taskVariant[2];

	static final String[] bioFormats = { ".tif", "raw microscopy file (e.g. OIB-file)" };
	String selectedBioFormat = bioFormats[1];

	String pattern = "";

//	boolean saveDateToFilenames = false;
//	boolean saveParam = true;
//	String ChosenNumberFormat = "Germany (0,00...)";	
	boolean resultsToNewFolder = false;
	String resultsDir = ""; // Specifies dir where output files will be saved if they are to be saved no new
							// folder
//	int channels = 3;

	// --------------------- Task data

	ArrayList<String> names = new ArrayList<String>();
	ArrayList<String> paths = new ArrayList<String>();

	private ProcessSettings() throws IOException {
		super();
	}

	/**
	 * Method to init Processes settings with default options
	 * 
	 * @return
	 * @throws IOException
	 */
	public static ProcessSettings initDefault() throws IOException {
		ProcessSettings inst = new ProcessSettings();
		inst.fileFinder();
		return inst;
	}

	/**
	 * Constructs new Object and triggers a GD for the user
	 * 
	 * @return User-chosen Processing Settings
	 * @throws IOException
	 */
	public static ProcessSettings initByGD(String pluginName, String pluginVersion) throws IOException {

		ProcessSettings inst = new ProcessSettings(); // returned instance of ImageSettingClass

		final Font headingFont = new Font("Sansserif", Font.BOLD, 14);
		final Font textFont = new Font("Sansserif", Font.PLAIN, 12);

		GenericDialog gd = new GenericDialog(pluginName + " - Image Processing Settings");
		gd.setInsets(0, 0, 0);
		gd.addMessage(pluginName + " - Version " + pluginVersion, headingFont);
		gd.addMessage("Insert Processing settings", textFont);

		// Change as necessary
		gd.setInsets(0, 0, 0);
		gd.addChoice("Example Choice ", taskVariant, inst.selectedTaskVariant);
		gd.setInsets(0, 0, 0);
		gd.addChoice("Input File format ", bioFormats, inst.selectedBioFormat);
		gd.setInsets(0, 0, 0);
		gd.addStringField("Enter pattern for pattern Matching", inst.pattern, 16);

		// show Dialog-----------------------------------------------------------------
		gd.showDialog();

		// read and process variables--------------------------------------------------

		inst.selectedTaskVariant = gd.getNextChoice();
		inst.selectedBioFormat = gd.getNextChoice();
		inst.pattern = gd.getNextString();

		inst.valid = !gd.wasCanceled();

		inst.fileFinder();

		return inst;
	}

	/**
	 * provides method to choose files and stores the retrieved informations, needs
	 * to be called before going on with processing.
	 */
	void fileFinder() throws IOException {

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {

		}

		if (this.selectedTaskVariant == taskVariant[0]) { // only one image open
			if (WindowManager.getIDList() == null) {
				new WaitForUserDialog("Plugin canceled - no image open in FIJI!").show();
				throw new IOException();
			} else {
				FileInfo info = WindowManager.getCurrentImage().getOriginalFileInfo();
				this.names.add(info.fileName); // get name
				this.paths.add(info.directory); // get directory
			}
		} else if (this.selectedTaskVariant == taskVariant[1]) { // select files indiviually
			if (WindowManager.getIDList() == null) {
				new WaitForUserDialog("Plugin canceled - no image open in FIJI!").show();
				throw new IOException();
			}
			int IDlist[] = WindowManager.getIDList();
			if (IDlist.length == 1) {
				selectedTaskVariant = taskVariant[0];
				FileInfo info = WindowManager.getCurrentImage().getOriginalFileInfo();
				names.add(info.fileName); // get name
				paths.add(info.directory); // get directory
			} else {
				for (int i = 0; i < IDlist.length; i++) {
					FileInfo info = WindowManager.getImage(IDlist[i]).getOriginalFileInfo();
					names.add(info.fileName); // get name
					paths.add(info.directory); // get directory
				}
			}
		} else if (this.selectedTaskVariant == taskVariant[2]) {

			OpenFilesDialog od = new OpenFilesDialog();
			od.setLocation(0, 0);
			od.setVisible(true);

			od.addWindowListener(new java.awt.event.WindowAdapter() {
				public void windowClosing(WindowEvent winEvt) {
					try {
						throw new Exception();
					} catch (Exception e) {
					}
				}
			});

			// Waiting for od to be done
			while (od.done == false) {
				try {
					Thread.currentThread().sleep(50);
				} catch (Exception e) {
				}
			}
			for (File f : od.filesToOpen) {
				names.add(f.getName());
				paths.add(f.getParent() + System.getProperty("file.separator"));
			}
		} else if (this.selectedTaskVariant == taskVariant[3]) {
			String txtPath = System.getProperty("user.dir");
			boolean validInput = false;
			while (!validInput) {
				txtPath = choosePathTxt("Choose txt containing paths of files to process", txtPath);
				if (txtPath.contains(".txt"))
					validInput = true;
			}
			// TODO read in paths fron list
		} else if (this.selectedTaskVariant == taskVariant[4]) {
			String dirToSearch = System.getProperty("user.dir");
			dirToSearch = choosePathTxt("Choose Directory to start pattern matching", dirToSearch);
			for (String s : matchPattern(dirToSearch, "pattern")) {
				names.add(s.substring(s.lastIndexOf(System.getProperty("file.seperator")), s.length())); // todo check
																											// the
																											// indizes
				paths.add(s.substring(0, s.lastIndexOf(System.getProperty("file.seperator"))));
			}
		}
	}

	public int getNOfTasks() {
		return this.names.size();
	}

	public class OpenFilesDialog extends javax.swing.JFrame implements ActionListener {
		LinkedList<File> filesToOpen = new LinkedList<File>();
		boolean done = false, dirsaved = false;
		File saved;// = new File(getClass().getResource(".").getFile());
		JMenuBar jMenuBar1;
		JMenu jMenu3, jMenu5;
		JSeparator jSeparator2;
		JPanel bgPanel;
		JScrollPane jScrollPane1;
		JList Liste1;
		JButton loadButton, removeButton, goButton;

		public OpenFilesDialog() {
			super();
			initGUI();
		}

		private void initGUI() {
			int prefXSize = 600, prefYSize = 400;
			this.setMinimumSize(new java.awt.Dimension(prefXSize, prefYSize + 40));
			this.setSize(prefXSize, prefYSize + 40);
			this.setTitle("Multi-Task-Manager - by JN Hansen (\u00a9 2016)");
//			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			// Surface
			bgPanel = new JPanel();
			bgPanel.setLayout(new BoxLayout(bgPanel, BoxLayout.Y_AXIS));
			bgPanel.setVisible(true);
			bgPanel.setPreferredSize(new java.awt.Dimension(prefXSize, prefYSize - 20));
			{
				jScrollPane1 = new JScrollPane();
				jScrollPane1.setHorizontalScrollBarPolicy(30);
				jScrollPane1.setVerticalScrollBarPolicy(20);
				jScrollPane1.setPreferredSize(new java.awt.Dimension(prefXSize - 10, prefYSize - 60));
				bgPanel.add(jScrollPane1);
				{
					Liste1 = new JList();
					jScrollPane1.setViewportView(Liste1);
					Liste1.setModel(new DefaultComboBoxModel(new String[] { "" }));
				}
				{
					JPanel spacer = new JPanel();
					spacer.setMaximumSize(new java.awt.Dimension(prefXSize, 10));
					spacer.setVisible(true);
					bgPanel.add(spacer);
				}
				{
					JPanel bottom = new JPanel();
					bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
					bottom.setMaximumSize(new java.awt.Dimension(prefXSize, 10));
					bottom.setVisible(true);
					bgPanel.add(bottom);
					int locHeight = 40;
					int locWidth3 = prefXSize / 4 - 60;
					{
						loadButton = new JButton();
						loadButton.addActionListener(this);
						loadButton.setText("add files");
						loadButton.setMinimumSize(new java.awt.Dimension(locWidth3, locHeight));
						loadButton.setVisible(true);
						loadButton.setVerticalAlignment(SwingConstants.BOTTOM);
						bottom.add(loadButton);
					}
					{
						removeButton = new JButton();
						removeButton.addActionListener(this);
						removeButton.setText("remove selected files");
						removeButton.setMinimumSize(new java.awt.Dimension(locWidth3, locHeight));
						removeButton.setVisible(true);
						removeButton.setVerticalAlignment(SwingConstants.BOTTOM);
						bottom.add(removeButton);
					}
					{
						goButton = new JButton();
						goButton.addActionListener(this);
						goButton.setText("start processing");
						goButton.setMinimumSize(new java.awt.Dimension(locWidth3, locHeight));
						goButton.setVisible(true);
						goButton.setVerticalAlignment(SwingConstants.BOTTOM);
						bottom.add(goButton);
					}
				}
			}
			getContentPane().add(bgPanel);
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			Object eventQuelle = ae.getSource();
			if (eventQuelle == loadButton) {
				JFileChooser chooser = new JFileChooser();
				chooser.setPreferredSize(new Dimension(600, 400));
				if (dirsaved) {
					chooser.setCurrentDirectory(saved);
				}
				chooser.setMultiSelectionEnabled(true);
				Component frame = null;
				chooser.showOpenDialog(frame);
				File[] files = chooser.getSelectedFiles();
				for (int i = 0; i < files.length; i++) {
//					IJ.log("" + files[i].getPath());
					filesToOpen.add(files[i]);
					saved = files[i];
					dirsaved = true;
				}
				updateDisplay();
			}
			if (eventQuelle == removeButton) {
				int[] indices = Liste1.getSelectedIndices();
				for (int i = indices.length - 1; i >= 0; i--) {
//					IJ.log("remove " + indices[i]);
					filesToOpen.remove(indices[i]);
				}
				updateDisplay();
			}
			if (eventQuelle == goButton) {
				done = true;
				dispose();
			}

		}

		@SuppressWarnings("unchecked")
		public void updateDisplay() {
			String resultsString[] = new String[filesToOpen.size()];
			for (int i = 0; i < filesToOpen.size(); i++) {
				resultsString[i] = (i + 1) + ": " + filesToOpen.get(i).getName();
			}
			Liste1.setListData(resultsString);
		}
	}

	/**
	 * choose path to dir
	 * 
	 * @param message
	 * @param defaultpath
	 * @return
	 */
	public static String choosePathTxt(String message, String defaultpath) {
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setMultiSelectionEnabled(false);
		fc.setCurrentDirectory(new File(defaultpath));
		if (fc.showDialog(fc, message) == JFileChooser.APPROVE_OPTION) {
//		   System.out.println(fc.getSelectedFile().getAbsoluteFile());
		}
		String selectedpath = fc.getSelectedFile().getPath();
		return selectedpath;
	}

	/**
	 * choose path of txt containing text
	 * 
	 * @param message
	 * @param defaultpath
	 * @return
	 */
	public static String choosePath(String message, String defaultpath) {
		if (defaultpath == "") {
			defaultpath = System.getProperty("user.dir");
		}
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setMultiSelectionEnabled(false);
		fc.setCurrentDirectory(new File(defaultpath));
		if (fc.showDialog(fc, message) == JFileChooser.APPROVE_OPTION) {
//		   System.out.println(fc.getSelectedFile().getAbsoluteFile());
		}
		String selectedpath = fc.getSelectedFile().getPath();
		return selectedpath;
	}

	/**
	 * implements string pattern matching to search in all sub directories of
	 * specified dir for files to process.
	 * 
	 * @param path    path to directory where the search starts
	 * @param pattern pattern to matched in filenames
	 * @return full paths of files matching requirements in the specified dir
	 */
	private ArrayList<String> matchPattern(String path, String pattern) {
		// TODO enter logic
		return null;
	}

	/**
	 * 
	 * @return task list as Array
	 */
	public String[] toArray() {
		String[] s = new String[this.getNOfTasks()];
		for (int i = 0; i < this.getNOfTasks(); i -= -1) {
			s[i] = this.names.get(i);
		}
		return s;
	}

	/**
	 * Returns the dir where the output should be stored (dir of input image or
	 * fixed dir)
	 */
	public String getOutputDir(int taskIndex) {
		return this.resultsToNewFolder ? this.resultsDir : this.paths.get(taskIndex);
	}

	public void selectOutputDir() {
		if (this.resultsToNewFolder) {
			choosePath("Choose output Folder", this.paths.get(0));
		}
	}
}
