package com.mattfeury.audiolizer;

import java.util.*;
import java.awt.*;
import java.applet.Applet;
import com.softsynth.jsyn.*;
import com.softsynth.jsyn.view11x.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class Oscillator extends Container implements ActionListener, ChangeListener
{
    public enum Scale {maj, min, harm, blues, whole};
  //  public static MathEvaluator math = new MathEvaluator();
    public static Random gen = new Random();
    //starts on C0
    public static int[] freqmap =  new int[]{16,17,18,20,21,22,23,25,26,28,29,31,
                                             33,35,37,39,41,44,46,49,52,55,58,62,
                                             65,69,73,78,82,87,93,98,104,110,117,124,
                                             131,139,147,156,165,175,185,196,208,220,233,247, //audible sound starts here
                                             262,278,294,311,330,349,370,392,415,440,466,494,
                                             523,554,587,622,659,699,740,784,831,880,932,988,
                                             1047,1109,1175,1245,1319,1397,1475,1568,1661,1760,1865,1976,
                                             2093,2218,2349,2489,2637,2794,2960,3136,3322,3520,3729,3951, //gets annoying here
                                             4186,4435,4699,4978,5274,5588,5920,6272,6645,7040,7459,7902};
                                     
    //music theory
    private final int[] major_ints   = new int[]{2,2,1,2,2,2,1};
    private final int[] minor_ints   = new int[]{2,1,2,2,1,2,2}; //natural minor
    private final int[] blues_ints   = new int[]{3,2,1,1,3,2};
    private final int[] harmonic_ints= new int[]{2,1,2,2,1,3,1};
    private final int[] whole_ints   = new int[]{2,2,2,2,2,2};
    private final int[] interval_weights =  new int[]{1,1,3,3,1,3,2,2,2};
    private final int[] bass_weights     = new int[]{-1,0,2,3,1,3,2,0,-1};
    private final int restfulness = 12; //the riff maker has a one-in-this chance of choosing to rest rather than play
    private final int forgetfulness = 20; //the riff maker has a one-in-this chance of forgetting a random note of the riff.
    
    //for improvising
    private LinkedList<Integer> scale;
    private LinkedList<Integer> chromatic;
    private ArrayList<Integer> riff, bass;
    private int cursor = 0;
    private int bass_cursor = 0;
    private int cursorkey = 0; //starting cursor position
    private int lowfreq = 130;
    private int highfreq = 1000;
    private static int key = 440;
    private int ticks = 0;
    private int measure = 3000; //in ms
    private int riff_size = 16;
    private double note_length = .69 * measure / riff_size; //in ticks
    private int riff_cursor = 0;
    private boolean dissonance = true;
    
    //amplitude
    private double bass_amp = 0.5;
    private double lead_amp = 0.5;
    private double[] bell = {(double)(measure/riff_size)/2000, lead_amp, (double)(measure/riff_size)/2000, 0.0 };
    private double[] rise = {(double)(measure/riff_size)/1000*.7, lead_amp, (double)(measure/riff_size)/1000*.3, 0.0    };
    private double[] fall = {(double)(measure/riff_size)/1000*.3, lead_amp, (double)(measure/riff_size)/1000*.7, 0.0    };
    private double[] inverted_bell = {(double)(measure/riff_size)/2000, 0.0, (double)(measure/riff_size)/2000, lead_amp };
    private double[] adsr = {(double)(measure/riff_size)/1000*.2, lead_amp, (double)(measure/riff_size)/1000*.10, lead_amp*.8,
                               (double)(measure/riff_size)/1000*.5, lead_amp*.8, (double)(measure/riff_size)/1000*.2, 0};
    private double[] warble = {(double)(measure/riff_size)/1000*.1, lead_amp*.5, (double)(measure/riff_size)/1000*.05, lead_amp*.8 };
    private double[] envelope;
    private boolean shouldEnvelope = false;

            
    //jsyn and interface
    private SynthOscillator    sineOsc, bassOsc;
    private LineOut            lineOut; 
    private SynthScope         scope;
    private AddUnit            mixer;
    private EnvelopePlayer     envPlayer, envLow;
    private SynthEnvelope      envData, envBass;
    private JSlider            speed,size,oscSlider,bassSlider;
    private javax.swing.Timer time;
    
    public Oscillator(Scale s, int freq, int ms, int numofnotes)
    {
       scale = new LinkedList<Integer>();
       chromatic = new LinkedList<Integer>();
       riff = new ArrayList<Integer>();
       bass = new ArrayList<Integer>();
       
       for(int i : freqmap)
            chromatic.add(i);
       
       //music
       measure = ms;
       riff_size = numofnotes;
       note_length = .69 * measure / riff_size;
       
       resetEnvelope();
       calculateScale(s, freq);
       setPreferredSize(new Dimension(400,700));
       start();
    }
    
    public static void main(String args[])
    {
       Oscillator  applet = new Oscillator(Scale.maj,440,4000,16);
       /*
       AppletFrame frame = new AppletFrame("Audiolizer Beta", applet);
       frame.resize(900,600);
       frame.show();
       frame.test();
       */
    }
    
    public void resetEnvelope()
    {
        switch(gen.nextInt(5))
        {
            case 0:
                envelope = bell;
                break;
            case 1:
                envelope = rise;
                break;
            case 2:
                envelope = fall;
                break;
            case 3:
                envelope = inverted_bell;
                break;
            default:
                envelope = warble;
        }
    }

 /*
  * Setup synthesis by overriding start() method.
  */
    public void start()  
    {
        time = new javax.swing.Timer(measure,this);
        
        Panel buttonPanel, controlPanel;
        setLayout( new BoxLayout(this,BoxLayout.Y_AXIS) );
        
        try
        {
            Synth.startEngine(0);
            sineOsc = new SineOscillator();
            bassOsc = new SineOscillator();
            lineOut = new LineOut();
            envPlayer = new EnvelopePlayer();
            envLow = new EnvelopePlayer();
            mixer = new AddUnit();

            makeRiff(true,true);
            
            //connect oscillators to mixer
            sineOsc.output.connect( mixer.inputA );
            bassOsc.output.connect( mixer.inputB );
            mixer.output.connect( 0, lineOut.input, 0 );
            mixer.output.connect( 0, lineOut.input, 1 );
            
            // control sine wave amplitude with envelope output
            if(shouldEnvelope) envPlayer.output.connect( sineOsc.amplitude );
            if(shouldEnvelope) envLow.output.connect(bassOsc.amplitude);
            envBass = new SynthEnvelope( adsr );
            envData = new SynthEnvelope( inverted_bell );
            System.out.println("using envelope: "+envelope);
            envPlayer.envelopePort.clear(); // clear the queue
            envPlayer.envelopePort.queueLoop( envData, 0, envelope.length/2 );  // queue attack
            envLow.envelopePort.clear(); // clear the queue
            envLow.envelopePort.queueLoop(envBass, 0, 4 );  // queue attack

            
            //setup initial values and start
            sineOsc.amplitude.set(lead_amp);
            bassOsc.amplitude.set(bass_amp);
            sineOsc.frequency.set(scale.get(0)); //maybe start on cursor here   
            bassOsc.frequency.set(scale.get(0));
            lineOut.start();
            mixer.start();
            envLow.start();
            envPlayer.start();
            sineOsc.start();
            bassOsc.start();
            
            
            //create interface
            add(  scope = new SynthScope() );
            scope.hideControls();
            scope.createProbe( sineOsc.output, "Lead", Color.blue );
            scope.createProbe( bassOsc.output, "Bass", new Color(150,150,0) );
            scope.createProbe( mixer.output, "Mixed", Color.green );
            scope.finish();

            buttonPanel = new Panel();
            buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            buttonPanel.setPreferredSize(new Dimension(400,70));
            add(buttonPanel);
            
            Button a = new Button("Master Improvise");
            a.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    makeRiff(true,true);
                }
            });
            buttonPanel.add( a );
            
            Button b = new Button("Improvise Riff");
            b.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    makeRiff(true,false);
                }
            });
            buttonPanel.add( b );
            
            Button c = new Button("Improvise Bass");
            c.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    makeRiff(false,true);
                }
            });
            buttonPanel.add( c );
            
            Panel faderChannel = new Panel();
            faderChannel.setLayout(new BoxLayout(faderChannel,BoxLayout.X_AXIS));
            add(faderChannel);
            
            //sliders
            oscSlider = new JSlider(JSlider.VERTICAL,0, 10, (int)(10*lead_amp));
            oscSlider.addChangeListener(this);
            oscSlider.setPaintLabels(true);
            Hashtable labelTable = new Hashtable();
            labelTable.put( new Integer( 0 ), new JLabel("0.0") );
            labelTable.put( new Integer( 5 ), new JLabel("0.5") );
            labelTable.put( new Integer( 10 ), new JLabel("1.0") );
            oscSlider.setLabelTable( labelTable );

            //sliders
            bassSlider = new JSlider(JSlider.VERTICAL,0, 10, (int)(10*bass_amp));
            bassSlider.addChangeListener(this);
            bassSlider.setPaintLabels(true);
            Hashtable labelTable2 = new Hashtable();
            labelTable2.put( new Integer( 0 ), new JLabel("0.0") );
            labelTable2.put( new Integer( 5 ), new JLabel("0.5") );
            labelTable2.put( new Integer( 10 ), new JLabel("1.0") );
            bassSlider.setLabelTable( labelTable2 );
            
            faderChannel.add(oscSlider); 
            faderChannel.add(bassSlider);
                   
            faderChannel.add(Box.createHorizontalGlue());
            
            //tempo
            speed = new JSlider(JSlider.VERTICAL,500, 10000, measure);
            speed.addChangeListener(this);
            speed.setMajorTickSpacing(1000);
            speed.setMinorTickSpacing(100);
            speed.setPaintTicks(true);
            speed.setPaintLabels(true);
            faderChannel.add(speed);
            
            //riff size
            size = new JSlider(JSlider.VERTICAL,4, 16, riff_size);
            size.addChangeListener(this);
            size.setMajorTickSpacing(4);
            size.setMinorTickSpacing(2);
            size.setPaintTicks(true);
            size.setPaintLabels(true);
            faderChannel.add(size);
            
            time.start();
       } catch(SynthException e) {
          SynthAlert.showError(this,e);
       }
    }

    public void stop()  
    {
        try
        {
           // riffer.stop();
            time.stop();
            sineOsc.delete();
            bassOsc.delete();
            removeAll(); // remove components from Applet panel.
            Synth.stopEngine();
        } catch(SynthException e) {
            System.out.println("Caught " + e);
        }
    }
    
    public void recalulate()
    {
        note_length = .69 * measure / riff_size;
        
        bell = new double[]{(double)(measure/riff_size)/2000, lead_amp, (double)(measure/riff_size)/2000, 0.0 };
        rise = new double[]{(double)(measure/riff_size)/1000*.7, lead_amp, (double)(measure/riff_size)/1000*.3, 0.0 };
        fall = new double[]{(double)(measure/riff_size)/1000*.3, lead_amp, (double)(measure/riff_size)/1000*.7, 0.0    };
        inverted_bell = new double[]{(double)(measure/riff_size)/2000, 0.0, (double)(measure/riff_size)/2000, lead_amp };
        adsr = new double[]{(double)(measure/riff_size)/1000*.2, lead_amp, (double)(measure/riff_size)/1000*.10, lead_amp*.8,
                               (double)(measure/riff_size)/1000*.5, lead_amp*.8, (double)(measure/riff_size)/1000*.2, 0};
        warble = new double[]{(double)(measure/riff_size)/1000*.1, lead_amp*.5, (double)(measure/riff_size)/1000*.05, lead_amp*.8 };
    
        resetEnvelope();
        if(shouldEnvelope) { 
            envBass.write(0,adsr,0,4);
            envData.write(0,envelope,0,envelope.length/2); 
            envPlayer.envelopePort.clear(); // clear the queue
            envPlayer.envelopePort.queueLoop( envData, 0, envelope.length/2 );  // queue attack
            envLow.envelopePort.clear(); // clear the queue
            envLow.envelopePort.queueLoop(envBass, 0, 4 );  // queue attack
        }
    }
    
    public int getRandomInterval(int[] weights)
    {
        ArrayList<Integer> k = new ArrayList<Integer>();
        
        for(int i = 0; i<weights.length; i++)
        {
            int j = weights[i];
            while(j >= 0)
            {
                k.add(i);
                j--;
            }
        }
        
        int b = k.get(gen.nextInt(k.size())) - (weights.length/2);
        
        return b;
    }
    
    public void calculateScale(Scale s, int note)
    {
        //always major for now
        key = note;
        int low = note;
        
        int[] scaler;
        switch(s)
        {
            case min:
                scaler = minor_ints;
                break;
            case harm:
                scaler = harmonic_ints;
                break;
            case blues:
                scaler = blues_ints;
                break;
            case whole:
                scaler = whole_ints;
                break;
            default: //major
                scaler = major_ints;
        }
        
        while(chromatic.get(chromatic.indexOf(low)-12) > lowfreq)
            low = chromatic.get(chromatic.indexOf(low)-12);
            
        scale.clear();
        int probe = 0;
        int k = 0;
        for(int i = low; k < scaler.length*2+1; probe = (probe+1) % scaler.length)
        {    
            scale.add(i);
            
            int index = chromatic.indexOf(i)+scaler[probe];
            if(index>=chromatic.size())
                break;
                
            i = chromatic.get(index);
            if(i==note) { cursor = k; cursorkey = k; } //set our cursor
            
            k++;
        }
        
        cursorkey = cursor;
        bass_cursor = cursor;
        System.out.println(scale);
        //System.out.println(cursor);
        
    }
    
    public void makeRiff(boolean newLead, boolean newBase)
    {
        if(newLead) riff.clear();
        if(newBase) bass.clear();
        cursor = cursorkey;
        bass_cursor = 0;
        
        for(int i = 0; i < riff_size; i++)    
                improvise(newLead, newBase);
    }
    
    public void improvise(boolean newLead, boolean newBase)
    {
        //lead
        int newfreq = 0;
        int bassfreq = 0;
        int size = scale.size();
        
        if(gen.nextInt(restfulness)!=0 && newLead) {
            //we will play a note
            int v = getRandomInterval(interval_weights) + cursor;
            if(v >= size || v<(size/4)) {
                improvise(newLead,newBase); 
                return;
            }
            
            newfreq = scale.get(v);
            cursor = v;
        }
        
        if(gen.nextInt(restfulness*2)!=0 && newBase) {
            //rock that bass
            int b = getRandomInterval(bass_weights) + bass_cursor;
        
            if(b<0)
                b = gen.nextInt(2);
            if(b>=size)
                b = size-1-gen.nextInt(3);
        
            bassfreq = scale.get(b);
            bass_cursor = b;
        }
        
        if(Math.abs(bass_cursor-cursor)==1 && !dissonance) {
            if(gen.nextInt(2)==0)
                bassfreq = scale.get(++bass_cursor);
            else
                newfreq  = scale.get(--cursor);
        }
        
        if(newLead) riff.add(newfreq);
        if(newBase) bass.add(bassfreq);
    }
    
    public void actionPerformed(ActionEvent e) {
        //new measure
       // System.out.println("new measure");
            
        riff_cursor = 0;
        for(int i=0; i < riff_size; i++)
        {   
            double leadnote = (double)riff.get(i);
            double bassnote = (double)bass.get(i);
            
            if(leadnote<bassnote) {
                riff.set(i,(int)bassnote);
                bass.set(i,(int)leadnote);
                
                double temp = leadnote;
                leadnote = bassnote;
                bassnote = temp;
            }
            
            double lead = (gen.nextInt(forgetfulness)==0) ? 0 : leadnote;
            double low  = (gen.nextInt(forgetfulness*2)==0) ? 0 : bassnote;
            
           // System.out.println("new note: "+lead+" / "+low);
            
            sineOsc.frequency.set((int)(Synth.getTickCount()+i*note_length),lead);   
            bassOsc.frequency.set((int)(Synth.getTickCount()+i*note_length),low);

        }
        
        ticks++;
    }
    
    public void stateChanged(ChangeEvent e) {
        JSlider source = (JSlider)e.getSource();
        if (!source.getValueIsAdjusting() && source.equals(speed)) {
                measure = (int)source.getValue();
                recalulate();
                time.setDelay(measure);
                System.out.println(measure + " " + note_length);
        } if (!source.getValueIsAdjusting() && source.equals(size)) {
                riff_size = (int)source.getValue();
                recalulate();
                makeRiff(true,true);
        } if (!source.getValueIsAdjusting() && source.equals(oscSlider)) {
                lead_amp = source.getValue() / 10.0;
                sineOsc.amplitude.set(lead_amp);
                recalulate();
                
                System.out.println(lead_amp);
        } if (!source.getValueIsAdjusting() && source.equals(bassSlider)) {
                bass_amp = source.getValue() / 10.0;
                bassOsc.amplitude.set(bass_amp);
                recalulate();
        }
    }
}
