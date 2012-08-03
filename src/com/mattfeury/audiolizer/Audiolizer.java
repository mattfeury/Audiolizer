package com.mattfeury.audiolizer;

import java.io.File;
import java.net.URL;

import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.applet.*;

import com.softsynth.jsyn.*;
/**
 * Write a description of class Audiolizer here.
 * 
 * @author (your name) 
 * @version (a version number or a date)
 */
public class Audiolizer extends Applet implements ActionListener
{
    private Oscillator osc;
    private ImageAnalysis img;
    private ImagePanel p;
    private String default_image = "http://farm5.static.flickr.com/4003/4697492698_f0bb39dc76_b.jpg";
    private String filename = "";
    private Button local, urlsubmit;
    private JTextField text;
    private JLabel load = new JLabel("Loading...");
    private BufferedImage logo;
    private Panel file;
    private boolean url = false;
    public boolean main = false;
        
    public void start()  
    {
        try {
           // logo = ImageIO.read(new URL("/audiolizer/logo_small.png"));
        } catch(Exception e) {
            System.out.println("no logo, sorry");
        }
        setLayout( new BorderLayout() );       
        
        
        file = new Panel();
        file.setPreferredSize(new Dimension(600,200));
        file.setLayout(new GridLayout());
        
        //local = new Button("From the rock");
        //local.addActionListener(this);
        //file.add(local);
        
        //file.add(new JLabel(""));
        //file.add(new ImagePanel(logo,null));
        String filename = "";
        if(main)
            filename = default_image;
        else
            filename = this.getParameter("url");
        
        System.out.println("first: "+filename);
        boolean url = true;
        beginAudiolizer(filename);
        /*text = new JTextField(,50);
        file.add(text);
        
        urlsubmit = new Button("From the cloud");
        urlsubmit.addActionListener(this);
        urlsubmit.setPreferredSize(new Dimension(100,100));
        file.add(urlsubmit);
        
        add("North",file);
        */
        setSize(new Dimension(1200,600));
        getParent().validate();
        getToolkit().sync();
    }
    
    public void beginAudiolizer(String file)
    {   
        add(load);
        //picture
        System.out.println(file);
        if(ImageAnalysis.loadImage(file,true)) {
            System.out.println("Audiolizer using "+file+". Let go!");
        } else {
            if(!ImageAnalysis.loadImage(default_image,true)) {
               add(new JLabel("sorry. i blew it.")); //rut roh.
               return;
            }
            else {
               System.out.println("Using default image: "+default_image); 
            }
        }
            
        ImageAnalysis.generateStatistics();
        
        int[] tempo = getTempo(ImageAnalysis.brightness);
        System.out.println("Temp: "+ImageAnalysis.temperature+" to Scale: "+generateScale(ImageAnalysis.temperature));
        System.out.println("Brightness: "+ImageAnalysis.brightness+" to tempo: measure: "+tempo[0]+" & #notes: "+tempo[1]);
        System.out.println("Color: "+ImageAnalysis.averageColor.toString());//+" to Scale: "+generateScale(ImageAnalysis.temperature));
        
        osc = new Oscillator(generateScale(ImageAnalysis.temperature),generateKey(ImageAnalysis.averageColor),tempo[0],tempo[1]);
        p = new ImagePanel(ImageAnalysis.image, osc);
        
        remove(load);
        add("West",osc);
        //p.setPreferredSize(new Dimension(ImageAnalysis.image.getWidth()+400,600));
        add("Center",p);
    }
    
    public void stop()
    {
        osc.stop();
        removeAll();
    }
    
    public String getAvgColor()
    {
        Color c = ImageAnalysis.averageColor;
        if(c==null)
            return null;
        
        return Integer.toHexString( c.getRGB() & 0x00ffffff );
        
    }
    
    public void actionPerformed(ActionEvent e) {
        remove(file);
        
        if(e.getSource().equals(local)) {
                JFileChooser fileopen = new JFileChooser();
                //FileFilter filter = new FileNameExtensionFilter("image files", ".jpg, .gif, .png, .jpeg");
                //fileopen.addChoosableFileFilter(filter);
                int ret = fileopen.showDialog(null, "Open file");
            
                if (ret == JFileChooser.APPROVE_OPTION) {
                    filename = fileopen.getSelectedFile().getPath();
                } else {
                    start();
                    return;
                }
                
                beginAudiolizer(filename);
        }
        if(e.getSource().equals(urlsubmit)) {
                filename = text.getText();
                url = true;
                beginAudiolizer(filename);
        }
        
        getParent().validate();
        getToolkit().sync();
    }
    
    public Oscillator.Scale generateScale(double temperature)
    {
        int t = Math.round((float)temperature);
        
        System.out.println((float)temperature + " is rounded to   " + t);
        
        Oscillator.Scale s;
        switch(t)
        {
            case -1:
                s = Oscillator.Scale.blues;
                break;
            case 0:
                s = Oscillator.Scale.min;
                break;
            default:
                s = Oscillator.Scale.maj;
        }
        
        return s;
    }
    
    public int[] getTempo(double brightness)
    {
        int[] tempo = new int[2];  
        
        int b = Math.round((float)brightness);
        
        //goes from 0 - 4
        switch(b)
        {
            case 0:
                tempo[0] = Oscillator.gen.nextInt(2001)+6000;   //measure length in ms. 6000-8000
                tempo[1] = 4;                                   //number of notes per measure. {4}
                break;
            case 1:
                tempo[0] = Oscillator.gen.nextInt(1001)+5000;   //measure length in ms. 5000-6000
                tempo[1] = (Oscillator.gen.nextInt(2)+1)*4;     //number of notes per measure. {4,8}
                break;
            case 2:
                tempo[0] = Oscillator.gen.nextInt(1001)+4000;   //measure length in ms. 4000-5000
                tempo[1] = (Oscillator.gen.nextInt(2)+2)*4;     //number of notes per measure. {8,12}
                break;
            case 3:
                tempo[0] = Oscillator.gen.nextInt(3001)+1000;   //measure length in ms. 1000-4000
                tempo[1] = (Oscillator.gen.nextInt(2)+3)*4;     //number of notes per measure. {12,16}
                break;        
            default:
                tempo[0] = Oscillator.gen.nextInt(1001)+1000;   //measure length in ms. 1000-2000
                tempo[1] = 16;     //number of notes per measure. {16}
                break;  
        }
        
        return tempo;
    }
    
    
    public static void main(String args[])
    {
       Audiolizer  applet = new Audiolizer();
       applet.main = true;
       AppletFrame frame = new AppletFrame("Audiolizer 0.9", applet);
       frame.resize(1200,600);
       frame.show();
       frame.test();
       
    }
    
    public String getAverageColor()
    {
        if(ImageAnalysis.averageColor==null)
            return null;
        else
            return Integer.toHexString( ImageAnalysis.averageColor.getRGB() & 0x00ffffff );
    }
    

    public int generateKey(Color c)
    {
        //this is very primitive
        
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        
        //create a value from 0 - 7
        int total = Math.round((float)((double)(r+g+b) / (255*3) * 7));
        
        System.out.println(total + " +48 is key of  " + Oscillator.freqmap[48+total]);
        return Oscillator.freqmap[48+total];
    }
  
}
