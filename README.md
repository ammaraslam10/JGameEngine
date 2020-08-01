
# JGameEngine
A basic class that uses a Canvas and a JFrame but bundles in basic 2D game development tools.
To develop NES/SNES styled games like Mario and The Legend of Zelda, [JTiledUtility](https://github.com/ammaraslam10/JTiledUtility) may be used to simplify the map creation process. It is kept as a separate utility due to its dependence on the GSon library.

## Usage
A class may be extended to JGameEngine to initialize the components. A window needs to be added afterwards in order to have the display. Without `setWindow()`, no window will appear.

```java
class ClockTest extends JGameEngine {
    public ClockTest() {
        this.setWindow("JGameEngine test");
        // this.addObject(new dummyClock(this));
    }
    public static void main(String[] args) {
	ClockTest c = new ClockTest();
    }
}
```
Writing this much code is enough to produce a window with the given String as the title. 
There is a subclass `JGameEngine.Object` that can be used to create game objects. These objects have abstract methods `start()` and `update()`. Start is called only once when the object is added to the game, update is called on every frame update.

```java
class dummyClock extends JGameEngine.Object {
    JGameEngine e;
    public dummyClock(JGameEngine e) { this.e = e; }

    @Override public void update() {
        String time = String.valueOf(System.currentTimeMillis());
        e.textSize(64);
        e.drawText(time, 
              e.cameraWidth() / 2 - e.textWidth(time) / 2, 
              e.cameraHeight() / 2);
    }
    @Override public void start() { }
}
```
The above code will place current time at the center of the screen when this object is added to the game. After the creation of a game object, it needs to be added to the game (or Game Space), this is done by `addObject()`. The line that was commented in the Constructor needs to be uncommented in order for this to work. 

## Included components and their methods
Everything can be classified into the following different components.

### Window
A window is a JFrame but it has additional properties. The window is updated after every `frameDelay` milliseconds. on every update, all objects in the Game Space are updated as well and the frame of the `Sprite` is updated.  The delay between each frame can se accessed by `deltaTime` and should be multiplied with movement/physics variables to have smooth movement. A window may not see the entirety of the Game Space. This concept is discussed in Camera section.
|   |   |
|--|--|
| [`void setWindow(String title, int x, int y, int width, int height)`](#) <br/> Set the window to be used. | [`void setGameSpace(int room_width, int room_height)`](#) <br/> Set the dimensions of the map. |
| [`void windowWidth()`](#) <br/> Get the raw window width. | [`void windowHeight()`](#) <br/> Get the raw window height. |
| [`void windowResizable(Boolean stance)`](#) <br/> Disable/enable window resizing. | [`void windowFullScreen(Boolean stance)`](#) <br/> Disable/enable fullscreen. |

### Object
Classes may be extended to `GameEngine.Object` to have the properties `x`, `y` and `name` available. Objects must implement `start()` and `update()` methods. Objects need to be added to the Game Space in order to have these methods automatically invoke on every Game Update.
|   |   |
|--|--|
| [`void addObject(JGameEngine.Object)`](#) <br/> Add an object to the Game Space. | [`void removeObject(JGameEngine.Object)`](#) <br/> Remove an object from the Game Space. |

### Sprite
Sprites need to be created before they are added. A sprite in this context represents an image that can be drawn. Each sprite has an `x` and `y` position as well as a `width` and `height`. It also has properties like `image_speed` and `image_index` that can be used to modify how fast the image is animating and what is the current frame of the animation (in case of animated sprites). If a sprite is associated with a Game Object, it is drawn relative to that Object.
|   |   |
|--|--|
| [`Sprite sprite(String path)`](#) <br/> Return a sprite from given path. | [`Sprite sprite(JGameEngine.Object obj, String image)`](#) <br/> Return a sprite from path that is bound to the coordinates of a Game Object. |
| [`Sprite sprite(String image, int subimages_x, int subimages_width, int subimages_y, int subimages_height)`](#) <br/> Return an animated sprite that has many images in the x, y direction, each of provided width & height. | [`Sprite sprite(Object obj, String image, int subimages_x, int subimages_width, int subimages_y, int subimages_height)`](#) <br/> Return an animated sprite bound to a Game Object that has many images in the x, y direction, each of provided width & height. |
| [`void addSprite(JGameEngine.Sprite spr)`](#) <br/> Add a sprite to the Game Space once and have it be drawn automatically. |  [`void removeSprite(JGameEngine.Sprite spr)`](#) <br/> An added sprite can be removed |
| [`void spriteWidthRelative(JGameEngine.Sprite spr, double width)`](#) <br/> Change sprite width while maintaining the aspect ratio. |  [`void spriteHeightRelative(JGameEngine.Sprite spr, double height)`](#) <br/> Change sprite height while maintaining the aspect ratio. |
| [`void drawSprite(Sprite sprite)`](#) <br/> Can be called inside the `update()` of an object to have the sprite drawn every frame without adding. |

### Draw
Shapes and other drawing tools.
|   |   |
|--|--|
| [`Graphics draw()`](#) <br/> Access the Graphics Object directly. |  |
| [`Color drawColor()`](#) <br/> Get the current color. | [`void drawColor(Color c)`](#) <br/> Change the current color. |
| [`void drawLine(double x1, double y1, double x2, double y2)`](#) <br/> Draw a line. | [`void drawOval(double x, double y, double w, double h)`](#) <br/> Draw an Oval. |
| [`void drawRect(double x, double y, double w, double h)`](#) <br/> Draw a rectangle. | [`void drawText(String s, double x, double y)`](#) <br/> Draw text. |
| [`double textWidth(String s)`](#) <br/> Get the width of the String. | [`double textHeight(String s)`](#) <br/> Get the height of the String.|
| [`void textFontSystem(String name, String type, int size)`](#) <br/> Select a system font to use. | [`void textFont(String path, float size)`](#) <br/> Use the font from the given ttf file. |
| [`void textSize(float size)`](#) <br/> Change text size. |  |

### Keyboard
There are 3 keyboard events recognized, `keyPressed` (true once when the  key is hit for the first time), `keyPressing` (true as long as key is being held down) and `keyReleased` (true once when the keyboard key stops being held). To better support readability, parameters are taken as strings.
`A-Z` are `0-9` and special characters are recognized as themselves in strings (i.e. "A", "0", or "%"), additionally the following strings are recognized `up, down, left, right, space, tab, enter, ctrl, alt, right_click, esc`
|   |   |
|--|--|
| [`boolean keyPressed(String key)`](#) <br/> Check if a key was pressed. | [`boolean keyPressing(String key)`](#) <br/> Check if a key is being held down. |
| [`boolean keyReleased(String key)`](#) <br/> Check if a key was released. |  |

### Mouse
Events are similar to keyboard
|   |   |
|--|--|
| [`int mouseX()`](#) <br/> Get the x-coordinate of mouse. | [`int mouseY()`](#) <br/> Get the y-coordinate of mouse. |
| [`void mouseDisableCursor()`](#) <br/> Disable the pointer. | [`boolean mouseClicked()`](#) <br/> Check if a left click occurred. |
| [`boolean mouseRightClicked()`](#) <br/> Check if a right click occurred. | [`boolean mouseReleased()`](#) <br/> Check if left click was released. |
| [`boolean mouseClicking()`](#) <br/> Check if left click is being clicked. | [`boolean mouseFocused()`](#) <br/> Check if mouse is inside the window. |

### Camera
For games that make use of a big map and only a part of it needs to be visible at a time, the camera is a great tool.
|   |   |
|--|--|
| [`void cameraFollow(JGameEngine.Object obj)`](#) <br/> Follow an object's x, y position. |  |
| [`double cameraX()`](#) <br/> Get the x-position of camera in the map. | [`double cameraY()`](#) <br/> Get the y-position of camera in the map. |
| [`void cameraX(double x)`](#) <br/> Set the x-position of camera in the map. | [`void cameraY(double y)`](#) <br/> Set the y-position of camera in the map. |
| [`double cameraWidth()`](#) <br/> Get the width of the camera in the map. | [`double cameraHeight()`](#) <br/> Get the height of the camera in the map. |
| [`double cameraDistance()`](#) <br/> Get the distance of camera. | [`void cameraDistance(double distance)`](#) <br/> Set the distance of camera (zoom). |
| [`boolean cameraBounded(double x, double y, double width, double height) `](#) <br/> Check if an object is visible to the Camera. |  |

###  Audio
Audio can be played. Current implementation uses Java Clip so all limitations that come from Clip are inherited, this includes not being able to play mp3 files, and not all wav files are supported as well. The pause and resume are imperfect.
|   |   |
|--|--|
| [`void audioPlay(String path, boolean loop, float gain)`](#) <br/> Start playing an audio. | [`Boolean audioPlaying(String path)`](#) <br/> Check if an audio is playing. |
| [`void audioPause(String path)`](#) <br/> Pause an audio. | [`void audioResume(String path)`](#) <br/> Resume playing an audio. |
| [`void audioRemove(String path)`](#) <br/> Stop playing an audio & remove it's resources. |  |

###  Collision
A tool is provided to effortlessly handle collisions by taking collisions as events. An area can be masked (relative to Game Space or a Game Object). A class that extends from `JGameEngine.Object` and implements `JGameEngine.Collision` can create a collisionMask and add it to the Game Space. A mask would be any area that will result in a collision, See `void collisionMaskDebug()`. The x, y position of a mask is ralative to the object it is attached to.. As the object moves, the mask will move. When something touches the Game Object after when a mask is added, the implementable function `void collision(Object with)` is called. Only rectangular and circular masks are currently supported.
Sample code:
    
```java
class Box extends JGameEngine.Object implements JGameEngine.Collision {
    JGameEngine e;
    public int width, height;
    // Properties x, y and name are inherited.
    public Box(JGameEngine e, int x, int y, int width, int height) {
        this.e = e;	
        this.x = x; this.y = y;
        this.width = width; this.height = height;
        name = "Box";
    }
    @Override public void start() {
        e.collisionMaskAdd(this, 0, 0, width, height); 
    }
    @Override public void update() {
        e.drawRect(x, y, width, height);
    }
    @Override public void collision(JGameEngine.Object with) {
        System.out.println(this.name + " is touching " + with.name);
    }
}
class BoxTest extends JGameEngine {
    public BoxTest() {
    	this.setWindow("JGameEngine test");
	this.addObject(new Box(this, 0, 0, 50, 50));
	this.addObject(new Box(this, 20, 30, 100, 50));
    }
    public static void main(String[] args) {
	BoxTest c = new BoxTest();
    }
}
```
|   |   |
|--|--|
| [`CollisionMask collisionMaskAdd(Object obj, double x, double y, double r)`](#) <br/> Create and add a circular collision mask for the object. | [`CollisionMask collisionMaskAdd(Object obj, double x, double y, double w, double h)`](#) <br/> Create and add a rectangular collision mask for the object. |
| [`void collisionMaskRemove(CollisionMask m)`](#) <br/> Remove a collision mask. | [`void collisionMaskDebug()`](#) <br/> Draw all masks to debug. |
| [`ArrayList<CollisionMask> collisionPointTest(int x, int y)`](#) <br/> Return a list of masks that collided with the given point. | [`ArrayList<CollisionMask> collisionBoxTest(int x, int y, int w, int h)`](#) <br/> Return a list of masks that collided with the given rectangle. |

###  Misc
These don't belong to a category
|   |   |
|--|--|
| [`int frameDelay()`](#) <br/> Get the artificial delay between each update. | [`void frameDelay(int delay)`](#) <br/> Set an artificial delay between each update. |
| [`void setBackground(Color c)`](#) <br/> Set the background of the window. | [`double fps()`](#) <br/> Get the current FPS. |
| [`int screenWidth()`](#) <br/> Return the screen width. | [`int screenHeight()`](#) <br/> Return the screen height. |
| [`Color color(int r, int g, int b)`](#) <br/> Create a color from RGB. | [`Color color(int r, int g, int b, int a)`](#) <br/> Create a color from RGBA. |

## Questions
**Bugs**
Yes there are many. Some of this code is untested (new features breaking previously working features).

**wHEre iS OOP?**
For readability, all of the Object Orientation is hidden using Wrappers. This simplicity is inspired by Game Maker Studio's GML.

## License
This project is licensed under the MIT License - see the LICENSE.md file for details

## Contributors
Ammar Aslam <br/>
Anzar Ahmad <br/>
Hasaan Majeed

## Acknowledgments
[Collisions - Quadtrees on tutsplus](https://gamedevelopment.tutsplus.com/tutorials/quick-tip-use-quadtrees-to-detect-likely-collisions-in-2d-space--gamedev-374)

[Collisions - Mozilla](https://developer.mozilla.org/en-US/docs/Games/Techniques/2D_collision_detection)

[Collisions - Stackoverflow](https://stackoverflow.com/questions/401847/circle-rectangle-collision-detection-intersection)

[Audio - geeksforgeeks](https://www.geeksforgeeks.org/play-audio-file-using-java/)
