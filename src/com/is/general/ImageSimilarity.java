/**
 * 
 */
package com.is.general;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.is.algorithm.Compare;
import com.is.utils.Check;
import com.is.utils.ISLogger;
import com.is.utils.ImageHolder;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * @author Grzegorz Polek <grzegorz.polek@gmail.com>
 *
 */
public class ImageSimilarity {

	private final static Logger LOG = Logger.getLogger(ImageSimilarity.class .getName()); 
	
	@Parameter
	private List<String> parameters = new ArrayList<String>();
	
	@Parameter(names = {"-h", "--help"}, description = "Show help")
	private boolean help = false;
	
	@Parameter(names = {"-g", "--gui"}, description = "Enable Graphical User Interface")
	private boolean gui = false;
	
	@Parameter(names = {"-i", "--img"}, description = "Absolute Path to the image")
	private String img;

	@Parameter(names = {"-d", "--dir"}, description = "Absolute Path to directory with images to compare with")
	private String dir;
	
	@Parameter(names = {"-t", "--threads"}, description = "Number of threads")
	private int threads;

	private static JCommander jc;
	
    /**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws RuntimeException, Exception 
	{
		ImageSimilarity is = new ImageSimilarity();
		
		jc = new JCommander(is);
		jc.setProgramName("ImageSimilarity");
		
		try
		{
			jc.parse(args);
		} 
		catch (ParameterException pe) 
		{
			System.out.println("Wrong console parameters. See usage of ImageSimilarity below:");
			jc.usage();
		}
		
		is.setup();
	}
	
	private void setup()
	{
		// Show help
		if(help)
		{
			jc.usage();
		}
		
		// Disable MediaLib. lol.
		System.setProperty("com.sun.media.jai.disableMediaLib", "true");
		
		// GUI enabled
		if(gui)
		{
			try {
				UserInterface ui = new UserInterface();
				ui.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(threads == 0)
		{
			threads = 1;
		}
		
		// Set up the logger
		try {
			ISLogger.setup();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	throw new RuntimeException("Problems with creating the log files!");
	    }
		
		// Run
		if(img != null && dir != null)
		{
			run();
		}
		else
		{
			throw new RuntimeException("Comparision failed. Ooops!");
		}
	}
	
	private void run()
	{
		if(Check.url(img))
		{
			if(!img.toLowerCase().endsWith(".jpeg") && !img.toLowerCase().endsWith(".jpg"))
			{
				throw new RuntimeException("ImageSimilarity accepts only JPEG files!");
			}
			
			URL url;
			
			try {
				url = new URL(img);
				
				HttpURLConnection huc;
				
				try {
					huc = ( HttpURLConnection )  url.openConnection ();
					huc.setRequestMethod ("GET");  //OR  huc.setRequestMethod ("HEAD"); 
					
					huc.connect () ; 
					int code = huc.getResponseCode() ;

					if(code < 200 || code > 300) // Accept all 2XX codes.
					{
						throw new RuntimeException("URL " + img + " returned " + code + " http error code!");
					}
					
				} catch (IOException e1) {
					throw new RuntimeException("Error occurred with testing connection for " + img);
				} 
				
				InputStream in;
				ByteArrayOutputStream out;
				
				try {
					in = new BufferedInputStream(url.openStream());
					
					out = new ByteArrayOutputStream();
					
					byte[] buf = new byte[1024];
					int n = 0;
					while (-1!=(n=in.read(buf)))
					{
					   out.write(buf, 0, n);
					}
					
					out.close();
					in.close();
					byte[] response = out.toByteArray();
					
					// Creates temporary image file
					File tf = File.createTempFile("image-similarity-temp", ".jpg");
					
					// Deletes file when the virtual machine terminate
			        tf.deleteOnExit();
					
			        img = tf.getAbsolutePath();
			        
					FileOutputStream fos = new FileOutputStream(img);
					
				    fos.write(response);
				    fos.close();
				} catch (IOException e) {
					throw new RuntimeException("Error occurred with donwloading a file form URL " + img);
				}
			} catch (MalformedURLException e1) {
				throw new RuntimeException("Wrong URL " + img);
			}
		}
		
		File image = new File(img);
		File directory = new File(dir);
		
		if(!image.exists())
		{
			throw new RuntimeException("Image doesn't exist!");
		}
		
		if(!directory.exists())
		{
			throw new RuntimeException("Directory doesn't exist!");
		}
		
		// TODO: Think about multithreading here, how we can implement it...
		
		DecimalFormat df = new DecimalFormat("#.##");
		
		// Compare images
		try {
			
			Compare compare = new Compare(image, directory, threads);
			
			List<ImageHolder> images = compare.getResults();
			for(ImageHolder i : images)
			{
				System.out.println("Image: " + i.getFile().getPath());
				System.out.println("Distance: " + df.format(i.getDistance()));
				System.out.println("Difference: " + df.format(i.getDifference()) + "%");
				System.out.println("Similarity: " + df.format(i.getSimilarity()) + "%");
				System.out.println("\n");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			LOG.severe("Comparision failed. Ooops!");
			throw new RuntimeException("Comparision failed. Ooops!");
		}
	}

}
