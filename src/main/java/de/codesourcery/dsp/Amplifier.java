package de.codesourcery.dsp;

public class Amplifier {

    private static String prettyPrint(double value,String baseUnit) 
    {
        if ( value >= 1000000 ) {
            return (value/1000000f)+" M"+baseUnit;
        }        
        if ( value >= 1000 ) {
            return (value/1000f)+" k"+baseUnit;
        }
        if ( value >= 1 ) {
            return value+" "+baseUnit;
        }        
        if ( value >= 0.001 ) {
            return (value*1000)+" m"+baseUnit;
        }
        if ( value >= 0.000001 ) {
            return (value*1000000)+" u"+baseUnit;
        }   
        if ( value >= 0.000000001 ) {
            return (value*1000000000)+" n"+baseUnit;
        }          
        return value+" "+baseUnit;
    }
    
    public static void main(String[] args) 
    {
        final float Vcc = 4f;
        final float Rl = 1000;
        final float beta = 200;
        final float Ve = 1.5f; // assumption: 1-2V
        final float lowestFreq = 20;
        
        final float Icq = (0.5f*Vcc)/Rl;
        final float Ib = Icq/beta;
        
        final float Vb = Ve + 0.7f;
        final float R2 = Vb/(10*Ib);
        final float R1 = (Vcc-Vb)/(11*Ib);
        
        final float Ie=Icq + Ib;
        final float Re = Ve/Ie;
        
        final float C2 = (float) ( 1/(2*Math.PI*lowestFreq*0.1*Re) );
        
        System.out.println("R1 = "+prettyPrint(R1,"Ohm"));
        System.out.println("R2 = "+prettyPrint(R2,"Ohm"));
        System.out.println("Re = "+prettyPrint(Re,"Ohm"));
        System.out.println("C2 = "+prettyPrint(C2,"Farad"));
    }
}
