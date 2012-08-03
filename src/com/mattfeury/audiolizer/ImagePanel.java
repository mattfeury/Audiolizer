package com.mattfeury.audiolizer;

import java.awt.event.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.swing.*;
import java.awt.geom.*;
import java.awt.image.*;

class ImagePanel extends JPanel implements MouseListener {
    private BufferedImage image;
    private Oscillator osc;
    private double scale = 1.0;
    private int tilesize = 20;
    private int ticks = 0;

    ImagePanel(BufferedImage image, Oscillator osc) {
        this.osc = osc;
        this.image = image;
        setLayout(new FlowLayout(FlowLayout.RIGHT));
        setPreferredSize(new Dimension(500,300));
        addMouseListener(this);
        setFocusable(true);
        
        double w = (double)image.getWidth();
        double h = (double)image.getHeight();
        
        if(w>600 && w>h)
            scale = 600/w;
        
        if(h>600 && h>w)
            scale = 600/h;
        
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        AffineTransform at =
            AffineTransform.getScaleInstance(scale,scale);
        g2d.drawRenderedImage(image,at);
        //g.drawImage(image, 0, 0, this);
    }
    
     
    /**
     * releasing of the mouse on the canvas.
     *  
     * @param e MouseEvent of the user's doing
     */
    public void mouseReleased(MouseEvent e) {
        Point i = e.getPoint();
        
        int[] rgbs = new int[tilesize*tilesize];
        image.getRGB((int)i.getX()-(int)(tilesize/2),(int)i.getY()-(int)(tilesize/2),tilesize,tilesize,rgbs,0,tilesize);
        
       // for(int k : rgbs)
       //     System.out.println(k);
        
        boolean newbass = false;
        if(ticks % 2 == 0)
            newbass = true;
            
        osc.makeRiff(true,newbass);
        ticks++;
    }
    
    public void mousePressed(MouseEvent e) {   
    }
    
     /**
     * Entering of the mouse on the canvas. Nothing doin'
     *  
     * @param e MouseEvent of the user's doing
     */
    public void mouseEntered(MouseEvent e) {
    }

    /**
     * Exiting of the mouse on the canvas. Nothing doin'
     *  
     * @param e MouseEvent of the user's doing
     */
    public void mouseExited(MouseEvent e) {
    }

    /**
     * Clicking of the mouse on the canvas. Nothing doin'
     *  
     * @param e MouseEvent of the user's doing
     */
    public void mouseClicked(MouseEvent e) {
    }

}
