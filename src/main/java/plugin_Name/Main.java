package plugin_Name;

import java.awt.event.WindowEvent;
import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.PlugIn;

public class Main implements PlugIn {

	static String pluginName = "plugin Name";
	static String pluginVersion = "v0.0.1";

	ProgressDialog progressDialog;
	boolean processingDone = false;
	boolean continueProcessing = true;

	static ProcessSettings pS;

	/**
	 * Takes care of the plugin configuration, file selection, and looping over the
	 * images - normally does not require changes
	 */
	public void run(String arg) {
		IJ.log("Plugin Started");
		pS = null;
		ImageSetting iS = null;
		try {
			IJ.log("Plugin GD");
			pS = ProcessSettings.initByGD(pluginName, pluginVersion);
			IJ.log("Image Settings GD");
			if (!pS.valid)
				return;
			iS = ImageSetting.initByGD(pluginName, pluginVersion);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		if (pS.resultsToNewFolder) {
			pS.selectOutputDir();
		}
		startProgressDialog(pS.toArray(), pS.getNOfTasks());

		IJ.log("Initialized settings");

		for (int task = 0; task < pS.getNOfTasks(); task++) {
			progressDialog.updateBarText("in progress...");
			IJ.log("processing task " + task);
			Processing.doProcessing(pS.paths.get(task), pS.names.get(task), pS.getOutputDir(task), iS, progressDialog);
			progressDialog.updateBarText("finished!");
			progressDialog.moveTask(task);
		}
		IJ.log("Pugin done!");
	}

	/**
	 * Wraps functionality of opening Images in IJ depending on the chosen file format
	 * @param path path to file to be opened
	 * @return reference of opened ImagePlus
	 */
	protected static ImagePlus openImage(String path) {
		ImagePlus imp;
		if (pS.selectedBioFormat.equals(ProcessSettings.bioFormats[0])) {
			imp = IJ.openImage(path);
		} else {
			IJ.run("Bio-Formats", "open=[" + path
					+ "] autoscale color_mode=Default rois_import=[ROI manager] view=Hyperstack stack_order=XYCZT");
			imp = WindowManager.getCurrentImage();
		}
		return imp;
	}

	private void startProgressDialog(String[] tasks, int nOfTasks) {
		progressDialog = new ProgressDialog(tasks, nOfTasks);
		progressDialog.setLocation(0, 0);
		progressDialog.setVisible(true);
		progressDialog.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(WindowEvent winEvt) {
				if (processingDone == false) {
					IJ.error("Script stopped...");
				}
				continueProcessing = false;
				return;
			}
		});
	}

}
