package orders;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.awt.print.PrinterJob;
import javax.swing.*;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.print.PrintService;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.text.DefaultEditorKit;
import java.io.*;
import java.io.File;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.net.URL;
import java.net.URLConnection;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.print.PrintException;
import javax.print.PrintServiceLookup;
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime; 
import java.awt.FontMetrics;

class printIt implements Printable {
	public static Boolean cfgCreated;
	public static String selPrinter;
	public static String orderUri;
	public static String secToken;
	public static String refTime;
	public static String fontName;
	public static String fontSize;
	public static JFrame frame;
	public static JLabel ilabel;
	public static Boolean getOrd;
	public static ScheduledExecutorService ses;
	public static Double pWidth;
	public static Double pHeight;
	public static Double prWidth;
	public static Double prHeight;
	public static Integer leftMargin;
	public static Integer rightMargin;
	public static Integer topMargin;
	public static Integer bottomMargin;
	public static JButton btRun = null;
	public static java.util.List<String> textPrint = new ArrayList<String>();
	int[] pageBreaks;
	java.util.List<String> textLines = new ArrayList<String>();
	
	public int print(Graphics g, PageFormat pf, int pageIndex) throws PrinterException {
	    Graphics2D g2d = (Graphics2D)g;
	    g2d.translate(pf.getImageableX(), pf.getImageableY());
	    Font font = new Font(fontName, Font.BOLD, Integer.parseInt(fontSize));
	    g2d.setFont(font);
	    FontMetrics metrics = g2d.getFontMetrics(font);
	    int lineHeight = metrics.getHeight();
	    int strCnt = 0;
	    textLines.clear();
	    Integer pLen = (int)Math.floor(prWidth);
	    for (int x = 0; x < textPrint.size(); x++) {
	    	String line = textPrint.get(x).trim();
	    	Integer txtW = metrics.stringWidth(line);
	    	if (txtW > pLen) {
	    		Integer ratio = (int)Math.floor(txtW/pLen);
	    		if (ratio * pLen < txtW) {
	    			ratio++;
	    		}
	    		Integer strLen = line.length();
	    		Integer strPart = (int)Math.floor(strLen / ratio);
	    		int stop = 0;
	    		for (int w = 0; w <= ratio; w++) {
	    			int start = stop;
	    			stop = start + strPart;
	    			if (stop > strLen) {
	    				stop = strLen;
	    			}
	    			textLines.add(line.substring(start, stop));
	    			Integer lH = (int)Math.ceil(strCnt * lineHeight);
	    			strCnt++;
	    		}
	    	} else {
	    		textLines.add(line);
	    		strCnt++;
	    	}
	    }
	    if (pageBreaks == null) {
            int linesPerPage = (int)(prHeight/lineHeight) - 2;
            int numBreaks = (textLines.size()-1)/linesPerPage;
            pageBreaks = new int[numBreaks];
            for (int b=0; b<numBreaks; b++) {
                pageBreaks[b] = (b+1)*linesPerPage; 
            }
        }
	    if (pageIndex > pageBreaks.length) {
            return NO_SUCH_PAGE;
        }
	    int y = 0; 
        int start = (pageIndex == 0) ? 0 : pageBreaks[pageIndex-1];
        int end   = (pageIndex == pageBreaks.length) ? textLines.size() : pageBreaks[pageIndex];
        for (int line=start; line<end; line++) {
            y += lineHeight;
            System.out.println(textLines.get(line));
            g.drawString(textLines.get(line), leftMargin, topMargin+y);
        }
	    return PAGE_EXISTS;
	  }
	
	public static void getOrders () {
		Integer ordCnt = 0;
		try {        
            URL oracle = new URL(orderUri+"site/get_new_orders?security_token="+secToken); // URL to Parse
            URLConnection yc = oracle.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
            String json;
            String inputLine;
            json = "";
            while ((inputLine = in.readLine()) != null) {           
            	json += inputLine;
            }
            in.close();
            JSONObject obj = new JSONObject(json);
            ordCnt = Integer.parseInt(obj.getJSONObject("status").getString("cnt"));
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss");  
            LocalDateTime now = LocalDateTime.now();  
            String currTime = dtf.format(now);
            if (ordCnt > 0) {
            	ilabel.setText("Nowe zamówienia: "+ordCnt+" ["+currTime+"]");
	            JSONObject orders = obj.getJSONObject("orders_list");
	            JSONArray ordIds = orders.names();
	            java.util.List<String> listOrd = new ArrayList<String>();
	            for (int i = 0; i < ordCnt; i++) {
	            	String ordId = ordIds.getString (i);	            	
	            	listOrd.add(ordId);
	            	PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream("zamowienia/zam_"+ordId+".txt")));
	            	JSONObject order = orders.getJSONObject(ordId);
	            	JSONObject ordDet = order.getJSONObject("ord_det");
	            	JSONObject customer = order.getJSONObject("customer");
	            	JSONObject ordPos = order.getJSONObject("ord_pos");
	            	JSONArray ordPosList = ordPos.names();
	            	String posTxt = "";
	            	for (int j = 0; j < ordPosList.length(); j++) {
	            		String posId = ordPosList.getString (j);
	            		JSONObject pos = ordPos.getJSONObject(posId);
	            		String fType = pos.getString("type");
	            		String fInfo = pos.getString("name");
	            		String fCnt = pos.getString("cnt");
		            	posTxt += fCnt+" x "+fType+": "+fInfo+"\r\n";
	            	}
	            	String custName = customer.getString("name");
	            	String custTel = customer.getString("tel");
	            	String delAddr = ordDet.getString("delivery_addr");
	            	String pMeth = ordDet.getString("payment");
	            	String boxCost = ordDet.getString("box_cost");
	            	String foodCost = ordDet.getString("food_cost");
	            	String deliveryCost = ordDet.getString("delivery_cost");
	            	String ordRem = ordDet.getString("rabat_code") + " " + ordDet.getString("rabat_info") + " " + ordDet.getString("remarks");
	            	Double total = Double.parseDouble(boxCost) + Double.parseDouble(foodCost) + Double.parseDouble(deliveryCost);
	            	String totalCost = String.format("%.2f", total);
	            	out.write("Zam. "+ordId+", "+custName+"\r\n"+custTel+", "+pMeth+"\r\n"+delAddr+"\r\n"+posTxt+"RAZEM: "+totalCost+"\r\n"+ordRem+"\r\n------\r\n");
	            	out.close(); 
	            }
	            String ordList = String.join(",", listOrd);
	            URL oracle2 = new URL(orderUri+"site/orders_fetched?security_token="+secToken+"&orders="+ordList); // URL to Parse
	            URLConnection yc2 = oracle2.openConnection();
	            BufferedReader out = new BufferedReader(new InputStreamReader(yc2.getInputStream()));
	            String answ = "";
	            String inputLine2;
	            while ((inputLine2 = out.readLine()) != null) {           
	            	answ += inputLine2;
	            }
	            out.close();
	            if (answ != "ok") {
	            	ilabel.setText("Problem z oznaczeniem zamówieñ jako pobrane.");
	            }
	            printOrders();
            } else {
            	ilabel.setText("Brak nowych zamówieñ. ["+currTime+"]");
            }
        } catch (FileNotFoundException e) {
        	ilabel.setText("Problem z pobraniem zamówieñ.");
            e.printStackTrace();
        } catch (IOException e) {
        	ilabel.setText("Problem z pobraniem zamówieñ.");
            e.printStackTrace();
        } catch (Exception ex) {
        	ex.printStackTrace();
        }
	}
	
	public static PrintService findPrintService(String printerName) {
        printerName = printerName.toLowerCase();
        PrintService service = null;
        PrintService[] services = PrinterJob.lookupPrintServices();
        for (int index = 0; service == null && index < services.length; index++) {
            if (services[index].getName().toLowerCase().indexOf(printerName) >= 0) {
                service = services[index];
            }
        }
        return service;
    }

	public static Boolean printOrders () {
        String currFile = "";
        File dir = new File("zamowienia");
        File[] files = dir.listFiles();
        if (files.length == 0) {
        	ilabel.setText("Brak plików do wydrukowania.");
        	return true;
        }
        try {
        	ilabel.setText("Drukowanie "+files.length+" plików.");
	        for (File file : files) {
	        	currFile = file.getAbsolutePath();
	            PrintService service = findPrintService(selPrinter);
	            if (service == null) {
	            	return false;
	            }
	            File in2 = new File(currFile);
	            Scanner myReader = new Scanner(in2);
	            textPrint.clear();
	            while (myReader.hasNextLine()) {
	            	textPrint.add(myReader.nextLine());
	            }
	            myReader.close();
	            try {
	            	File fn = new File(currFile);
	            	String fName = fn.getName();
	            	int pos = fName.lastIndexOf(".");
	            	String ordName = fName.substring(0, pos);
	                PrinterJob pjob = PrinterJob.getPrinterJob();
	                pjob.setPrintService(service);
	                PageFormat pageFormat = pjob.defaultPage();
	                Paper p = pageFormat.getPaper();
	                pWidth = p.getWidth();
	                pHeight = p.getHeight();
	                if (pWidth > pHeight) {
	                	pWidth = p.getHeight();
		                pHeight = p.getWidth();
	                }
	                leftMargin = (int)Math.ceil(0.05 * pHeight);
	                rightMargin = (int)Math.ceil(0.05 * pHeight);
	                topMargin = (int)Math.ceil(0.05 * pHeight);
	                bottomMargin = (int)Math.ceil(0.05 * pHeight);
	                prWidth = pWidth - leftMargin - rightMargin;
	                prHeight = pHeight - topMargin - bottomMargin;
	                p.setImageableArea(leftMargin, topMargin, prWidth, prHeight);
	                pageFormat.setOrientation(PageFormat.PORTRAIT);
	                pageFormat.setPaper(p);
	                pjob.setJobName("Druk_zam_nr_"+ordName);
	                pjob.setCopies(1);
	                pjob.setPrintable(new printIt(), pageFormat);
	                pjob.print();
	                String newPath = currFile.replace("zamowienia", "zamowienia_bkp");
	                Files.move(Paths.get(currFile), Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING);
	              } catch (PrinterException pe) {
	            	ilabel.setText(pe.getMessage());
	                pe.printStackTrace();
	              }
	        }
        } catch (NullPointerException ep) {
        	ilabel.setText(ep.getMessage());
        } catch (IOException ie) {
        	ilabel.setText(ie.getMessage());
        }
		return true;
	}
	
	public static void createConfig() {
		cfgCreated = false;
		File f = new File("config.ini");
		if(f.exists()) {
			cfgCreated = true;
			try {
				File file = new File("config.ini");
				Scanner input = new Scanner(file);
				java.util.List<String> listCfg = new ArrayList<String>();
				while (input.hasNextLine()) {
					listCfg.add(input.nextLine());
				}
				selPrinter = listCfg.get(0).trim();
				orderUri = listCfg.get(1).trim();
				secToken = listCfg.get(2).trim();
				refTime = listCfg.get(3).trim();
				fontName = listCfg.get(4).trim();
				fontSize = listCfg.get(5).trim();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		} else {
			selPrinter = "";
			orderUri = "";
			secToken = "";
			refTime = "2";
			fontName = "Serif";
			fontSize = "9";
		}
		
		JFrame cframe = new JFrame("Konfiguracja drukarki");
		JPanel cpanel = (JPanel) cframe.getContentPane();
	    cpanel.setLayout(null);
	    
	    JLabel c1label = new JLabel("Adres strony z zamówieniami: ");
        c1label.setFont(new Font("Courier", Font.BOLD, 16));
        cpanel.add(c1label);
        Dimension c1size = c1label.getPreferredSize();
        c1label.setBounds(20, 20, c1size.width, c1size.height);
        
        JTextField textField1 = new JTextField(60);
        cpanel.add(textField1);
        textField1.setText(orderUri);
        textField1.setBounds(40 + c1size.width, 20, 400, (c1size.height + 4));
        
        JPopupMenu popupMenu1 = new JPopupMenu();
        Action pasteAction1 = new DefaultEditorKit.PasteAction();
        pasteAction1.putValue(Action.NAME, "Wklej");
        popupMenu1.add(pasteAction1);
        textField1.setComponentPopupMenu(popupMenu1);
        
        JLabel c2label = new JLabel("Token bezpieczeñstwa: ");
        c2label.setFont(new Font("Courier", Font.BOLD, 16));
        cpanel.add(c2label);
        Dimension c2size = c2label.getPreferredSize();
        c2label.setBounds(20, 50, c2size.width, c2size.height);
        
        JTextField textField2 = new JTextField(40);
        cpanel.add(textField2);
        textField2.setText(secToken);
        textField2.setBounds(40 + c1size.width, 50, 200, (c2size.height + 4));
        
        JPopupMenu popupMenu2 = new JPopupMenu();
        Action pasteAction2 = new DefaultEditorKit.PasteAction();
        pasteAction2.putValue(Action.NAME, "Wklej");
        popupMenu2.add(pasteAction2);
        textField2.setComponentPopupMenu(popupMenu2);
	    
        JLabel c3label = new JLabel("Odœwie¿anie co [min]: ");
        c3label.setFont(new Font("Courier", Font.BOLD, 16));
        cpanel.add(c3label);
        Dimension c3size = c3label.getPreferredSize();
        c3label.setBounds(20, 80, c3size.width, c3size.height);
        
        JTextField textField3 = new JTextField(20);
        cpanel.add(textField3);
        textField3.setText(refTime);
        textField3.setBounds(40 + c1size.width, 80, 100, (c3size.height + 4));
        
        JLabel c4label = new JLabel("Czcionka: ");
        c4label.setFont(new Font("Courier", Font.BOLD, 16));
        cpanel.add(c4label);
        Dimension c4size = c4label.getPreferredSize();
        c4label.setBounds(20, 110, c4size.width, c4size.height);
	 
		String fontNames[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		JComboBox fontList = new JComboBox(fontNames);
		cpanel.add(fontList);
		fontList.getModel().setSelectedItem(fontName);;
		fontList.setBounds(40 + c1size.width, 110, 250, (c4size.height + 4));
		
		String fontSizes[] = {"7", "8","9","10","11","12"};
		JComboBox sizeList = new JComboBox(fontSizes);
		cpanel.add(sizeList);
		sizeList.getModel().setSelectedItem(fontSize);
		sizeList.setBounds(300 + c1size.width, 110, 50, (c4size.height + 4));
		
	    JLabel clabel = new JLabel("Wybierz drukarkê, której chcesz u¿ywaæ do drukowania zamówieñ.");
        clabel.setFont(new Font("Courier", Font.BOLD, 16));
        cpanel.add(clabel);
        Dimension csize = clabel.getPreferredSize();
        clabel.setBounds(20, 160, csize.width, csize.height);
        
        if (selPrinter == "") {
        	selPrinter = PrintServiceLookup.lookupDefaultPrintService().getName();
        }
        PrinterJob pj = PrinterJob.getPrinterJob();
		PrintService[] services = pj.lookupPrintServices();
		String testPrinter = "";
		Integer printCnt = 0;
		ArrayList<String> printerList = new ArrayList<String>();
		for (PrintService ps:services){
		    String pName = ps.getName();
		    printerList.add(pName);
		    printCnt++;
		}
		JRadioButton[] radiobutton = new JRadioButton[printCnt];
		ButtonGroup bgroup = new ButtonGroup();
		for (int i=0; i < printCnt; i++) {
			String prName = printerList.get(i);
			JRadioButton printerBtn = new JRadioButton(prName);
			if (prName.contentEquals(selPrinter)) {
				printerBtn.setSelected(true);
			}
			printerBtn.addActionListener(new ActionListener() {
			    @Override
			    public void actionPerformed(ActionEvent event) {
			    	selPrinter = prName;
			    }
			});
			bgroup.add(printerBtn);
			cpanel.add(printerBtn);
			Dimension btsize = printerBtn.getPreferredSize();
			printerBtn.setBounds(20, (200+25*i), btsize.width, btsize.height);
		}
		JButton btSave = new JButton("Zapisz ustawienia");
		btSave.setBounds(560,500,200,30);  
		btSave.addActionListener(new ActionListener() {
		    @Override
		    public void actionPerformed(ActionEvent e) {
	    	 try {
	    		 orderUri = textField1.getText();
	    		 secToken = textField2.getText();
	    		 refTime = textField3.getText();
	    		 fontName = (String) fontList.getSelectedItem();
	    		 fontSize = (String) sizeList.getSelectedItem();	    		
	    	     FileWriter myWriter = new FileWriter("config.ini");
	    	     myWriter.write(selPrinter+"\n");
	    	     myWriter.write(orderUri+"\n");
	    	     myWriter.write(secToken+"\n");
	    	     myWriter.write(refTime+"\n");
	    	     myWriter.write(fontName+"\n");
	    	     myWriter.write(fontSize+"\n");
	    	     myWriter.close();
	    	    } catch (IOException ex) {
	    	      System.out.println("An error occurred.");
	    	      ex.printStackTrace();
	    	    }
		        cfgCreated = true;
		        cframe.dispose();
		        frame.setVisible(true);
		    }
		});
		cframe.add(btSave);
        cframe.setSize(800, 600);
        cframe.setVisible(true);
	}
	
	public static void main(String[] args) throws PrintException, IOException {
		getOrd = false;
		frame = new JFrame("Drukowanie nadchodz¹cych zamówieñ");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = (JPanel) frame.getContentPane();
	    panel.setLayout(null);
        frame.setSize(420, 240);
        JButton btCfg = new JButton("Zmieñ ustawienia");
        frame.add(btCfg);
		btCfg.setBounds(20,20,360,30);  
		btCfg.addActionListener(new ActionListener() {
			@Override
		    public void actionPerformed(ActionEvent e) {
				createConfig();
			}
		});
        btRun = new JButton("W³¹cz sprawdzanie zamówieñ");
        frame.add(btRun);
		btRun.setBounds(20,60,360,30);  
		btRun.addActionListener(new ActionListener() {
			@Override
		    public void actionPerformed(ActionEvent e) {
				if (getOrd) {
					btRun.setForeground(Color.BLACK);
					btRun.setText("W³¹cz sprawdzanie zamówieñ");
					ilabel.setText("Sprawdzanie zatrzymane");
					getOrd = false;
					ses.shutdown();
				} else {
					btRun.setForeground(Color.RED);
					btRun.setText("Wy³¹cz sprawdzanie zamówieñ");
					ilabel.setText("Sprawdzanie co "+refTime+" minut");
					getOrd = true;
					ses = Executors.newSingleThreadScheduledExecutor();
					Integer minRun = Integer.parseInt(refTime);
					ses.scheduleWithFixedDelay(new Runnable() {
			            @Override
			            public void run() {
			            	getOrders();
			            }
			        }, 0, minRun, TimeUnit.MINUTES);
				}
			}
		});
		JButton btTest = new JButton("Drukowanie testowe/awaryjne");
        frame.add(btTest);
		btTest.setBounds(20,100,360,30);  
		btTest.addActionListener(new ActionListener() {
			@Override
		    public void actionPerformed(ActionEvent e) {
				printOrders();
			}
		});
		ilabel = new JLabel("");
        ilabel.setFont(new Font("Courier", Font.BOLD, 16));
        frame.add(ilabel);
        ilabel.setBounds(20, 140, 360, 30);
        File d = new File("zamowienia");
		if(!d.exists()) { 
			d.mkdir();
		}
        File dt = new File("zamowienia_bkp");
		if(!dt.exists()) { 
			dt.mkdir();
		}
		File f = new File("config.ini");
		if(!f.exists()) { 
			createConfig();
		} else {
			cfgCreated = true;
			try {
				File file = new File("config.ini");
				Scanner input = new Scanner(file);
				java.util.List<String> listCfg = new ArrayList<String>();
				while (input.hasNextLine()) {
					listCfg.add(input.nextLine());
				}
				input.close();
				selPrinter = listCfg.get(0).trim();
				orderUri = listCfg.get(1).trim();
				secToken = listCfg.get(2).trim();
				refTime = listCfg.get(3).trim();
				fontName = listCfg.get(4).trim();
				fontSize = listCfg.get(5).trim();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			frame.setVisible(true);
		}
	}
}