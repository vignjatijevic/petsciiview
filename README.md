# PETSCII View

*PETSCII View* is a custom view library for **Android** which "simulates" a C64 text-mode screen. The appropriate fonts, colors and charsets are already embedded into the library to assure everything looks exactly as on the good old home computer.

![PETSCIIView Sample Screenshot 1](http://garageapps.org/wp-content/gallery/petsciiview_android/petsciiview_android_01.png)



# Requirements
*PETSCII View* requires **API Level 14 (Android 4.0 ICS)** or above.



# Usage
For a simple implementation of the library see the `sample/` folder.

Adding *PETSCII View* to project:

**build.gradle**
```XML
dependencies {
    compile 'org.garageapps:petsciiview:1.0.0'
}
```

Adding *PETSCII View* to layout:

**my_layout.xml**
```XML
<org.garageapps.android.petsciiview.PETSCIIView
	android:id="@+id/petsciiView"
	android:layout_width="wrap_content"
	android:layout_height="wrap_content"/>
```

*Note: the layout size is always ignored since the view resizes itself automatically depending on defined border, screen and font sizes.*

Adding a callback listener to *PETSCII View* from code:

**MyActivity.java**
```JAVA
PETSCIIView pv = (PETSCIIView) findViewById(R.id.petsciiView);
pv.setListener(new PETSCIIView.PETSCIIListener() {
    @Override
    public void onClick(int action, int x, int y) {
        if (action == MotionEvent.ACTION_UP) {
        	// your code here
        }
    }
});
```

*Note: the view returns coordinates of the character or -1 if the border was clicked*



# Customization

*PETSCII View* default attributes match with the original C64 setup. The screen is 40 by 25 characters big, the border size is 4 characters by 4 and a quarter characters big; border color and cursor color are set to light blue, and the background color is set to blue.

Here is a list of all supported layout attributes:

* `pet_attrScreenWidth` - screen width
* `pet_attrScreenHeight` - screen height
* `pet_attrFontSize` - font size
* `pet_attrBorderSizeLeft` - border left size
* `pet_attrBorderSizeTop` - border top size
* `pet_attrBorderSizeRight` - border right size
* `pet_attrBorderSizeBottom` - border bottom size
* `pet_attrBorderColor` - border color
* `pet_attrBackgroundColor` - background color
* `pet_attrCursorColor` - cursor color
* `pet_attrTestPicture` - if true, a test picture is shown to help determining the best view size

All attributes except for the last one have getters and setters, so they can be changed programatically during runtime.

*Note: following attributes will trigger a complete view reset and measurement: screen width, screen height, font size. Changing border size will trigger only view measurement.*



# Displaying text

*PETSCII View* works, like a real C64, with two buffers, one for characters and one for the colors. That means to place a character with a specific color on the screen `putChar` and `putColor` methods must be called on the same screen position. Of course both methods can be used independently to achieve various text or color cycling effects.

```JAVA
// place a red character at the bottom right corner of the screen
pv.putChar('A', 39, 24);
pv.putColor(2, 39, 24);
```

There are also two other methods, `printText` and `printFormattedText` which work just like the BASIC command PRINT and can be used to display whole strings with a specific color on the screen.

```JAVA
// print a light blue string at the top left corner of the screen
pv.printText("Hello world", 0, 0, 14);

// print text also supports line breaks
pv.printText("Hello\nworld", 0, 0, 14);
```

*Note: whenever a screen (frame) is ready for displaying, the `invalidate` method should be called on the view manually (avoids calling `onDraw` on every buffer change).* 



# Using formatters

Anyone who typed BASIC programs (listings) from various computer magazines back in the eighties remembers that some PRINT commands in those listings contained additional shortcuts like: move cursor ten times to the left or change cursor color to yellow. The `printFormattedText` method can parse those extra commands to allow easy cursor moving or color changing using just one print call.

```JAVA
// clear screen and print a light blue string at the top left corner
pv.printFormattedText("{CLR}{COL=14}Hello", 0, 0, 14);

// change color to red and write the rest of the string reversed
pv.printFormattedText("{COL=02}{RON}world{ROF}", 0, 1, 14);
```

Here is a list of all supported formatters:

* `{CLR}` - clear screen
* `{HOM}` - move cursor to home (upper left corner of the screen)
* `{CUP=xx}` - move cursor up xx times
* `{CDN=xx}` - move cursor down xx times
* `{CLT=xx}` - move cursor left xx times
* `{CRT=xx}` - move cursor right xx times
* `{COL=xx}` - set cursor color to xx
* `{RON}` - set reverse on
* `{ROF}` - set reverse off

*Note: formatter values must always have two digits; for example '2' should be written as '02'. If an error occurs during parsing, it will show up at the top of the screen.*



# Buffers

Like already mentioned, *PETSCII View* uses two buffers. Both buffers can be enabled or disabled programatically during runtime. To turn off the screen buffer, `setScreenRamEnabled` must be called with the appropriate parameter and then only the border will be visible (the C64 uses this feature when accessing the datasette). To turn off the color buffer, `setColorRamEnabled` must be called and then all text on screen will be shown using only the current cursor color. There are also two additional methods for quickly clearing each of the buffers (completely or just a portion) with a specific character or color. These methods are `fillWithChar` and `fillWithColor`.

```JAVA
// fill whole screen with a character
pv.fillWithChar('A');

// fill upper left portion of the screen with red color
pv.fillWithColor(2, 0, 0, pv.getScreenWidth / 2, pv.getScreenHeight / 2);
```


# Changelog
* 1.0.0 - Initial release



# Credits
* Developed by: [Vladimir Ignjatijevic](mailto:igvlada@gmail.com)
* C64 TrueType font by: [Style64](http://style64.org/c64-truetype)
* C64 references available at: [C64-Wiki](https://www.c64-wiki.com)



# License
	Copyright 2016 Vladimir Ignjatijevic

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
