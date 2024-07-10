# Basic .unitypackage Viewer

I just wanted a simple tool to do the following:

* Browse the tree of a `.unitypackage` file
* Extract some individual files
* All without having to start Unity

So I threw this together for my own use. Maybe it could be helpful to others.

This is a Java program requiring Java 8 or higher.


# How to use

![](.github/BupV.png?raw=true)

* Run with Java
* Accepts a `.unitypackage` as a program argument to open immediately
* Open `.unitypackage` files using the Open button, or drag and drop a `.unitypackage` onto the window
* Search by name or GUID in the text box and press Enter or the Search button
* Expand/Collapse the tree
* Files with a preview will appear in the preview box
* Extract the selected item into the same directory as the `.unitypackage`.
  The directory will be opened in your OS file viewer.
* Right-click to copy name, size, or GUID
* History.ini saves the last directory used


# Disclaimers

* This is *beta* quality software. No rigorous testing has been done. It probably has bugs. Only tested with data as I make use of it.

* All I know about the `.unitypackage` file format came from examining several files, and reading code from some existing tools.
I didn't read any specification (assuming there is one). There could certainly be edge cases this misses.

# The `.unitypackage` file format

A `.unitypackage` is just a compressed TAR archive.
Inside it contains several root directories.
Each directory name is a GUID.

![](.github/tar-directories.png?raw=true)


Inside each of them is the contents of the asset with that GUID.
Specifically, each GUID directory contains some of these 4 files.

![](.github/tar-directory-contents.png?raw=true)

* `pathname` First line of text is the full path of the asset where it will appear when imported into Unity.
* `asset.meta` The corresponding .meta file.
* `asset` Contains the actual asset payload. Won't exist for directories.
* `preview.png` Optional preview of some types of assets.


It's not difficult to build up the structure of what's shown when you import into unity.

As shown in Unity:

![](.github/Unity.png?raw=true)

As shown in this tool:

![](.github/BupV.png?raw=true)

# Existing open source tools I found

### https://gist.github.com/yasirkula/dfc43134fbfefb820d0adbc5d7c25fb3

A very nice Unity script to explore `.unitypackages`.

### https://github.com/Switch-9867/UnitypackgeExtractor

C# command-line tool.

### https://github.com/Cobertos/unitypackage_extractor

Python command-line tool. Its associated pypi package at https://pypi.org/project/unitypackage-extractor/

