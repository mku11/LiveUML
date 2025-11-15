/*
MIT License

Copyright (c) 2024 Max Kas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.mku.liveuml.view;

import java.awt.geom.Path2D;

public class Shapes {

    public static class Diamond extends Path2D.Double {
        public Diamond(double width, double height) {
            moveTo(0, 0);
            lineTo(-width / 2, -height / 2);
            lineTo(-width, 0);
            lineTo(-width / 2, height / 2);
            closePath();
        }
    }

    public static class OpenArrow extends Path2D.Double {
        public OpenArrow(double width, double height) {
            moveTo(0, 0);
            lineTo(-width / 2, -height / 2);
            lineTo(0, 0);
            lineTo(-width / 2, height / 2);
            closePath();
        }
    }

    public static class ClosedArrow extends Path2D.Double {
        public ClosedArrow(double width, double height) {
            moveTo(0, 0);
            lineTo(-width / 2, -height / 2);
            lineTo(-width / 2, height / 2);
            closePath();
        }
    }
}
