package mastery.cleaner;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class ExamPaperCleaner {

	private static final Logger logger = LogManager.getLogger(ExamPaperCleaner.class);
	private static final String C_DRIVER = "C:";
	private static final String TEMP = "temp";
	private static final String JPEG_FILE_EXT = ".jpg";
	private static final String PDF_FILE_EXT = ".pdf";
	private static final String DLL_FILE_EXT = ".dll";
	private static final String XML_FILE_EXT = ".xml";
	private static final String FILE_SPRT = "//";
	private static final String ROOT_FOLDER = C_DRIVER + "\\ExamPaperCleaner";
	private static final String LIB_FOLDER = ROOT_FOLDER + "\\lib";
	private static final String LOG_FOLDER = ROOT_FOLDER + "\\log";
	private static final String RAW_FOLDER = ROOT_FOLDER + "\\raw";
	private static final String IMG_FOLDER = ROOT_FOLDER + "\\image";
	private static final String PROC_FOLDER = ROOT_FOLDER + "\\processed";
	private static final String RESULT_FOLDER = ROOT_FOLDER + "\\result";
	private static final String OPENCV_FILE_NAME = "opencv_java342";
	private static final String OPENCV_FILE = OPENCV_FILE_NAME + DLL_FILE_EXT;
	private static final String LOG_FILE_NAME = "log4j2";
	private static final String LOG_FILE = LOG_FILE_NAME + XML_FILE_EXT;
	private static final int PROG_MIN = 0;
	private static final int PROG_MAX = 100;
	private static enum LogLevel {INFO, WARN, ERROR};
	
	private int baseBlackMaxVal = 26;
	private int lightBlackMaxVal = 127;
	private int redMaxVal = 76;
	private int blueMaxVal = 76;
	private int addMaxVal = 127;
	
	private Scalar blackMin1 = new Scalar(6,0,0);
	private Scalar blackMax1 = new Scalar(101,255,this.baseBlackMaxVal);

	private Scalar blackMin2 = new Scalar(136,0,0);
	private Scalar blackMax2 = new Scalar(164,255,this.baseBlackMaxVal);
	
	private Scalar lightBlackMin = new Scalar(0,0,0);
	private Scalar lightBlackMax = new Scalar(0,0,this.lightBlackMaxVal);
	
	private Scalar redMin1 = new Scalar(165,0,0);
	private Scalar redMax1 = new Scalar(180,255,this.redMaxVal);
	
	private Scalar redMin2 = new Scalar(0,0,0);
	private Scalar redMax2 = new Scalar(5,255,this.redMaxVal);
	
	private Scalar blueMin = new Scalar(102,0,0);
	private Scalar blueMax = new Scalar(135,255,this.blueMaxVal);
	
	private Scalar addMin1 = new Scalar(6,0,this.baseBlackMaxVal);
	private Scalar addMax1 = new Scalar(101,255,addMaxVal);
	
	private Scalar addMin2 = new Scalar(136,0,this.baseBlackMaxVal);
	private Scalar addMax2 = new Scalar(164,255,addMaxVal);
	
	private File rootFolder;
	private File libFolder;
	private File logFolder;
	private File rawFolder;
	private File imgFolder;
	private File procFolder;
	private File resultFolder;
	
	private JFrame frame = new JFrame();
	private JTextArea logArea = new JTextArea();
	private JProgressBar progressBar = new JProgressBar();
	private JButton startButton = new JButton("Start");
	private JSpinner bbSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
	private JSpinner lbSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
	private JSpinner redSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
	private JSpinner blueSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
	private JSpinner addSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
	
	public ExamPaperCleaner() {
		
		try {
			File logConfigFile = new File(LOG_FOLDER + FILE_SPRT + LOG_FILE);
			LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
			context.setConfigLocation(logConfigFile.toURI());
		}catch(Exception e) {
			logger.error(e.getMessage());
			e.printStackTrace();
		}
			
	}
	
	public void start(){

		try {
			this.createLayout();
			this.checkingFolders();
			this.loadLibrary();
			startButton.setEnabled(true);
		} catch (Exception e) {
			writeLog("Library load fails! Please contact Travis Li for help!", LogLevel.ERROR);
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		
	}
	
	private void createLayout(){
		frame.setTitle("Exam Paper Cleaner v1.2");
		frame.setSize(900, 800);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel insPanel = new JPanel();
		insPanel.setBorder(new EmptyBorder(0,5,0,5));
		insPanel.setLayout(new GridLayout(7,1));
		
		JLabel ins0 = new JLabel("Instruction:");
		JLabel ins1 = new JLabel("1. Put raw files under " + RAW_FOLDER + ".");
		JLabel ins2 = new JLabel("2. Select those threshold values.");
		JLabel ins3 = new JLabel("3. Press start button.");
		JLabel ins4 = new JLabel("4. After the progress bar become 100%, get the cleared files in " + RESULT_FOLDER + ".");
		JLabel ins5 = new JLabel("5. Please remember to copy all files in result folder, all files will be cleared when start button is pressed.");
		
		progressBar.setMinimum(PROG_MIN);
		progressBar.setMaximum(PROG_MAX);
		progressBar.setValue(PROG_MIN);
	    progressBar.setStringPainted(true);
		insPanel.add(progressBar);
		insPanel.add(ins0);
		insPanel.add(ins1);
		insPanel.add(ins2);
		insPanel.add(ins3);
		insPanel.add(ins4);
		insPanel.add(ins5);
		
		frame.add(insPanel, BorderLayout.SOUTH);
		
		JPanel spinnerPanel = new JPanel(new GridLayout(6,1));
		
		JPanel bbFuncPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		JLabel bbFuncLabel1 = new JLabel("Base Black Color Threshold");
		JLabel bbFuncLabel2 = new JLabel("[0-255]. If \"questions\" are not clear tune this up!");
		
		bbSpinner.setValue(this.baseBlackMaxVal);
		
		bbFuncPanel.add(bbFuncLabel1);
		bbFuncPanel.add(bbSpinner);
		bbFuncPanel.add(bbFuncLabel2);
		
		JPanel lbFuncPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		JLabel lbFuncLabel1 = new JLabel("Light Black Color Threshold");
		JLabel lbFuncLabel2 = new JLabel("[0-255]. If pencils are still there tune this down!");
		
		lbSpinner.setValue(this.lightBlackMaxVal);
		
		lbFuncPanel.add(lbFuncLabel1);
		lbFuncPanel.add(lbSpinner);
		lbFuncPanel.add(lbFuncLabel2);
		
		JPanel redFuncPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		JLabel redFuncLabel1 = new JLabel("Red Color Threshold");
		JLabel redFuncLabel2 = new JLabel("[0-255]. If red pens are still there tune this down!");
		
		redSpinner.setValue(this.redMaxVal);
		
		redFuncPanel.add(redFuncLabel1);
		redFuncPanel.add(redSpinner);
		redFuncPanel.add(redFuncLabel2);
		
		JPanel blueFuncPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		JLabel blueFuncLabel1 = new JLabel("Blue Color Threshold");
		JLabel blueFuncLabel2 = new JLabel("[0-255]. If blue pens are still there tune this down!");
		
		blueSpinner.setValue(this.blueMaxVal);
		
		blueFuncPanel.add(blueFuncLabel1);
		blueFuncPanel.add(blueSpinner);
		blueFuncPanel.add(blueFuncLabel2);
		
		JPanel addFuncPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		JLabel addFuncLabel1 = new JLabel("Additional Color Threshold");
		JLabel addFuncLabel2 = new JLabel("[0-255]. If white pixels appear inside characters tune this up!");
		
		addSpinner.setValue(this.addMaxVal);
		
		addFuncPanel.add(addFuncLabel1);
		addFuncPanel.add(addSpinner);
		addFuncPanel.add(addFuncLabel2);
		
		JPanel buttonPanel = new JPanel(new GridLayout(1,1));
		buttonPanel.setBorder(new EmptyBorder(0,5,0,5));
		buttonPanel.add(startButton);
				
		spinnerPanel.add(bbFuncPanel);
		spinnerPanel.add(lbFuncPanel);
		spinnerPanel.add(redFuncPanel);
		spinnerPanel.add(blueFuncPanel);
		spinnerPanel.add(addFuncPanel);
		spinnerPanel.add(buttonPanel);
		
		frame.add(spinnerPanel, BorderLayout.NORTH);
		
		startButton.setEnabled(false);
		startButton.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {
	        	 setAndUpdateFilterVal();
	            CleanHelper ch = new CleanHelper();
	            ch.execute();
	         }          
	      });
		
		JPanel logPanel = new JPanel(new GridLayout(1,1));
		logPanel.setBorder(new EmptyBorder(0,5,0,5));
		logArea.setEditable(false);
		
		DefaultCaret caret = (DefaultCaret)logArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		JScrollPane scroll = new JScrollPane(logArea);
		logPanel.add(scroll);
		
		frame.add(logPanel, BorderLayout.CENTER);
		
		frame.setVisible(true);
		
	}

	private void loadLibrary() throws Exception{
		
		writeLog("Loading library...", LogLevel.INFO);
		
		System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");

		File libFile = new File(LIB_FOLDER + FILE_SPRT + OPENCV_FILE);
		
		System.load(libFile.getAbsolutePath());
		
		writeLog("Library successfully loaded.", LogLevel.INFO);
	}
	
	private void checkingFolders(){

		writeLog("Checking required folder...", LogLevel.INFO);
		
		rootFolder = new File(ROOT_FOLDER);
		if(!rootFolder.exists()){
			writeLog("Creating root folder...", LogLevel.INFO);
			rootFolder.mkdirs();
		}

		rawFolder = new File(RAW_FOLDER);

		if(!rawFolder.exists()){
			writeLog("Creating raw folder...", LogLevel.INFO);
			rawFolder.mkdirs();
		}

		libFolder = new File(LIB_FOLDER);

		if(!libFolder.exists()){
			writeLog("Creating lib folder...", LogLevel.INFO);
			libFolder.mkdirs();
		}
		
		logFolder = new File(LOG_FOLDER);

		if(!logFolder.exists()){
			writeLog("Creating log folder...", LogLevel.INFO);
			logFolder.mkdirs();
		}

		imgFolder = new File(IMG_FOLDER);

		if(!imgFolder.exists()){
			writeLog("Creating image folder...", LogLevel.INFO);
			imgFolder.mkdirs();
		}else{
			
		}

		procFolder = new File(PROC_FOLDER);

		if(!procFolder.exists()){
			writeLog("Creating processed folder...", LogLevel.INFO);
			procFolder.mkdirs();
		}

		resultFolder = new File(RESULT_FOLDER);

		if(!resultFolder.exists()){
			writeLog("Creating result folder...", LogLevel.INFO);
			resultFolder.mkdirs();
		}
	}
	
	private void deteleFileInFolders() {
		deleteFileInFolder(imgFolder);
		deleteFileInFolder(procFolder);
		writeLog("Deleting Files in Folder - " + resultFolder.getAbsolutePath(), LogLevel.INFO);
		for(File file:resultFolder.listFiles()){
			file.delete();
		}
	}

	private void deleteFileInFolder(File folder){
		
		writeLog("Deleting Files in Folder - " + folder.getAbsolutePath(), LogLevel.INFO);

		for(File file:folder.listFiles()){
			try {
				FileUtils.deleteDirectory(file);
			} catch (IOException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
			}
		}

	}
	
	private class CleanHelper extends SwingWorker<Void, Void>{

		@Override
		protected Void doInBackground() throws Exception {
			
			startButton.setEnabled(false);
			
			deteleFileInFolders();
			
			logArea.setText("");
			
			writeLog("Exam Paper Cleaner Start... ", LogLevel.INFO);
			
			progressBar.setValue(PROG_MIN);
			
			int fileCnt = 1;

			File[] fileArray = rawFolder.listFiles();
			
			int totalFileNo = fileArray.length;
			
			for(File file: fileArray){

				writeLog("Processing File - " + file.getName(), LogLevel.INFO);

				File imgWkFolder = pdfToImg(file, fileCnt);
				File procWkFolder = processImg(imgWkFolder, fileCnt);

				String folderName = file.getName().split("\\.")[0];

				logger.info("Rename Image Folder");
				File imgRenameFolder = new File(imgFolder.getAbsoluteFile() + FILE_SPRT + folderName);
				imgWkFolder.renameTo(imgRenameFolder);

				logger.info("Rename Processed Folder");
				File procRenameFolder = new File(procFolder.getAbsoluteFile() + FILE_SPRT + folderName);
				procWkFolder.renameTo(procRenameFolder);

				combinesImgsToPDF(procRenameFolder);

				Double progress = ((double)(fileCnt) / (double)(totalFileNo)) * 100;
				
				progressBar.setValue(progress.intValue());
				
				fileCnt++;

			}

			progressBar.setValue(PROG_MAX);
			return null;
		}
		
		@Override
	    public void done() {
			writeLog("Cleaning Completed! Please check " + resultFolder.getAbsolutePath() + "!",LogLevel.INFO);
			startButton.setEnabled(true);
			try {
				Runtime.getRuntime().exec("explorer.exe /select, " + resultFolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
	    }
		
	}

	private File pdfToImg(File file, int cnt){

		writeLog("Convert PDF to Images",LogLevel.INFO);

		File wkFolder = new File(IMG_FOLDER + FILE_SPRT + TEMP + cnt);

		try {

			if(!wkFolder.exists()){
				wkFolder.mkdirs();
			}

			final PDDocument document = PDDocument.load(file);

			PDFRenderer pdfRenderer = new PDFRenderer(document);
			for (int page = 0; page < document.getNumberOfPages(); ++page)
			{
				int pageNo = page+1;
				writeLog("Converting page " + pageNo,LogLevel.INFO);
				BufferedImage bim = pdfRenderer.renderImageWithDPI(page, 300, ImageType.RGB);
				String fileName = wkFolder.getAbsolutePath() + FILE_SPRT + "image" + page + JPEG_FILE_EXT ;
				ImageIOUtil.writeImage(bim, fileName, 300);
			}
			document.close();
		} catch (IOException e){
			writeLog("Exception while getting image from pdf document", LogLevel.ERROR);
		}

		return wkFolder;

	}


	//image
	private File processImg(File file, int cnt){

		writeLog("Analysing Image...", LogLevel.INFO);

		File wkFolder = new File(PROC_FOLDER + FILE_SPRT + TEMP + cnt);

		if(!wkFolder.exists()){
			wkFolder.mkdirs();
		}

		File[] imgFiles = file.listFiles();

		sortFilesByModifiedDate(imgFiles);

		for(File imgFile: imgFiles){

			writeLog("Working on " + imgFile.getAbsolutePath(), LogLevel.INFO);

			Mat src = Imgcodecs.imread(imgFile.getAbsolutePath());
			
			//turn src image in HSV color space
			writeLog("Turn RGB to HSV...", LogLevel.INFO);
			Mat hsvImg = new Mat();
			Imgproc.cvtColor(src, hsvImg, Imgproc.COLOR_BGR2HSV);
						
			Mat dest = new Mat();
			
			//find those non red blue dark black color
			writeLog("Applying Base Black Filter...", LogLevel.INFO);
			Mat b1 = new Mat();
			Core.inRange(hsvImg, blackMin1, blackMax1, b1);
			/*String b1File = wkFolder + FILE_SPRT + "B1_" + imgFile.getName();
			Imgcodecs.imwrite(b1File, b1);*/
			
			Mat b2 = new Mat();
			Core.inRange(hsvImg, blackMin2, blackMax2, b2);
			/*String b2ile = wkFolder + FILE_SPRT + "B2_" + imgFile.getName();
			Imgcodecs.imwrite(b2File, b2);*/
			
			Core.add(b1, b2, dest);
			
			//find those dark black color
			writeLog("Applying Light Black Filter...", LogLevel.INFO);
			Mat lb = new Mat();
			Core.inRange(hsvImg, lightBlackMin, lightBlackMax, lb);
			/*String lbFile = wkFolder + FILE_SPRT + "LB_" + imgFile.getName();
			Imgcodecs.imwrite(lbFile, lb);*/
				
			Core.add(dest, lb, dest);
			
			//find those red black color
			Mat r1 = new Mat();
			Core.inRange(hsvImg, redMin1, redMax1, r1);
			/*String r1File = wkFolder + FILE_SPRT + "R1_" + imgFile.getName();
			Imgcodecs.imwrite(r1File, r1);*/
			
			Core.add(dest, r1, dest);
			
			Mat r2 = new Mat();
			Core.inRange(hsvImg, redMin2, redMax2, r2);
			/*String r2File = wkFolder + FILE_SPRT + "R2_" + imgFile.getName();
			Imgcodecs.imwrite(r2File, r1);*/
			
			Core.add(dest, r2, dest);
			
			//find those blue black color
			Mat bl = new Mat();
			Core.inRange(hsvImg, blueMin, blueMax, bl);
			/*String blFile = wkFolder + FILE_SPRT + "BL_" + imgFile.getName();
			Imgcodecs.imwrite(blFile, bl);*/
			
			Core.add(dest, bl, dest);
			
			//find those additional color
			Mat ad1 = new Mat();
			Core.inRange(hsvImg, addMin1, addMax1, ad1);
			/*String adFile = wkFolder + FILE_SPRT + "AD_" + imgFile.getName();
			Imgcodecs.imwrite(adFile, ad);*/
			
			Core.add(dest, ad1, dest);
			
			//find those additional color
			Mat ad2 = new Mat();
			Core.inRange(hsvImg, addMin2, addMax2, ad2);
			/*String adFile = wkFolder + FILE_SPRT + "AD_" + imgFile.getName();
			Imgcodecs.imwrite(adFile, ad);*/
			
			Core.add(dest, ad2, dest);
			
			//Make all pixel grey pixel to exact black
			Imgproc.threshold(dest, dest, 254, 255, Imgproc.THRESH_BINARY_INV);

			writeLog("Saving image to processed folder...", LogLevel.INFO);
			String destFile = wkFolder + FILE_SPRT + imgFile.getName();
			Imgcodecs.imwrite(destFile, dest);

		}

		return wkFolder;

	}

	private void combinesImgsToPDF(File file){

		writeLog("Combine images to PDF", LogLevel.INFO);

		PDDocument document = new PDDocument();
		String fileName = file.getName();

		File[] procFiles = file.listFiles();

		sortFilesByModifiedDate(procFiles);

		try {
			for(File procfile: procFiles){
				BufferedImage bimg = ImageIO.read(procfile);
				float width = bimg.getWidth();
				float height = bimg.getHeight();
				PDPage page = new PDPage(new PDRectangle(width, height));
				document.addPage(page);
				PDImageXObject img = PDImageXObject.createFromFile(procfile.getAbsolutePath(), document);
				PDPageContentStream contentStream = new PDPageContentStream(document, page);
				contentStream.drawImage(img, 0, 0);
				contentStream.close();
			}

			writeLog("Saving PDF to result folder...", LogLevel.INFO);
			document.save(this.resultFolder + FILE_SPRT + fileName + PDF_FILE_EXT);
			document.close();

		}catch (IOException e) {
			writeLog("Errors occur when saving PDF", LogLevel.ERROR);
			logger.error(e.getMessage());
			e.printStackTrace();
		}


	}

	private static void sortFilesByModifiedDate(File[] files){

		Arrays.sort(files, 
				new Comparator<File>(){
			public int compare(File f1, File f2)
			{
				return Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
			} 
		});

	}
	
	private void writeLog(String log, LogLevel l){
		
		logArea.append(log + "\n");
		
		switch (l){
			case WARN:
				logger.warn(log);
				break;
			case ERROR:
				logger.error(log);
				break;
			default:
				logger.info(log);
		}
		
	}
	
	private void setAndUpdateFilterVal(){
		
		this.baseBlackMaxVal = (Integer)this.bbSpinner.getValue();
		this.lightBlackMaxVal = (Integer)this.lbSpinner.getValue();
		this.redMaxVal = (Integer)this.redSpinner.getValue();
		this.blueMaxVal = (Integer)this.blueSpinner.getValue();
		this.addMaxVal = (Integer)this.addSpinner.getValue();
		
		blackMin1 = new Scalar(6,0,0);
		blackMax1 = new Scalar(101,255,this.baseBlackMaxVal);

		blackMin2 = new Scalar(136,0,0);
		blackMax2 = new Scalar(164,255,this.baseBlackMaxVal);
		
		lightBlackMin = new Scalar(0,0,0);
		lightBlackMax = new Scalar(0,0,this.lightBlackMaxVal);
		
		redMin1 = new Scalar(165,0,0);
		redMax1 = new Scalar(180,255,this.redMaxVal);
		
		redMin2 = new Scalar(0,0,0);
		redMax2 = new Scalar(5,255,this.redMaxVal);
		
		blueMin = new Scalar(102,0,0);
		blueMax = new Scalar(135,255,this.blueMaxVal);
		
		addMin1 = new Scalar(6,0,this.baseBlackMaxVal);
		addMax1 = new Scalar(101,255,this.addMaxVal);
		
		addMin2 = new Scalar(136,0,this.baseBlackMaxVal);
		addMax2 = new Scalar(164,255,this.addMaxVal);
	}
		
	public static void main(String[] args) throws Exception{

		ExamPaperCleaner epc = new ExamPaperCleaner();
		epc.start();

	}

}