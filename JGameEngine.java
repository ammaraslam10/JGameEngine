import java.awt.Canvas;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author ammaraslam10
 */
public class JGameEngine {
    /** Get how much time passed since last frame finished. This will be useful to write frame independent code by multiplication with this variable. */
    public volatile double deltaTime;
    private volatile int frameDelay;
    private JGameEngine.Window window;
    private JGameEngine.Key keyboard;
    private JGameEngine.Mouse mouse;
    private JGameEngine.Camera camera;
    private JGameEngine.Quadtree collisionTree;
    private JGameEngine.Audios audios;
    
    // Initialize JGameEngine
    public JGameEngine() {
	window = null;
	keyboard = new JGameEngine.Key();
	mouse = new JGameEngine.Mouse();
	camera = new JGameEngine.Camera();
	audios = new JGameEngine.Audios();
	collisionTree = null;
	deltaTime = 0;
	frameDelay = 0;
    }
    /** Get the delay between each each frame */
    int frameDelay() { return frameDelay; } 
    /** Set the delay between each each frame */
    void frameDelay(int delay) { frameDelay = delay; }
    /** Set a game space. Clears out all objects and sprites and prepares a room with the given size for collisions. It is the logical boundary of the Game Area */
    void setGameSpace(int room_width, int room_height) {
    	collisionTree = new JGameEngine.Quadtree(0, new Rectangle(room_width, room_height));
	if(window != null) {
	    window.objects.clear(); window.object_queue.clear(); window.object_queue_r.clear();
	    window.sprites.clear(); window.sprite_queue.clear(); window.sprite_queue_r.clear(); }
    }
    /** Actual window on the screen with positions x, y and dimensions width, height */
    void setWindow(String title, int x, int y, int width, int height) {
	if(window != null) { window.dispose(); window.onWindowClosing(); }
	window = new JGameEngine.Window(title, x, y, width, height);
	window.addWindowListener( new WindowAdapter() {
	   @Override
	   public void windowClosing( WindowEvent e ) {
		window.onWindowClosing();
		try {
		    window.gameThread.join();
		} catch( InterruptedException ex ) { ex.printStackTrace(); }
		System.exit( 0 );
	   }
	});
    }
    /** Actual window on the screen with positions at the centre of the screen and dimensions width, height */
    void setWindow(String title, int width, int height) { setWindow(title, (screenWidth() - width) / 2, (screenHeight() - height) / 2, 16 * 50, 9 * 50); }
    /** Actual window on the screen with positions at the centre of the screen and dimensions 800, 450 with a default Game Space of 4000, 2250 (if not set) */
    void setWindow(String title) { if(collisionTree == null) setGameSpace(4000, 2250); setWindow(title, 16 * 50, 9 * 50); }
    /** Set a window with default settings 800, 450 with a default Game Space of 4000, 2250 (if not set) */
    void setWindow() { if(collisionTree == null) setGameSpace(4000, 2250); setWindow("JGameEngine :)"); }
    /** Set the background colour */
    void setBackground(Color c) { window.canvas.setBackground(c); }
    /** Get the screen width. */
    public int screenWidth() { return Toolkit.getDefaultToolkit().getScreenSize().width; }
    /** Get the screen height.  */
    public int screenHeight() { return Toolkit.getDefaultToolkit().getScreenSize().height; }
    /** Get the window . This is a raw value, See cameraWidth() for a more useful value. */
    public int windowWidth() { return window.canvas.getSize().width; }
    /** Get the window height. This is a raw value, See cameraHeight() for a more useful value. */
    public int windowHeight() { return window.canvas.getSize().height; }
    /** Set if the window is resizable by user */
    public void windowResizable(boolean stance) { window.setResizable(stance); }
    /** Set window to full screen or make it windowed */
    public void windowFullScreen(boolean stance) {
	window.dispose();
	if(stance) {
	    window.setUndecorated(true);
	    window.setExtendedState(JFrame.MAXIMIZED_BOTH);
	} else {
	    window.setUndecorated(false);
	    window.setExtendedState(JFrame.NORMAL);
	    window.setLocation(window.x, window.y);
	    window.setSize(window.width, window.height);
	}
	window.setVisible(true);
    }
    //~~~~~~~~~~ Rendering
    private class Window extends JFrame implements Runnable {

	private ArrayList<JGameEngine.Object> objects, object_queue, object_queue_r;
	private ArrayList<JGameEngine.Sprite> sprites, sprite_queue, sprite_queue_r;
	private BufferStrategy bs;
	private volatile boolean running;
	private Thread gameThread;
	private Graphics g = null;
	private long last_time = System.currentTimeMillis();
	private Canvas canvas;
	int x, y, width, height;
	public Window(String title, int x, int y, int width, int height) {
	    canvas = new Canvas();
	    objects = new ArrayList<>(); object_queue = new ArrayList<>(); object_queue_r = new ArrayList<>();
	    sprites = new ArrayList<>(); sprite_queue = new ArrayList<>(); sprite_queue_r = new ArrayList<>();
	    this.x = x; this.y = y; this.width = width; this.height = height;
	    init(title, x, y, width, height);
	}
	private void init(String title, int x, int y, int width, int height) {
	    canvas.setSize( width, height );
	    canvas.setBackground( Color.WHITE );
	    canvas.setIgnoreRepaint( true );
	    setLocation(x, y);
	    getContentPane().add( canvas );
	    setTitle( title );
	    setIgnoreRepaint( true );
	    addKeyListener(keyboard);
	    this.setResizable(false);
	    
	    pack();
	    setVisible( true );
	    canvas.createBufferStrategy( 2 );
	    bs = canvas.getBufferStrategy();
	    canvas.addMouseListener(mouse);
	    addComponentListener(new ComponentAdapter() {  // Update canvas size on window resize
		@Override public void componentResized(ComponentEvent evt) {
		    canvas.setSize(getWidth(), getHeight());
		}
	    });
	    gameThread = new Thread( this );
	    gameThread.start();
	}
	@Override
	public void run() {
	    running = true;
	    if(collisionTree == null) { String err = "No Game Space is set! use setGameSpace()"; try { throw new Exception(err); } catch(Exception e) { System.out.println(err); } }
	    while( running ) {
		// Calculate time since last thread run as deltaTime => deltaTime can be used to
		// implement framerate independant code
		long current_time = System.currentTimeMillis();
		deltaTime = (current_time - last_time)/100f;
		last_time = current_time;
		gameLoop();
	    }
	}
	private void gameLoop() {
	    do {
	       do {
		  Graphics g = null;
		  try {
			g = bs.getDrawGraphics();
			g.clearRect( 0, 0, getWidth(), getHeight() );

			keyboard.allow_remove_pressed = true; keyboard.allow_remove_released = true; 
			mouse.allow_remove_clicked = true; mouse.allow_remove_released = true;

			render(g);
			
			if(keyboard.allow_remove_released) { keyboard.removeReleased(); keyboard.allow_remove_released = false; }
			if(keyboard.allow_remove_pressed) { keyboard.removePressed(); keyboard.allow_remove_pressed = false; }
			if(mouse.allow_remove_clicked) { mouse.removeClicked(); mouse.allow_remove_clicked = false; }
			if(mouse.allow_remove_released) { mouse.removeReleased(); mouse.allow_remove_released = false; }
			Thread.sleep(frameDelay);
		  } catch (InterruptedException ex) { ex.printStackTrace(); } 
		  finally { if( g != null ) g.dispose(); }
	       } while( bs.contentsRestored() );
	       bs.show();
	    } while( bs.contentsLost() );
	}
	// Run update code for each game object
	private void render(Graphics ga) {
	    g = ga;
	    // While running object loops, the objects may have requested to add/remove other objects, handle queue
	    while(!object_queue.isEmpty()) { objects.add(object_queue.remove(0)); } while(!object_queue_r.isEmpty()) { objects.remove(object_queue_r.remove(0)); }
	    while(!sprite_queue.isEmpty()) { sprites.add(sprite_queue.remove(0)); } while(!sprite_queue_r.isEmpty()) { sprites.remove(sprite_queue_r.remove(0)); }
	    collisionTree.runCollisions();
	    for(JGameEngine.Object o : objects) {
		o.preUpdate();
		o.update();
	    }
	    for(JGameEngine.Sprite s : sprites) {
		s.draw(s, g);
	    }
	    revalidate();
	}
	private void onWindowClosing() {
	    running = false;
	    System.gc();
	}
    }
    //~~~~~~~~~~ Rendering

    //~~~~~~~~~~ Game Object Managment
    private static interface WrapCall { public abstract void call(); }    
    /** The Object class represents a Game Object. Every Game Object has an x and y position, a start() and update() method */
    public static abstract class Object {
       /** The x position of the Game Object */	    public double x = 0;
       /** The y position of the Game Object */	    public double y = 0;
       /** The name of the Game Object */	    public String name = "Default";
       //private ArrayList<WrapCall> calls = new ArrayList<>();

       /** This function is called once when the object is added to the Game Space */
       public abstract void start();
       /** This function is called on every update cycle of the game. Also see deltaTime() */
       public abstract void update();
       private void preUpdate() {
	   // Current implementation never supported a preUpdate() so keeping this felt like a waste
	   // If there are additional object properties to invoke, do that before update (colliders etc)
	   // for(WrapCall c : calls) {
	   //	c.call();
	   // }
       }
    }
    /** Place a game object in current space, start() is called immediately. Object is added in next cycle */
    void addObject(JGameEngine.Object obj) {	
	obj.start(); // Initialize object
	window.object_queue.add(obj);
    }
    /** Remove a game object from current space, Object is removed in next cycle */
    void removeObject(JGameEngine.Object obj) {
	window.object_queue_r.add(obj);
    }
    //~~~~~~~~~~ Game Object Managment Ends

    //~~~~~~~~~~ Sprite Managment
    /** The Sprite class contains information about a sprite. A sprite is an image or a collection of images (for animation) */
    public class Sprite {
       /** The x-position in the game space */		double x = 0; 
       /** The y-position in the game space */		double y = 0;
       /** The width of the sprite	    */		double width = 0; 
       /** The height of the sprite	     */		double height = 0;
       /** The speed at which the animation cycles */	double image_speed = 0;
       /** The frame of the sprite animation */		int image_index = 0;
       private JGameEngine.Object obj = null;
       private BufferedImage[] img;
       private int subimages_x = 0, subimages_width = 0;
       private int subimages_y = 0, subimages_height = 0;
       private double current_count_speed;
       
       public Sprite(String image) {
	    try { 
		img = new BufferedImage[1];
		img[0] = ImageIO.read(new File(image));   
		width = img[0].getWidth(); height = img[0].getHeight();
	    } catch (IOException ex) { }
	}
	public Sprite(Object ob, String image) { this(image); obj = ob; x = 0; y = 0; }
	public Sprite(String image, int subimages_x, int subimages_width, int subimages_y, int subimages_height) {
	    this.subimages_x = subimages_x; this.subimages_y = subimages_y;
	    this.subimages_width = subimages_width; this.subimages_height = subimages_height;
	    BufferedImage tmp = null;
	    try { tmp = ImageIO.read(new File(image)); } catch (IOException ex) { System.out.println("Sprite:: Unable to open image " + image); }
	    if(subimages_x > 0 && subimages_y > 0) {
		img =  new BufferedImage[subimages_x * subimages_y];
		for(int i = 0; i < subimages_x; i++) {
		    for(int j = 0; j < subimages_y; j++) {
			img[i * subimages_y + j] = tmp.getSubimage(j * subimages_width, i * subimages_height, subimages_width, subimages_height);
		    }
		}
	    } else {
		img = new BufferedImage[1];
		img[0] = tmp;
	    }
	    width = img[0].getWidth(); height = img[0].getHeight();
	}
	public Sprite(Object ob, String image, int subimages_x, int subimages_width, int subimages_y, int subimages_height) { this(image, subimages_x, subimages_width, subimages_y, subimages_height); obj = ob; x = 0; y = 0; }
	private void draw(Sprite sprite, Graphics g) {
	    double draw_x = x, draw_y = y, check_x = x, check_y = y;
	    if(obj != null) {
		draw_x += obj.x; draw_y += obj.y;
		check_x += obj.x; check_y += obj.y;
	    }
	    draw_x = cameraCoordX(draw_x); draw_y = cameraCoordY(draw_y); 
	    double w = width; double h = height;
	    if(sprite.subimages_x == 0 || sprite.subimages_y == 0) {
		sprite.image_index = 0;
		if(!cameraBounded(check_x, check_y, w, h)) return;
		g.drawImage(sprite.img[sprite.image_index], (int) (draw_x * camera.d), (int) (draw_y * camera.d), (int) (w * camera.d), (int) (h * camera.d), null);
		return;
	    }
	    if(!cameraBounded(check_x, check_y, w, h)) return;
	    g.drawImage(sprite.img[sprite.image_index], (int) (draw_x * camera.d), (int) (draw_y * camera.d), (int) (w * camera.d), (int) (h * camera.d), null);

	    // Runs after about 1ms under frameDelay of 128. Can't keep up well afterwards. 
	    current_count_speed += image_speed * deltaTime;
	    if(current_count_speed >= 1) {
		sprite.image_index = (sprite.image_index + 1) % (sprite.subimages_x * sprite.subimages_y);
		current_count_speed -= 1;
	    }
	}
    }
    /** Create a Sprite from the path to an image.
     * @param path A String containing the path to the image
     * @return A Sprite */ 
    public Sprite sprite(String path) { return new Sprite(path); }
    /** Create a Sprite whose coordinates are relative to object. Moving the object by X amount will move the sprite by X amount.
     * @param obj The Game Object to follow
     * @param image A String containing the path to the image 
     * @return A Sprite */
    public Sprite sprite(Object obj, String image) { return new Sprite(obj, image); }
    /** Create an Animated Sprite, The images will cycle according to the image_speed set. The current image can be obtained or changed through image_index.
     * @param image A String containing the path to the image 
     * @param subimages_x The number of images that exist in the x-axis 
     * @param subimages_width The width of each image in the x-axis
     * @param subimages_y The number of images that exist in the y-axis 
     * @param subimages_height The width of each image in the y-axis 
     * @return A Sprite */
    public Sprite sprite(String image, int subimages_x, int subimages_width, int subimages_y, int subimages_height) { return new Sprite(image, subimages_x, subimages_width, subimages_y, subimages_height); }
    /** Create an Animated Sprite whose coordinates are relative to object. The images will cycle according to the image_speed set. The current image can be obtained or changed through image_index.  Moving the object by X amount will move the sprite by X amount. 
     * @param obj The game object to follow 
     * @param image A String containing the path to the image 
     * @param subimages_x The number of images that exist in the x-axis 
     * @param subimages_width The width of each image in the x-axis
     * @param subimages_y The number of images that exist in the y-axis 
     * @param subimages_height The width of each image in the y-axis 
     * @return A Sprite */
    public Sprite sprite(Object obj, String image, int subimages_x, int subimages_width, int subimages_y, int subimages_height) { return new Sprite(obj, image, subimages_x, subimages_width, subimages_y, subimages_height); }
    /** Draw a sprite */
    public void drawSprite(Sprite sprite) { sprite.draw(sprite, window.g); }
    /** Add a sprite to the game space. This may be done once in start() of your game object. Once a sprite is added it will keep being drawn until its removed */
    void addSprite(JGameEngine.Sprite spr) { window.sprite_queue.add(spr);  }
    /** Remove a sprite from the game space. This may be done once before deletion. Once a sprite is deleted it will no longer be drawn. See addSprite() */
    void removeSprite(JGameEngine.Sprite spr) { window.sprite_queue_r.add(spr); }
    /** Update sprite width but respect the aspect ratio */
    void spriteWidthRelative(Sprite sprite, double width) { sprite.height *= width/sprite.width; sprite.width = width; }
    /** Update sprite height but respect the aspect ratio */
    void spriteHeightRelative(Sprite sprite, double height) { sprite.width *= height/sprite.height; sprite.height = height; }
    //~~~~~~~~~~ Sprite Managment Ends

    //~~~~~~~~~~ Keyboard Input Managment
    // Keylistener for the window
    private class Key implements KeyListener {
	// Single press, Single Release, Button held down
	private volatile boolean[] pressed, released, pressing;
	private volatile boolean allow_remove_pressed = false, allow_remove_released = false;
	Key() {
	    pressed = new boolean[118];
	    released = new boolean[118];
	    pressing = new boolean[118];
	    for(int i = 0; i < 118; i++) {
		pressed[i] = false;
		released[i] = false;
		pressing[i] = false;
	    }
	}
	@Override public void keyTyped(KeyEvent e) { }
	@Override public void keyPressed(KeyEvent e) { allow_remove_pressed = false; if(!pressing[e.getKeyCode()]) pressed[e.getKeyCode()] = true; pressing[e.getKeyCode()] = true; }
	@Override public void keyReleased(KeyEvent e) { allow_remove_released = false; released[e.getKeyCode()] = true; pressing[e.getKeyCode()] = false; }
	private synchronized void removePressed() { 
	    for(int i = 0; i < 118; i++) pressed[i] = false; 
	}
	private synchronized void removeReleased() {
	    for(int i = 0; i < 118; i++) released[i] = false; 
	}
    }    
    // Helper to convert english to key codes 
    private int key_code(String key) {
	if(key.length() == 1) {
	    return key.charAt(0);
	}
	else if("up".equals(key)) return 38; else if("down".equals(key)) return 40; else if ("left".equals(key)) return 37;
	else if("right".equals(key)) return 39; else if("space".equals(key)) return 32; else if("tab".equals(key)) return 9;
	else if("enter".equals(key)) return 13; else if("ctrl".equals(key)) return 17; else if("alt".equals(key)) return 18;
	else if("right_click".equals(key)) return 93; else if("esc".equals(key)) return 27;
	return -1;
    }
    /** Will return true once when the key is first held down
     *  @param key A string representation of the key. May be one of the following: up, down, left, right, space, tab, enter, ctrl, alt, right_click, esc, [A-Z], [0-9], [Special Characters]*/
    public boolean keyPressed(String key) {
	int code = key_code(key);
	return keyboard.allow_remove_pressed && keyboard.pressed[code];
    }
    /** Will return true as long as the key is being held down
     *  @param key A string representation of the key. May be one of the following: up, down, left, right, space, tab, enter, ctrl, alt, right_click, esc, [A-Z], [0-9], [Special Characters]*/
    public boolean keyPressing(String key) {
	int code = key_code(key);
	return keyboard.pressing[code];
    }
    /** Will return true once when the key stops being held down
     *  @param key A string representation of the key. May be one of the following: up, down, left, right, space, tab, enter, ctrl, alt, right_click, esc, [A-Z], [0-9], [Special Characters]*/
    public boolean keyReleased(String key) {
	int code = key_code(key);
	return keyboard.allow_remove_released && keyboard.released[code];
    }
    //~~~~~~~~~~ Keyboard Input Managment Ends

    //~~~~~~~~~~ Mouse Input Managment 
    private class Mouse implements MouseListener { 
	private boolean mouseClicked = false, mouseRightClicked = false;
	private boolean mouseClicking = false, mouseReleased = false;
	private boolean windowFocus, allow_remove_clicked = false, allow_remove_released = false;
	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mousePressed(MouseEvent e) { 
	    allow_remove_clicked = false; 
	    if(!SwingUtilities.isRightMouseButton(e)) {
		if(mouseClicked == true) mouseClicked = false; else mouseClicked = true; mouseClicking = true; 
	    } else {
		if(mouseRightClicked == true) mouseRightClicked = false; else mouseRightClicked = true; 	    
	    }
	}
	@Override public void mouseReleased(MouseEvent e) { 
	    if(SwingUtilities.isRightMouseButton(e)) return;
	    allow_remove_released = false; mouseClicking = false; mouseReleased = true; 
	}
	@Override public void mouseEntered(MouseEvent e) { windowFocus = true; }
	@Override public void mouseExited(MouseEvent e) { windowFocus = false; }
	private void removeClicked() { mouseClicked = false; mouseRightClicked = false; }
	private void removeReleased() { mouseReleased = false; }
    }
    /** Get the x-position of the mouse inside the window, if the mouse is outside the window it will assume that the mouse is at the corner. You can use mouseFocused() to see if mouse is inside the window */
    public int mouseX() {
	try { return window.getContentPane().getMousePosition(true).x; }
	catch(Exception e) { 
	    if(MouseInfo.getPointerInfo().getLocation().x > window.getContentPane().getX() + window.getX() + window.getContentPane().getWidth()) 
		return window.getContentPane().getWidth() - 32;
	    else if(MouseInfo.getPointerInfo().getLocation().y > window.getContentPane().getY() + window.getY() + window.getContentPane().getHeight())
		if(MouseInfo.getPointerInfo().getLocation().x <= window.getContentPane().getX() + window.getX())
		    return 0;
		else return MouseInfo.getPointerInfo().getLocation().x - window.getContentPane().getX() - window.getX();
	    else if(MouseInfo.getPointerInfo().getLocation().x > window.getContentPane().getX() + window.getX())
		return MouseInfo.getPointerInfo().getLocation().x - window.getContentPane().getX() - window.getX();
	    else return 0;
	}
    }
    /** Get the y-position of the mouse inside the window, if the mouse is outside the window it will assume that the mouse is at the corner. You can use mouseFocused() to see if mouse is inside the window */
    public int mouseY() {
	try { return window.getContentPane().getMousePosition(true).y; }
	catch(Exception e) { 
	    if(MouseInfo.getPointerInfo().getLocation().y > window.getContentPane().getY() + window.getY() + window.getContentPane().getHeight()) 
		if(MouseInfo.getPointerInfo().getLocation().x < window.getContentPane().getX() + window.getX() + window.getContentPane().getWidth()) 
		    return window.getContentPane().getHeight() - 32;
		else return window.getContentPane().getHeight() - 32; 
	    else if(MouseInfo.getPointerInfo().getLocation().x > window.getContentPane().getX() + window.getX() + window.getContentPane().getWidth() && MouseInfo.getPointerInfo().getLocation().y < window.getContentPane().getY() + window.getY())
		return 0;
	    else if(MouseInfo.getPointerInfo().getLocation().x < window.getContentPane().getX() + window.getX() && MouseInfo.getPointerInfo().getLocation().y < window.getContentPane().getY() + window.getY())
		return 0;
	    else if(MouseInfo.getPointerInfo().getLocation().x > window.getContentPane().getX() + window.getX() && MouseInfo.getPointerInfo().getLocation().x < window.getContentPane().getX() + window.getX() + window.getContentPane().getWidth())
		return 0;
	    else return MouseInfo.getPointerInfo().getLocation().y - window.getContentPane().getY() - window.getY() - 16;
	}
    }
    /** Remove the cursor of the mouse. It can not be added back once it has been removed. */
    public void mouseDisableCursor() {
	BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
	Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0, 0), "blank cursor");
	window.getContentPane().setCursor(blankCursor);
    }
    /** Check if the mouse was left clicked. Occurs once the first click. */
    public boolean mouseClicked() { return mouse.allow_remove_clicked && mouse.mouseClicked; }
    /** Check if the mouse was right clicked. Occurs once the first click. */
    public boolean mouseRightClicked() { return mouse.allow_remove_clicked && mouse.mouseRightClicked; }
    /** Check if the mouse stopped being clicked. Occurs once when finger is lifted off. */
    public boolean mouseReleased() { return mouse.allow_remove_released && mouse.mouseReleased; }
    /** Check if the mouse being left clicked. Occurs as long as the finger isn't lifted off. */
    public boolean mouseClicking() { return mouse.mouseClicking; }
    /** Check if the mouse is inside the window */
    public boolean mouseFocused() { return mouse.windowFocus; }
    //~~~~~~~~~~ Mouse Input Managment Ends

    //~~~~~~~~~~ Camera 
    /** The Camera is part of the game space that is displayed on the window, The actual game space may be much much larger. 
     The coordinates of everything in the Game Space is translated into the Camera Space. A camera can be set to follow a Game Object. 
     The camera may also be zoomed in and out as required. The camera is a game object which is added to the Game Space when it is set to follow a Game Object. */
    public class Camera extends Object {
	private Object following;
	private double d; /* distance */
	public Camera() {
	    d = 1;
	    following = null;
	}
	@Override public void start() { }
	@Override public void update() {
	    if(following != null) {
		x = following.x;
		y = following.y;
	    }
	}
    }
    /** Set the camera to follow a Game Object */
    void cameraFollow(JGameEngine.Object obj) {
	camera.following = obj;
	if(obj != null)
	    addObject(camera);
	else 
	    removeObject(camera);
    }
    /** Get the camera's x-coordinate in the Game Space. It may be useful to position UI elements at the position of the camera so that the UI follows the camera */ 
    public double cameraX() { return camera.x; } 
    /** Get the camera's y-coordinate in the Game Space. It may be useful to position UI elements at the position of the camera so that the UI follows the camera */ 
    public double cameraY() { return camera.y; }
    /** Set the camera's x-coordinate in the Game Space */ 
    public void cameraX(double x) { camera.x = x; } 
    /** Set the camera's y-coordinate in the Game Space */ 
    public void cameraY(double y) { camera.y = y; } 
    /** Get how much the camera sees in the x-direction. When camera distance is 1 this value is same as windowWidth() otherwise the value scales with the cameraDistance */
    public double cameraWidth() { return window.width * 1 / camera.d; } 
    /** Get how much the camera sees in the y-direction. When camera distance is 1 this value is same as windowHeight() otherwise the value scales with the cameraDistance */
    public double cameraHeight() { return window.height * 1 / camera.d; }
    /** Set the camera distance */ 
    public void cameraDistance(double distance) { camera.d = 1/distance; } 
    /** Get the camera distance */ 
    public double cameraDistance() { return 1/camera.d; }
    // Get X, Y position relative to camera position to translate position to Window Space
    private double cameraCoordX(double x) { return x - camera.x; }
    private double cameraCoordY(double y) { return y - camera.y; }
    /** Check if a box lies inside the Camera Space (If it is currently visible to the player) */
    public boolean cameraBounded(double x, double y, double width, double height) {
	x = cameraCoordX(x); y = cameraCoordY(y);
	x *= camera.d; y *= camera.d; width *= camera.d; height *= camera.d;
	if(x + width < 0 && y + width < 0) return false;
	if(x > windowWidth() && y > windowHeight()) return false;
	return true;
    } 
    //~~~~~~~~~~ Camera Ends

    //~~~~~~~~~~ Draw
    /** Directly access the Graphics. This is not recommended as objects drawn from this will always be in the Camera Space (May be avoided if CameraX, CameraY, CameraDistance is used) */
    public Graphics draw() {
	return window.g;
    }
    /** Get the colour that the Graphics Object will use */ 
    public Color drawColor() { return draw().getColor(); } 
    /** Set the colour that the Graphics Object will use */ 
    public void drawColor(Color c) { draw().setColor(c); }
    /** Draw a line */ 
    public void drawLine(double x1, double y1, double x2, double y2) { 
	if(x2>x1 && y2>y1 && cameraBounded(x1,y1,x2-x1,y2-y1)) { x1 = cameraCoordX(x1); y1 = cameraCoordY(y1); x2 = cameraCoordX(x2); y2 = cameraCoordY(y2);
	    draw().drawLine((int) (x1 * camera.d), (int) (y1 * camera.d), (int) (x2 * camera.d), (int) (y2 * camera.d)); } 
    }
    /** Draw an oval */ 
    public void drawOval(double x, double y, double w, double h) {
	if(cameraBounded(x,y,w,h)) { x = cameraCoordX(x); y = cameraCoordY(y);
	    draw().drawOval((int) (x * camera.d), (int) (y * camera.d), (int) (w * camera.d), (int) (h * camera.d)); } 
    }
    /** Draw a rectangle */ 
    public void drawRect(double x, double y, double w, double h) { 
	if(cameraBounded(x,y,w,h)) { x = cameraCoordX(x); y = cameraCoordY(y);
	    draw().drawRect((int) (x * camera.d), (int) (y * camera.d), (int) (w * camera.d), (int) (h * camera.d)); }
    }
    /** Draw text */ 
    public void drawText(String s, double x, double y) { 
	window.g.setFont(window.g.getFont().deriveFont(window.g.getFont().getSize() * (float) camera.d));
	if(cameraBounded(x,y,textWidth(s),textHeight(s))) { x = cameraCoordX(x); y = cameraCoordY(y);
	    draw().drawString(s, (int) (x * camera.d), (int) (y * camera.d)); }
    }
    /** Draw a line */ 
    public void drawLine(double x1, double y1, double x2, double y2, Color c) { Color t = draw().getColor(); drawLine(x1, y1, x2, y2); draw().setColor(t); }
    /** Draw an oval */ 
    public void drawOval(double x, double y, double w, double h, Color c) { drawOval(x, y, w, h, c, false); }
    /** Draw a rectangle */ 
    public void drawRect(double x, double y, double w, double h, Color c) { drawRect(x, y, w, h, c, false); }
    /** Draw text */ 
    public void drawText(String s, double x, double y, Color c) { Color t = draw().getColor(); draw().setColor(c); drawText(s, x, y); draw().setColor(t); }
    /** Get the width of a text */  
    public double textWidth(String s) { return draw().getFontMetrics().stringWidth(s); }
    /** Get the height of a text */ 
    public double textHeight(String s) { return draw().getFontMetrics().getStringBounds(s, draw()).getHeight(); }
    /** Change the font of text to a system font. Type can be changed to "italic" and "bold", otherwise it will be normal. 3rd parameter is font size */  
    public void textFontSystem(String name, String type, int size) { int t = Font.PLAIN; if(type.equals("bold")) t = Font.BOLD; else if(type.equals("italic")) t = Font.ITALIC; draw().setFont(new Font(name, t, size)); }
    /** Change the font of text to a custom font from a given file, ttf format supported only. */  
    public void textFont(String path, float size) { 
	try { Font font = Font.createFont(Font.TRUETYPE_FONT, new File(path)); draw().setFont(font.deriveFont(size)); }
	catch(Exception e) { System.out.println("textFont() font " + path + " can't be set. details: " + e.toString()); }
    }
    /** Change the font size */
    public void textSize(float size) { draw().setFont(draw().getFont().deriveFont(size)); }
    /** Draw an filled oval of colour c */ 
    public void drawOval(double x, double y, double w, double h, Color c, Boolean fill) { Color t = draw().getColor(); draw().setColor(c); 
	if(fill && cameraBounded(x,y,w,h)) { x = cameraCoordX(x); y = cameraCoordY(y);
	    draw().fillOval((int) (x * camera.d), (int) (y * camera.d), (int) (w * camera.d), (int) (h * camera.d));
	} else drawOval(x, y, w, h); draw().setColor(t); 
    }
    /** Draw a filled rectangle of colour c*/ 
    public void drawRect(double x, double y, double w, double h, Color c, Boolean fill) { Color t = draw().getColor(); draw().setColor(c); 
	if(fill && cameraBounded(x,y,w,h)) { x = cameraCoordX(x); y = cameraCoordY(y);
	    draw().fillRect((int) (x * camera.d), (int) (y * camera.d), (int) (w * camera.d), (int) (h * camera.d));
	} else drawRect(x, y, w, h); draw().setColor(t); 
    }
    //~~~~~~~~~~ Draw Ends
    
    //~~~~~~~~~~ Collisions https://gamedevelopment.tutsplus.com/tutorials/quick-tip-use-quadtrees-to-detect-likely-collisions-in-2d-space--gamedev-374
    private class Quadtree {
	private int MAX_OBJECTS = 10, MAX_LEVELS = 5;
	private int level;
	private ArrayList<CollisionMask> objects, objects_a, objects_r;
	private Rectangle bounds;
	private Quadtree[] nodes;
	public Quadtree(int pLevel, Rectangle pBounds) {
	    level = pLevel;   objects = new ArrayList(); objects_r = new ArrayList();
	    bounds = pBounds; nodes = new Quadtree[4];   objects_a = new ArrayList();
	}
	private void clear() {
	    objects.clear();
	    for (int i = 0; i < nodes.length; i++) {
		if (nodes[i] != null) nodes[i].clear();
		nodes[i] = null;
	    }
	}
	private void split() {
	    int subWidth = (int)(bounds.getWidth() / 2), subHeight = (int)(bounds.getHeight() / 2);
	    int x = (int) bounds.getX(), y = (int) bounds.getY();

	    nodes[0] = new Quadtree(level+1, new Rectangle(x + subWidth, y, subWidth, subHeight));
	    nodes[1] = new Quadtree(level+1, new Rectangle(x, y, subWidth, subHeight));
	    nodes[2] = new Quadtree(level+1, new Rectangle(x, y + subHeight, subWidth, subHeight));
	    nodes[3] = new Quadtree(level+1, new Rectangle(x + subWidth, y + subHeight, subWidth, subHeight));
	}
	private int getIndex(Rectangle pRect) {
	    int index = -1;
	    double verticalMidpoint = bounds.getX() + (bounds.getWidth() / 2), horizontalMidpoint = bounds.getY() + (bounds.getHeight() / 2);
	    // Object can completely fit within the top/bottom quadrants
	    boolean topQuadrant = (pRect.getY() < horizontalMidpoint && pRect.getY() + pRect.getHeight() < horizontalMidpoint);
	    boolean bottomQuadrant = (pRect.getY() > horizontalMidpoint);
	    // Object can completely fit within the left/right quadrants
	    if (pRect.getX() < verticalMidpoint && pRect.getX() + pRect.getWidth() < verticalMidpoint) {
		if (topQuadrant)	 index = 1;
		else if (bottomQuadrant) index = 2;
	    }
	    else if (pRect.getX() > verticalMidpoint) {
		if (topQuadrant)	 index = 0;
		else if (bottomQuadrant) index = 3;
	    }
	    return index;
	}
	private void insert(CollisionMask m) {
	    if (nodes[0] != null) {
		int index = getIndex(m.realBoundsRect());
		if (index != -1) {
		    nodes[index].insert(m); return;
		}
	    }
	    objects.add(m);
	    if (objects.size() > MAX_OBJECTS && level < MAX_LEVELS) {
		if (nodes[0] == null)
		    split(); 
		int i = 0;
		while (i < objects.size()) {
		    int index = getIndex(objects.get(i).realBoundsRect());
		    if (index != -1) nodes[index].insert(objects.remove(i));
		    else i++;
		}
	    }
	}
	public ArrayList<CollisionMask> retrieve(ArrayList<CollisionMask> returnObjects, Rectangle pRect) {
	    int index = getIndex(pRect);
	    if (index != -1 && nodes[0] != null)
		nodes[index].retrieve(returnObjects, pRect);
	    returnObjects.addAll(objects);
	    return returnObjects;
	}
	public void remake() {
	    ArrayList<CollisionMask> tmp = new ArrayList<>(objects);
	    clear();
	    for(CollisionMask t : tmp) {
		insert(t);
	    }
	}
	public void remove(CollisionMask m) { objects_r.add(m); }
	public void add(CollisionMask m) { objects_a.add(m); }
	public void runCollisions() {
	    while(!objects_a.isEmpty()) { objects.add(objects_a.remove(0)); } while(!objects_r.isEmpty()) { objects.remove(objects_r.remove(0)); }
	    ArrayList<CollisionMask> returnObjects = new ArrayList();
	    for (int i = 0; i < objects.size(); i++) {
		returnObjects.clear(); retrieve(returnObjects, objects.get(i).realBoundsRect());
		for (int x = 0; x < returnObjects.size(); x++) {
		    if(objects.get(i).check(objects.get(i).realBounds(), returnObjects.get(x).realBounds()) && objects.get(i) != returnObjects.get(x)) {
			objects.get(i).c.collision(returnObjects.get(x).o);
		    }
		}
	    }
	}
    }
    private class CollisionMask {
	char type;
	double x, y, w, h;
	Object o = null; Collision c = null;
	public CollisionMask(Object obj, Collision col, double x, double y, double r) {
	    o = obj; c = col; this.x = x; this.y = y; this.w = r; this.h = r; this.type = 1;
	}
	public CollisionMask(Object obj, Collision col, double x, double y, double w, double h) {
	    o = obj; c = col; this.x = x; this.y = y; this.w = w; this.h = h; this.type = 0;
	}
	public boolean check(CollisionMask c1, CollisionMask c2) {
	    // https://developer.mozilla.org/en-US/docs/Games/Techniques/2D_collision_detection & https://stackoverflow.com/questions/401847/circle-rectangle-collision-detection-intersection
	    if(c1.type == 0 && c2.type == 0) {		
		if (c1.x < c2.x + c2.w &&
		    c1.x + c1.w > c2.x &&
		    c1.y < c2.y + c2.h &&
		    c1.y + c1.h > c2.y) return true;
	    } else if(c1.type == 1 && c2.type == 1) {
		double	dx = c1.x - c2.x + c1.w - c2.w,
			dy = c1.y - c2.y + c1.w - c2.w;
		double distance = Math.sqrt(dx * dx + dy * dy);
		if (distance < c1.w + c2.w) return true;
	    } else {
		double c1x = c1.x, c1y = c1.y, c1w = c1.w, c1h = c1.h, 
		       c2x = c2.x + c2.w, c2y = c2.y + c2.w, c2r = c2.w;
		if(c1.type == 1) {
		    c1x = c2.x; c1y = c2.y; c1w = c2.w; c1h = c2.h;
		    c2x = c1.x + c1.w; c2y = c1.y + c1.w; c2r = c1.w; 
		}
		double	DeltaX = c2x - Math.max(c1x, Math.min(c2x, c1x + c1w)),
			DeltaY = c2y - Math.max(c1y, Math.min(c2y, c1y + c1h));
		return (DeltaX * DeltaX + DeltaY * DeltaY) < (c2r * c2r);
	    }
	    return false;
	}
	/** Get real bounds in rectangle form */
	Rectangle realBoundsRect() { CollisionMask tmp = realBounds(); return new Rectangle((int) tmp.x, (int) tmp.y, (int) tmp.w, (int) tmp.h); }
	/** Get real bounds (relative to position of object) */
	CollisionMask realBounds() {
	    if(o == null) return this;
	    CollisionMask tmp = new CollisionMask(o, c, x, y, w, h); tmp.type = type;
	    tmp.x += o.x; tmp.y += o.y;
	    return tmp;
	}
    }
    /** Need to be implemented by Objects that need collisions, also see collisionMaskAdd() */
    public interface Collision { public abstract void collision(Object with); }
    /** Add a collision mask to Object. Object must implement Collision class, Note that r is radius (not width/diameter). The collision box will be relative to Object x, y (So x=0,y=0 is the x,y position of Object), Need to be done once in Start() */
    CollisionMask collisionMaskAdd(Object obj, double x, double y, double r) {
	CollisionMask m = null; 
	if(Collision.class.isInstance(obj)) {
	    m = new CollisionMask(obj, (Collision) obj, x, y, r);
	    collisionTree.add(m);
	} else {
	    String err = "Trying to add a Collision Mask to an Object that doesn't implement Collsions (Add implements Collision to your Object Class)"; try{ throw new Exception(err); }
	    catch(Exception e) { System.out.println(err); }
	}    
	return m;
    }
    /** Add a collision mask to Object. Object must implement Collision class, the collision box will be relative to Object x, y (So x=0,y=0 is the x,y position of Object), Need to be done once in Start() */
    CollisionMask collisionMaskAdd(Object obj, double x, double y, double w, double h) {
	CollisionMask m = null; 
	if(Collision.class.isInstance(obj)) {
	    m = new CollisionMask(obj, (Collision) obj, x, y, w, h);
	    collisionTree.add(m);
	} else {
	    String err = "Trying to add a Collision Mask to an Object that doesn't implement Collsions (Add implements Collision to your Object Class)"; try{ throw new Exception(err); }
	    catch(Exception e) { System.out.println(err); }
	}
	return m;
    }
    /** Remove the collision mask */
    void collisionMaskRemove(CollisionMask m) {
	collisionTree.remove(m);
    }
    /** Draw all collision masks */
    void collisionMaskDebug() {
	for(int i = 0; i < collisionTree.objects.size(); i++) {
	    CollisionMask m = collisionTree.objects.get(i).realBounds();
	    if(m.type == 0) { drawRect(m.x, m.y, m.w, m.h); }
	    else if(m.type == 1) { this.drawOval(m.x, m.y, m.w * 2, m.w * 2); }
	}
    }
    /** Get the list of objects that are collide to the given point */
    ArrayList<CollisionMask> collisionPointTest(int x, int y) {
	return collisionBoxTest(x,y,1,1);
    }    
    /** Get the list of objects that are collide to the given box */
    ArrayList<CollisionMask> collisionBoxTest(int x, int y, int w, int h) {
	ArrayList<CollisionMask> m = new ArrayList<>();
	collisionTree.retrieve(m, new Rectangle(x, y, w, h));
	CollisionMask tmp = new CollisionMask(null, null, x, y, w, h);
	for (int i = 0; i < m.size(); i++) {
	    if(!m.get(i).check(m.get(i), tmp)) {
		m.remove(m.get(i));
	    }
	}
	return m;
    }    
    //~~~~~~~~~~ Collisions End
    
    //~~~~~~~~~~ Audio https://www.geeksforgeeks.org/play-audio-file-using-java/
    private class Audio {
	long current;
	String status, file;
	Clip clip;
	Boolean loop;
	Audio(String file, boolean loop) { this.file = file; this.loop = loop; makeStream(file, loop); }
	Audio(String file, boolean loop, float gain) { this.file = file; this.loop = loop; makeStream(file, loop); setVolume(gain); }
	void makeStream(String file, boolean loop) {
	    AudioInputStream audioInputStream = null;
	    try {
		audioInputStream = AudioSystem.getAudioInputStream(new File(file).getAbsoluteFile());
		clip = AudioSystem.getClip(); 
	        clip.open(audioInputStream);     
	    } catch(Exception e) {
		System.out.println("Sound clip " + file + " doesn't exist or line unavailable. details: " + e.toString());
	    }
	    if(loop)
		clip.loop(Clip.LOOP_CONTINUOUSLY); 
	    status = "playing";
	}
	private void play(float gain) {
	    if(status.equals("none")) {
		clip.start();
		status = "playing";
	    } else if(status.equals("paused")) {
		clip.close(); 
		makeStream(file, loop); 
		clip.setMicrosecondPosition(current); 
		this.play(gain); 
	    }
	    if(gain != -1) setVolume(gain);
	}
	private void setVolume(float gain) {
	    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
	    float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
	    gainControl.setValue(dB);
	}
	private void pause() {
	    if(status.equals("none") || status.equals("paused")) return;
	    clip.stop();
	    current = clip.getMicrosecondPosition();
	    status = "paused";
	}
	public void stop() { 
	    clip.stop(); 
	    clip.close(); 
        } 
    }
    private class Audios {
	ArrayList<Audio> audios;
	public Audios() { audios = new ArrayList<>(); }
	void add(String path, Boolean loop, float gain) { 
	    // remove inactive instances of this audio clip
	    for(int i = 0; i < audios.size(); i++) 
		if(audios.get(i).file.equalsIgnoreCase(path)) 
		    if(!audios.get(i).clip.isRunning() && !audios.get(i).status.equals("paused")) audios.remove(i);
	    audios.add(new Audio(path, loop, gain));    
	}
	Audio find(String path) { 
	    for(int i = 0; i < audios.size(); i++) if(audios.get(i).file.equalsIgnoreCase(path)) return audios.get(i);
	    return null;
	}
	void remove(String path) { Audio found = find(path); if(found != null) { found.stop(); audios.remove(found); } }
	void removeAll(String path) { 
	    Audio found = null; 
	    do { found = find(path); if(found != null) { found.stop(); audios.remove(found); }
	    } while(found != null);
	}
    }
    /** Play a new audio */
    void audioPlay(String path, boolean loop, float gain) { audios.add(path, loop, gain); }
    /** Check if given audio is playing (not paused and haven't ended playback) */
    Boolean audioPlaying(String path) { Audio found = audios.find(path); if(found != null && found.status.equals("playing")) return found.clip.isRunning(); return false; }
    /** Pause an already playing audio */
    void audioPause(String path) { Audio found = audios.find(path); if(found != null) found.pause(); }
    /** Resume an already playing audio */
    void audioResume(String path) { Audio found = audios.find(path); if(found != null) found.play(-1); }
    /** Remove an audio */
    void audioRemove(String path) { audios.remove(path);}
    //~~~~~~~~~~ Audio End 
    
    /** Make a colour from rgb value */	    public Color color(int r, int g, int b) { return new java.awt.Color(r, g, b); }
    /** Make a colour from rgba value */    public Color color(int r, int g, int b, int a) { return new java.awt.Color(r, g, b, a); }
    /** Get the fps in the last update */   public double fps() { return 1 / (deltaTime / 10f); }
}
