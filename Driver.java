public class Driver extends JGameEngine {
    public static void main(String[] args) {
	ClockTest c = new ClockTest();		
    }
}

class ClockTest extends JGameEngine {
    public ClockTest() {
    	this.setWindow("JGameEngine test");
	this.addObject(new dummyClock(this));	
    }
}

class dummyClock extends JGameEngine.Object {
    JGameEngine e;
    public dummyClock(JGameEngine e) {
	this.e = e;
    }
    @Override
    public void update() {
	String time = String.valueOf(System.currentTimeMillis());
	// Set font
	e.textSize(64);
	// Write text at the center of window
	e.drawText( time, 
		    e.cameraWidth() / 2 - e.textWidth(time) / 2, 
		    e.cameraHeight() / 2);
    }
    @Override
    public void start() { }
}