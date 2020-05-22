
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

## Included components and their methods
Everything can be classified into the following different components.

### Window
A window is a JFrame but it has additional properties. The window is updated after every `frameDelay` milliseconds. on every update, all objects in the Game Space are updated as well and the frame of the `Sprite` is updated.  The delay between each frame can se accessed by `deltaTime` and should be multiplied with movement/physics variables to have smooth movement. A window may not see the entirety of the Game Space. This concept is discussed in Camera section.
|   |   |
|--|--|
| [`void setWindow(String title, int x, int y, int width, int height)`](#) <br/> Set the window to be used. | [`void setGameSpace(int room_width, int room_height)`](#) <br/> Set the dimensions of the map. |
| [`void windowWidth()`](#) <br/> Get the raw window width. | [`void windowHeight()`](#) <br/> Get the raw window height. |
| [`void windowResizable(Boolean stance)`](#) <br/> Disable/enable window resizing. | [`void windowFullScreen(Boolean stance)`](#) <br/> Disable/enabe fullscreen. |

### Object
Classes may be extended to `GameEngine.Object` to have the properties `x`, `y` and `name` available. Objects must implement `start()` and `update()` methods. Objects need to be added to the Game Space in order to have these methods automatically invoke on every Game Update.
|   |   |
|--|--|
| [`void addObject(JGameEngine.Object)`](#) <br/> Add an object to the Game Space. | [`void removeObject(JGameEngine.Object)`](#) <br/> Remove an object to the Game Space. |

