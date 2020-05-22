# JGameEngine
A basic class that uses a Canvas and a JFrame but bundles in basic 2D game development tools.
To develop NES/SNES styled games like Mario and The Legend of Zelda, `JTiledUtility` may be used to simplify the map creation process. It is kept as a separate utility due to it's dependence on the GSon library.

## Usage
A class may be extended to JGameEngine to initialize the components. A window needs to be added afterwards in order to have the display.

```java
class ClockTest extends JGameEngine {
    public ClockTest() {
        this.setWindow("JGameEngine test");
        // this.addObject(new dummyClock(this));
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
The above code will place current time at the center of the screen when this object is added to the game. The line that was commented in the Constructor needs to be uncommented in order for this to work.

## Included tools and their methods
Everything can be classified into the following different areas.

### Window
A window is a JFrame but it has additional properties. The window is updated after every `frameDelay` milliseconds. on every update, all objects in the Game Space are updated as well and the frame of the `Sprite` is updated.  A window may not see the entirety of the Game Space. This concept is discussed in Camera section.
|   |   |
|--|--|
|  `void setWindow(String title, int x, int y, int width, int height)` | `void setGameSpace(int room_width, int room_height)` |
| `void windowWidth()` | `void windowHeight()` |
| `void windowResizable(Boolean stance)` | `void windowFullScreen(Boolean stance)` |

### Object
Classes may be extended to `GameEngine.Object` to have the properties `x`, `y` and `name` available. Objects must implement `start()` and `update()` methods. Objects need to be added to the Game Space in order to have these methods automatically invoke on every Game Update.
|   |   |
|--|--|
|  `void addObject(JGameEngine.Object)` | `void removeObject(JGameEngine.Object)` |

### Sprite
Sprites need to be created before they are added. A sprite in this context represents an image that can be drawn. Each sprite has an `x` and `y` position as well as a `width` and `height`. It also has properties like `image_speed` and `image_index` that can be used to modify how fast the image is animating and what is the current frame of the animation (in case of animated sprites). If a sprite is associated with a Game Object, it is drawn relative to that Object.
|   |   |
|--|--|
|  `Sprite sprite(String path)` - return a sprite from given path | `Sprite sprite(JGameEngine.Object obj, String image)` - return a sprite from path that is bound to the coordinates of a Game Object |
|  `Sprite sprite(String image, int subimages_x, int subimages_width, int subimages_y, int subimages_height)` - return an animated sprite that has many images in the x, y direction, each of provided width & height | `Sprite sprite(Object obj, String image, int subimages_x, int subimages_width, int subimages_y, int subimages_height)`- return an animated sprite bound to a Game Object that has many images in the x, y direction, each of provided width & height |
|  `void drawSprite(Sprite sprite)` - May be called inside the `update()` of an object to have the sprite drawn every frame | `void addSprite(JGameEngine.Sprite spr)`- add a sprite to the Game Space once and have it be drawn automatically |
|  `void removeSprite(JGameEngine.Sprite spr)` |  |

### Draw
Shapes and other drawing tools.
|   |   |
|--|--|
|  `Graphics draw()` | `boolean keyPressing(String key)` |
|  `Color drawColor()` | `void drawColor(Color c)` |
|  `void drawLine(double x1, double y1, double x2, double y2)` | `void drawOval(double x, double y, double w, double h)` |
|  `void drawRect(double x, double y, double w, double h)` | `void drawText(String s, double x, double y)` |
|  `double textWidth(String s)` | `double textHeight(String s)` |
|  `void textFontSystem(String name, String type, int size)` | `void textFont(String path, float size)` |
|  `void textSize(float size)` |  |

### Keyboard
There are 3 keyboard events recognized, `keyPressed` (true once when the  key is hit for the first time), `keyPressing` (true as long as key is being held down) and `keyReleased` (true once when the keyboard key stops being held). To better support readability, parameters are taken as strings.
`A-Z` are `0-9` and special characters are recognized as themselves in strings (i.e. "A", "0", or "%"), additionally the following strings are recognized `up, down, left, right, space, tab, enter, ctrl, alt, right_click, esc`
|   |   |
|--|--|
|  `boolean keyPressed(String key)` | `boolean keyPressing(String key)` |
|  `boolean keyReleased(String key)` |  |

### Mouse
Events are similar to keyboard
|   |   |
|--|--|
|  `int mouseX()` | `int mouseX()` |
|  `void mouseDisableCursor()` | `boolean mouseClicked()` |
|  `boolean mouseRightClicked()` | `boolean mouseReleased()` |
|  `boolean mouseClicking()` | `boolean mouseFocused()` |

### Camera
For games that make use of a big map and only a part of it needs to be visible at a time, the camera is a great tool.
|   |   |
|--|--|
|  `void cameraFollow(JGameEngine.Object obj)` - Follow the object's x, y position | `int mouseX()` |
|  `double cameraX()` | `double cameraY()` |
|  `void cameraX(double x)` | `void cameraY(double y)` |
|  `double cameraWidth()` | `double cameraHeight()` |
|  `double cameraDistance()` | `void cameraDistance(double distance)` |
|  `boolean cameraBounded(double x, double y, double width, double height) ` |  |

###  Audio
Audio can be played. Current implementation uses Java Clip so all limitations that come from Clip are inherited, this includes not being able to play mp3 files, and not all wav files are supported as well. The pause and resume are imperfect.
|   |   |
|--|--|
|  `void audioPlay(String path, boolean loop, float gain)` | `Boolean audioPlaying(String path)` |
|  `void audioPause(String path)` | `void audioResume(String path)` |
|  `void audioRemove(String path)` |  |

###  Collision
A tool is provided to effortlessly handle collisions by taking collisions as events. An area can be masked (relative to Game Space or a Game Object). A class that extends from `JGameEngine.Object` and implements `JGameEngine.Collision` can create a collisionMask and add it to the Game Space. When something touches the Game Object after this, the implementable function `void collision(Object with)` is called. Only rectangular and circular masks are currently supported.
Sample code:
    
```java
class Box extends JGameEngine.Object implements JGameEngine.Collision {
    JGameEngine e;
    public int w, h;
    public Box(JGameEngine e, int x, int y, int w, int h) {
        this.e = e;	
        this.x = x; this.y = y;
        this.w = w; this.h = h;
        name = "Box";
    }
    @Override public void start() { e.collisionMaskAdd(this, 0, 0, w/2); }
    @Override public void update() {
        e.drawOval(x, y, w, h);
    }
    @Override public void collision(JGameEngine.Object with) {
        System.out.println(this.name + " is touching " + with.name);
    }
}
```
|   |   |
|--|--|
|  `CollisionMask collisionMaskAdd(Object obj, double x, double y, double r)` | `CollisionMask collisionMaskAdd(Object obj, double x, double y, double w, double h)` |
|  `void collisionMaskRemove(CollisionMask m)` | `void collisionMaskDebug()` |
|  `ArrayList<CollisionMask> collisionBoxTest(int x, int y, int w, int h)` |  |

###  Misc
These don't belong to a category
|   |   |
|--|--|
|  `int frameDelay()` | `void frameDelay(int delay)` |
|  `void setBackground(Color c)` | `double fps()` |
|  `int screenWidth()` | `int screenHeight()` |
|  `Color color(int r, int g, int b)` | `Color color(int r, int g, int b, int a)` |

## Questions
**Bugs**
Yes there are many. Some this code is untested (new features breaking previously working features).

**wHEre iS OOP?**
For readability, all of the Object Orientation is hidden using Wrappers. This simplicity is inspired by Game Maker Studio's GML.

## License
This project is licensed under the MIT License - see the LICENSE.md file for details

## Acknowledgments
[Collisions - Quadtrees on tutsplus](https://gamedevelopment.tutsplus.com/tutorials/quick-tip-use-quadtrees-to-detect-likely-collisions-in-2d-space--gamedev-374)

[Collisions - Mozilla](https://developer.mozilla.org/en-US/docs/Games/Techniques/2D_collision_detection)

[Collisions - Stackoverflow](https://stackoverflow.com/questions/401847/circle-rectangle-collision-detection-intersection)

[Audio - geeksforgeeks](https://www.geeksforgeeks.org/play-audio-file-using-java/)
