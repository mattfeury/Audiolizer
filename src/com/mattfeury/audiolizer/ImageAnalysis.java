package com.mattfeury.audiolizer;

import java.awt.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import java.net.URL;

public class ImageAnalysis{

    public static BufferedImage image;

    public static double temperature;
    public static double brightness;
    public static Color averageColor;

    public static boolean loadImage(String name, boolean url){

        //filename = "gStache.jpg"; //DELETE THIS LATER
        
        try{
            if(url)
                image = ImageIO.read(new URL(name));
            else
                image = ImageIO.read(new File(name));
        }

        catch(Exception e){
            System.out.println(e);
            System.out.println(e.getStackTrace());
            return false;

        }

        return true;


    }

    public static void generateStatistics(){
        double totalRed = 0, totalGreen = 0, totalBlue = 0;

        for(int i = 0; i < image.getWidth(); i++){
            for (int j = 0; j < image.getHeight(); j++){

                Color c = new Color (image.getRGB(i, j));

                double r = c.getRed()     / 255.0;
                double g = c.getGreen() / 255.0;
                double b = c.getBlue()     / 255.0;

                totalRed += r;
                totalGreen += g;
                totalBlue += b;

            }

        }

        long numPixels = image.getWidth() * image.getHeight();

        double redRange = totalRed / numPixels;
        double blueRange = totalBlue / numPixels;
        System.out.println(totalRed);
System.out.println(totalBlue);
        temperature = redRange - blueRange;


        int avR = (int)(totalRed / numPixels * 255);
        int avG = (int)(totalGreen / numPixels * 255);
        int avB = (int)(totalBlue / numPixels * 255);

        averageColor = new Color (avR, avG, avB);
        brightness = (float)Math.sqrt((.241 * avR * avR) + (.691 * avG * avG) + (.068 * avB * avB));
        brightness /= 255;
        brightness *= 4;


    }
/*

    public static void main(String args[]){

        if(!loadImage("gay")){
            System.out.println ("failed to load");
            return;
        }
        generateStatistics();

        System.out.println ("average color: R:" + averageColor.getRed() + " G: " + averageColor.getGreen() + " B: " + averageColor.getBlue());
        System.out.println ("Brightness: " + brightness);
        System.out.println ("Temperature: " + temperature);


    }

*/
}
