# LiveUML
[![License: MIT](https://img.shields.io/github/license/mku11/LiveUML.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-0.9.2-blue)](https://github.com/mku11/LiveUML/releases)

Generate UML Class Diagrams from Java source code.  
This is a tool I use to validate my design and technical specs during development.  
It's still in its infancy but it's been very helpful for what it offers.  
I tried to put more flair with the dark mode from FlatLaf and added more features.  

![alt text](https://github.com/mku11/LiveUML/blob/main/screenshots/Screenshot.png)  

# Features
- Create UML Diagrams from Java source code
- Most common UML relationships are supported
- Displays fields, constructors, and methods
- Support for access modifiers by visibility
- Support for modifiers by stereotypes
- Highlight fields and methods in relationship
- Partial support for generics
- Allow multiple source roots
- Allow change UML objects position
- Classes list for easier selection
- Compact display option for UML classes  
- Find class, field, and method references  
- Open text editor at specific class, field, method (Notepad++, IDEA, and Eclipse supported)  
- Save/Load UML Diagram  
- Export as Image (experimental)  
  
# Specs
- Provides a custom meta data model for Java classes, fields, and methods  
- Uses force layout algorithm by for better class layout  
- Uses GraphML format for saving diagrams  
- Uses PNG format for image export  
  
# Dependencies:
- [JavaParser](https://github.com/javaparser/javaparser)  
- [JGraphT](https://github.com/jgrapht/jgrapht)  
- [JUNGRAPHT-VISUALIZATION](https://github.com/tomnelson/jungrapht-visualization)  
- [FlatLaf](https://www.formdev.com/flatlaf/)  
- [Gson](https://github.com/google/gson)  

# License
LiveUML is released under MIT Licence, see [LICENSE](https://github.com/mku11/LiveUML/blob/main/LICENSE) file.  
Make sure you read the LICENSE file and display proper attribution if you decide to use this software.  
Dependency libraries from Github, Maven, and NuGet are covered by their own license  
see [NOTICE](https://github.com/mku11/LiveUML/blob/main/NOTICE)  
Icons provided by https://uxwing.com license: https://uxwing.com/license/  
