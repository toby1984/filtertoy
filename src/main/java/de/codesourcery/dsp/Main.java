package de.codesourcery.dsp;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Main extends JFrame
{
    private static final String SERIES_INPUT = "input";
    private static final String SERIES_SAWTOOTH = "sawtooth";
    private static final String SERIES_PWM = "pwm";
    private static final String SERIES_LOWPASS = "lowpass";
    
    public static final class Config 
    {
        float sineFrequency;
        /**
         * Samples per second
         */
        float sampleRate;
        /**
         * How many seconds worth of samples to generate
         */
        float seconds;
        /**
         * Frequency for changes of the PWM signal.
         */
        float pwmFrequency;
        float lowPassCutoffFrequency;
    }    
    
    private JPanel panel;
    
    public void run() throws UnsupportedAudioFileException, IOException 
    {
        final Config config = new Config();
        config.sineFrequency = 100f;
        config.sampleRate = 44100;
        config.seconds = 0.01f;
        config.pwmFrequency = 2000;
        config.lowPassCutoffFrequency = 50f;
        
        final JPanel topLevel = new JPanel();
        topLevel.setLayout( new GridBagLayout() );
        panel = createPanel(config);
        
        final Runnable redraw = () -> 
        {
            topLevel.remove( panel );
            try {
                panel = createPanel(config);
                topLevel.add( panel, cnstrs(0,0,1,0.5) );
                topLevel.validate();
                repaint();
            } catch (Exception e) {
                e.printStackTrace();
            }            
        };
        
        final JPanel controls = new JPanel();
        controls.setLayout( new GridBagLayout() );
        final int controlCnt = 5;
        final double weightY=1/controlCnt;
        int idx = 0;
        controls.add( createControl("Sine frequency",100,10000, v -> { 
            config.sineFrequency = v;
            redraw.run();
        },config.sineFrequency) , cnstrs(0,idx++,1,weightY ) );
        controls.add( createControl("LWP frequency",10,2000, v -> { 
            config.lowPassCutoffFrequency = v;
            redraw.run();
        },config.lowPassCutoffFrequency) , cnstrs(0,idx++,1,weightY ) );      
        controls.add( createControl("PWM frequency",10,10000, v -> { 
            config.pwmFrequency = v;
            redraw.run();
        },config.pwmFrequency) , cnstrs(0,idx++,1,weightY ) );          
        controls.add( createControl("Sample rate",4000,44100, v -> { 
            config.sampleRate = v;
            redraw.run();
        }, config.sampleRate ) , cnstrs(0,idx++,1,weightY ) );         
        controls.add( createControl("Time",0.001f,0.1f, v -> { 
            config.seconds= v;
            redraw.run();
        },config.seconds) , cnstrs(0,idx++,1,weightY ) );             
        
        topLevel.add( panel, cnstrs(0,0,1,0.5));
        topLevel.add( controls, cnstrs(0,1,1,0.5));
        getContentPane().add( topLevel );
    }
    
    private JPanel createControl(String label,float min,float max,Consumer<Float> consumer,float initialValue) 
    {
        final JTextField[] textArray = {null}; 
        
        final ChangeListener[] sliderListener = {null}; 
        final ActionListener[] textListener = {null};

        final float perc2 = (int) (100*((initialValue - min)/(max-min)));
        final JSlider slider = new JSlider(0,100);
        final JTextField text = new JTextField(Double.toString(min));
        
        slider.setValue( (int) perc2);
        initialValue = min+(slider.getValue()/100f)*(max-min);
        sliderListener[0]= ev -> 
        {
            text.removeActionListener( textListener[0] );
            float v = min+(slider.getValue()/100f)*(max-min);
            textArray[0].setText( Float.toString( v ) );
            text.addActionListener( textListener[0] );
            consumer.accept(v);
        };
        slider.addChangeListener(sliderListener[0]);

        // text field
        text.setMaximumSize( new Dimension(150,20 ) );
        text.setText( Float.toString( initialValue ) );
        textArray[0]=text;
        
        final ActionListener listener = ev -> 
        {
            slider.removeChangeListener( sliderListener[0] );
            float v = Float.parseFloat( text.getText().trim() );
            final float perc = 100*((v - min)/(max-min));
            slider.setValue( (int) perc );
            slider.addChangeListener( sliderListener[0] );
            consumer.accept(v);
        };
        textListener[0] = listener;
        text.addActionListener( listener);
        
        final JPanel result = new JPanel();
        result.setLayout( new GridBagLayout() );
        JLabel jlabel = new JLabel(label);
        jlabel.setMinimumSize( new Dimension(150,20 ) );
        jlabel.setMaximumSize( new Dimension(150,20 ) );
        result.add( jlabel , cnstrs(0,0,0.0,1 ) );
        result.add( text , cnstrs(1,0,0.2,1 ) );
        result.add( slider , cnstrs(2,0,0.8,1 ) );
        return result;
    }
    
    private JPanel createPanel(Config config) throws UnsupportedAudioFileException, IOException {
        
        // final XYSeriesCollection series = createInputSeries(file,config);

        System.out.println("Displayed time: "+config.seconds);
        System.out.println("Displayed samples: "+config.sampleRate*config.seconds);
        System.out.println("Sample rate: "+config.sampleRate);
        System.out.println("Sine frequency: "+config.sineFrequency);
        final float samplesPerPeriod = config.sampleRate / config.sineFrequency;        
        System.out.println("Samples/"+config.sineFrequency+" Hz : "+samplesPerPeriod);
        final InputStream sineStream = new InputStream() 
        {
            private final double inc = 360 / samplesPerPeriod;
            private double degrees = 0;
            
            @Override
            public int read() throws IOException
            {
                final double rad = degrees*(Math.PI/180.0);
                final double v = Math.sin(rad); // -1...1
                degrees += inc;
                final int result = (int) (-128+((v+1)/2)*255);
                return result & 0xff;
            }
        };
        
        final XYSeriesCollection series = createInputSeries(sineStream,config);
        
        final XYSeries input = series.getSeries(SERIES_INPUT);
        final XYSeries sawtooth = series.getSeries(SERIES_SAWTOOTH);
        final XYSeries pwm = series.getSeries(SERIES_PWM);
        final XYSeries lowpass = series.getSeries(SERIES_LOWPASS);
        
        final JFreeChart inputChart = ChartFactory.createXYLineChart(
                "Input",
                "","",
                seriesCollection(input,sawtooth,pwm),
                PlotOrientation.VERTICAL,
                true,false,false);

        final JFreeChart sawToothChart = ChartFactory.createXYLineChart(
                "Output",
                "","",
                seriesCollection(input,lowpass),
                PlotOrientation.VERTICAL,
                true,false,false);        

        final ChartPanel chartPanel1 = new ChartPanel( inputChart );
        final ChartPanel chartPanel2 = new ChartPanel( sawToothChart );

        chartPanel1.setPreferredSize( new java.awt.Dimension( 560 , 200 ) );
        chartPanel2.setPreferredSize( new java.awt.Dimension( 560 , 200 ) );
        
        final JPanel panel = new JPanel();
        panel.setLayout( new GridBagLayout() );
        panel.add( chartPanel1 , cnstrs(0,0,1.0,0.5) );
        panel.add( chartPanel2, cnstrs(0,1,1.0,0.5) );
        return panel;
    }
    
    private GridBagConstraints cnstrs(int x,int y,double weightx,double weighty) 
    {
        GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.weightx = weightx; cnstrs.weighty = weighty;
        cnstrs.gridwidth = 1 ; cnstrs.gridheight = 1;
        cnstrs.gridx = x ; cnstrs.gridy = y;
        cnstrs.fill = GridBagConstraints.BOTH;
        return cnstrs;
    }
    
    private XYSeriesCollection createInputSeries(File file,Config config) throws UnsupportedAudioFileException, IOException {
        final AudioInputStream in = AudioSystem.getAudioInputStream( file );
        final AudioFormat format = in.getFormat();
        config.sampleRate = format.getSampleRate();
        return createInputSeries(in,config);
    }
    
    private XYSeriesCollection createInputSeries(InputStream in,Config config) throws UnsupportedAudioFileException, IOException
    {
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int value = 0;
        int maxSamples = (int) (config.sampleRate*config.seconds);              
        for ( int count = 0 ; ( value = in.read() ) != -1 && count < maxSamples ; count++) 
        {
            bout.write( value );
        }
        final byte[] bytes = bout.toByteArray();
        final double[] data = new double[bytes.length];
        for ( int i = 0 ; i < bytes.length;i++ ) {
            data[i]=128+bytes[i];
        }
        
        // create data series
        final XYSeriesCollection result = new XYSeriesCollection();        
        final XYSeries inputSeries = new XYSeries(SERIES_INPUT);
        final XYSeries sawToothSeries = new XYSeries(SERIES_SAWTOOTH);
        final XYSeries pwmSeries = new XYSeries(SERIES_PWM);        
        final XYSeries lowPassSeries = new XYSeries(SERIES_LOWPASS);        
        
        final double[] sawTooth = create8BitSawTooth( config );
        final double[] pwm = createPwm(data,sawTooth,config);
        final double[] lowPass = lpf( pwm, config );
        
        // pwm
        maxSamples = (int) (config.pwmFrequency*config.seconds*256);            
        for ( int i = 0 ; i < pwm.length && i < maxSamples;i++) 
        {
            final float timeInSeconds = (i/(config.pwmFrequency*256));
            pwmSeries.add(timeInSeconds,pwm[i]);
        }     
        
        // lowpass
        maxSamples = (int) (config.pwmFrequency*config.seconds*256);        
        for ( int i = 0 ; i < lowPass.length && i < maxSamples;i++) 
        {
            final float timeInSeconds = (i/(config.pwmFrequency*256));
            lowPassSeries.add(timeInSeconds,lowPass[i]);
        }        
        
        // sawtooth
        maxSamples = (int) (config.pwmFrequency*256*config.seconds);
        for ( int i = 0 ; i < sawTooth.length && i < maxSamples;i++) 
        {
            final float timeInSeconds = (i/(config.pwmFrequency*256));
            sawToothSeries.add(timeInSeconds,sawTooth[i]);
        }
        
        // data
        maxSamples = (int) (config.sampleRate*config.seconds);        
        for ( int i = 0 ; i < data.length && i < maxSamples;i++) 
        {
            final float timeInSeconds = (i/config.sampleRate);
            inputSeries.add(timeInSeconds,data[i]);
        }
        result.addSeries(inputSeries);
        result.addSeries(sawToothSeries);
        result.addSeries(pwmSeries);
        result.addSeries(lowPassSeries);
        return result;
    }
    
    private double[] createPwm(double[] signal,double[] sawTooth,Config config) 
    {
        final int noSamples = (int) (config.seconds*config.pwmFrequency*256);
        double[] result = new double[ noSamples ];
        System.out.println("PWM samples: "+noSamples);
        for ( int i = 0 ; i <sawTooth.length;i++) 
        {
            double t0 = i/(config.pwmFrequency*256);
            final int sampleIndex = (int) (t0 * config.sampleRate);
            double value;
            try {
                value = signal[sampleIndex % signal.length];
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("sampleIndex: "+sampleIndex+",max: "+signal.length);
                throw e;
            }
            if( value >= sawTooth[i] ) 
            {
                result[i] = 255;
            } 
            else if ( value < sawTooth[i] ) 
            {
                result[i] = 0;
            }
        }
        return result;
    }
    
    private double[] create8BitSawTooth(Config config) 
    {
        final int numberOfSamples = (int) (config.seconds * config.pwmFrequency * 256);        
        System.out.println("Creating "+numberOfSamples+" sawtooth samples with "+config.pwmFrequency+" Hz");
        
        double value = 0;
        final double[] result = new double[numberOfSamples];
        final double step = 2;
        System.out.println("Sawtooth step: "+step);
        int sign = 1;
        for ( int i = 0 ; i < numberOfSamples ; i++ ) 
        {
            if ( value > 255 ) {
                value = 255-(value-255);
                result[i] = value;
                sign = -sign;
            } else if ( value < 0 ) {
                value = -value;
                result[i] = value;
                sign = -sign;                
            } else {
                result[i] = value;
                value += sign*step;
            }
        }
        return result;
    }
    
    private double[] lpf(double[] dataIn,Config config) 
    {
        float tau = 1f/config.lowPassCutoffFrequency;
        float delta = 1f/(config.pwmFrequency);
        float alpha = delta / tau;
        final double[] dataOut = new double[dataIn.length];
        float yk = 0;
        for ( int k = 0 ; k < dataIn.length;k++) {
            yk += alpha * ( dataIn[k]-yk);
            dataOut[k] = yk;
        }
        return dataOut;
    }    
    
    private static XYSeriesCollection seriesCollection(XYSeries s1,XYSeries... series) {
        XYSeriesCollection result = new XYSeriesCollection();
        result.addSeries(s1);
        if ( series != null ) 
        {
            Stream.of(series).forEach( result::addSeries );
        }
        return result;
    }
    
    public static void main(String[] args) throws InvocationTargetException, InterruptedException
    {
        SwingUtilities.invokeAndWait( () -> {
            try {
                new Main().run();
            } catch (UnsupportedAudioFileException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Main() 
    {
        super("filtertoy");
        setVisible( true );
        setPreferredSize( new Dimension(640,480));
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }    
}