package mastery.cleaner;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
	private static final String FILE_SPRT = "//";
	private static final String ROOT_FOLDER = C_DRIVER + "\\ExamPaperCleaner";
	private static final String LIB_FOLDER = ROOT_FOLDER + "\\lib";
	private static final String RAW_FOLDER = ROOT_FOLDER + "\\raw";
	private static final String IMG_FOLDER = ROOT_FOLDER + "\\image";
	private static final String PROC_FOLDER = ROOT_FOLDER + "\\processed";
	private static final String RESULT_FOLDER = ROOT_FOLDER + "\\result";
	private static final String OPENCV_DLL = "opencv_java342.dll";
	private static final int BLACK_HUE_MAX = 180;
	private static final int BLACK_SAT_MAX = 255;
	private static final int PROG_MIN = 0;
	private static final int PROG_MAX = 100;
	private static enum LogLevel {INFO, WARN, ERROR};

	private File rootFolder;
	private File libFolder;
	private File rawFolder;
	private File imgFolder;
	private File procFolder;
	private File resultFolder;
	private int blackValue = 127;
	private Scalar blackMin = new Scalar(0,0,0);
	private Scalar blackMax = new Scalar(BLACK_HUE_MAX,BLACK_SAT_MAX,blackValue);
	private cleanHelper ch = new cleanHelper();

	private JFrame frame = new JFrame();
	private JTextArea logArea = new JTextArea();
	private JProgressBar progressBar = new JProgressBar();
	private JButton startButton = new JButton("Start");
	private JSpinner spinner = new JSpinner(new SpinnerNumberModel(127, 0, 255, 1));
		
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
		frame.setTitle("Exam Paper Cleaner v1.0");
		frame.setSize(800, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JPanel insPanel = new JPanel();
		insPanel.setBorder(new EmptyBorder(0,5,0,5));
		insPanel.setLayout(new GridLayout(7,1));
		
		JLabel ins0 = new JLabel("Instruction:");
		JLabel ins1 = new JLabel("1. Put raw files under " + RAW_FOLDER + ".");
		JLabel ins2 = new JLabel("2. Select black threshold, this value control what kind of black pixels will be removed.");
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
		
		JPanel funcPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		JLabel funcLabel1 = new JLabel("Black Threshold");
		
		spinner.setValue(this.blackValue);
		
		JLabel funcLabel2 = new JLabel("[0-255]");
		startButton.setEnabled(false);
		startButton.addActionListener(new ActionListener() {
	         public void actionPerformed(ActionEvent e) {
	            setBlackValue((Integer) spinner.getValue());
	            ch.execute();
	         }          
	      });
		
		funcPanel.add(funcLabel1);
		funcPanel.add(spinner);
		funcPanel.add(funcLabel2);
		funcPanel.add(startButton);
		
		frame.add(funcPanel, BorderLayout.NORTH);
		
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

		File fileOut = new File(LIB_FOLDER + FILE_SPRT + OPENCV_DLL);

		if(!fileOut.exists()){
			try {
				InputStream inputStream = this.getClass().getResourceAsStream("/"+OPENCV_DLL);

				if (fileOut != null && inputStream!=null) {
					OutputStream outputStream = new FileOutputStream(fileOut);
					byte[] buffer = new byte[1024];
					int length;

					while ((length = inputStream.read(buffer)) > 0) {
						outputStream.write(buffer, 0, length);
					}

					inputStream.close();
					outputStream.close();

				}else{
					throw new Exception("Library load file fail");
				}
			} catch (IOException e) {
				logger.error(e.getMessage());
				e.printStackTrace();
				throw e;
			}
		}
		
		System.load(fileOut.getAbsolutePath());
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
	
	private class cleanHelper extends SwingWorker<Void, Void>{

		@Override
		protected Void doInBackground() throws Exception {
			
			startButton.setEnabled(false);
			
			deteleFileInFolders();
			
			writeLog("Note Eraser Start... ", LogLevel.INFO);
			
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
				Runtime.getRuntime().exec("explorer.exe /select, C:\\NoteEraser\\result\\");
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

			//remove those purple color
			writeLog("Applying Black Filter...", LogLevel.INFO);
			Mat dest = new Mat();
			Core.inRange(hsvImg, blackMin, blackMax, dest);
						
			//Threshold the filter and make those red and blue pixel set to 120
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

	public int getBlackValue() {
		return blackValue;
	}

	private void setBlackValue(int blackValue) {

		if(blackValue>=0&&blackValue<=255){
			this.blackValue = blackValue;
			blackMax = new Scalar(BLACK_HUE_MAX,BLACK_SAT_MAX, this.blackValue);
		}else{
			writeLog("Black Value must be >= 0 and <= 255", LogLevel.ERROR);
			this.blackValue = 127;
			spinner.setValue(this.blackValue);
		}

	}

	public static void main(String[] args) throws Exception{

		ExamPaperCleaner epc = new ExamPaperCleaner();
		epc.start();

	}

}
