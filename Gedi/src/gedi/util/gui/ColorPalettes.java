/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

package gedi.util.gui;

import gedi.util.functions.EI;
import gedi.util.math.stat.factor.Factor;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;


/**
 * GGplot2 color palettes; adopted from https://github.com/beastmachine
 * @author erhard
 *
 */
public enum ColorPalettes {

	BrBG(new Color[][]{
			{ },
			{ new Color(216, 179, 101) },
			{ new Color(216, 179, 101), new Color(90, 180, 172) },
			{ new Color(216, 179, 101), new Color(245, 245, 245), new Color(90, 180, 172) },
			{ new Color(166, 97, 26), new Color(223, 194, 125), new Color(128, 205, 193), new Color(1, 133, 113) },
			{ new Color(166, 97, 26), new Color(223, 194, 125), new Color(245, 245, 245), new Color(128, 205, 193), new Color(1, 133, 113) },
			{ new Color(140, 81, 10), new Color(216, 179, 101), new Color(246, 232, 195), new Color(199, 234, 229), new Color(90, 180, 172), new Color(1, 102, 94) },
			{ new Color(140, 81, 10), new Color(216, 179, 101), new Color(246, 232, 195), new Color(245, 245, 245), new Color(199, 234, 229), new Color(90, 180, 172), new Color(1, 102, 94) },
			{ new Color(140, 81, 10), new Color(191, 129, 45), new Color(223, 194, 125), new Color(246, 232, 195), new Color(199, 234, 229), new Color(128, 205, 193), new Color(53, 151, 143), new Color(1, 102, 94) },
			{ new Color(140, 81, 10), new Color(191, 129, 45), new Color(223, 194, 125), new Color(246, 232, 195), new Color(245, 245, 245), new Color(199, 234, 229), new Color(128, 205, 193), new Color(53, 151, 143), new Color(1, 102, 94) },
			{ new Color(84, 48, 5), new Color(140, 81, 10), new Color(191, 129, 45), new Color(223, 194, 125), new Color(246, 232, 195), new Color(199, 234, 229), new Color(128, 205, 193), new Color(53, 151, 143), new Color(1, 102, 94), new Color(0, 60, 48) },
			{ new Color(84, 48, 5), new Color(140, 81, 10), new Color(191, 129, 45), new Color(223, 194, 125), new Color(246, 232, 195), new Color(245, 245, 245), new Color(199, 234, 229), new Color(128, 205, 193), new Color(53, 151, 143), new Color(1, 102, 94), new Color(0, 60, 48) },
	}),

	PiYG(new Color[][]{
			{  },
			{ new Color(233, 163, 201) },
			{ new Color(233, 163, 201), new Color(161, 215, 106) },
			{ new Color(233, 163, 201), new Color(247, 247, 247), new Color(161, 215, 106) },
			{ new Color(208, 28, 139), new Color(241, 182, 218), new Color(184, 225, 134), new Color(77, 172, 38) },
			{ new Color(208, 28, 139), new Color(241, 182, 218), new Color(247, 247, 247), new Color(184, 225, 134), new Color(77, 172, 38) },
			{ new Color(197, 27, 125), new Color(233, 163, 201), new Color(253, 224, 239), new Color(230, 245, 208), new Color(161, 215, 106), new Color(77, 146, 33) },
			{ new Color(197, 27, 125), new Color(233, 163, 201), new Color(253, 224, 239), new Color(247, 247, 247), new Color(230, 245, 208), new Color(161, 215, 106), new Color(77, 146, 33) },
			{ new Color(197, 27, 125), new Color(222, 119, 174), new Color(241, 182, 218), new Color(253, 224, 239), new Color(230, 245, 208), new Color(184, 225, 134), new Color(127, 188, 65), new Color(77, 146, 33) },
			{ new Color(197, 27, 125), new Color(222, 119, 174), new Color(241, 182, 218), new Color(253, 224, 239), new Color(247, 247, 247), new Color(230, 245, 208), new Color(184, 225, 134), new Color(127, 188, 65), new Color(77, 146, 33) },
			{ new Color(142, 1, 82), new Color(197, 27, 125), new Color(222, 119, 174), new Color(241, 182, 218), new Color(253, 224, 239), new Color(230, 245, 208), new Color(184, 225, 134), new Color(127, 188, 65), new Color(77, 146, 33), new Color(39, 100, 25) },
			{ new Color(142, 1, 82), new Color(197, 27, 125), new Color(222, 119, 174), new Color(241, 182, 218), new Color(253, 224, 239), new Color(247, 247, 247), new Color(230, 245, 208), new Color(184, 225, 134), new Color(127, 188, 65), new Color(77, 146, 33), new Color(39, 100, 25) },
	}),

	PRGn(new Color[][]{
			{ },
			{ new Color(175, 141, 195) },
			{ new Color(175, 141, 195), new Color(127, 191, 123) },
			{ new Color(175, 141, 195), new Color(247, 247, 247), new Color(127, 191, 123) },
			{ new Color(123, 50, 148), new Color(194, 165, 207), new Color(166, 219, 160), new Color(0, 136, 55) },
			{ new Color(123, 50, 148), new Color(194, 165, 207), new Color(247, 247, 247), new Color(166, 219, 160), new Color(0, 136, 55) },
			{ new Color(118, 42, 131), new Color(175, 141, 195), new Color(231, 212, 232), new Color(217, 240, 211), new Color(127, 191, 123), new Color(27, 120, 55) },
			{ new Color(118, 42, 131), new Color(175, 141, 195), new Color(231, 212, 232), new Color(247, 247, 247), new Color(217, 240, 211), new Color(127, 191, 123), new Color(27, 120, 55) },
			{ new Color(118, 42, 131), new Color(153, 112, 171), new Color(194, 165, 207), new Color(231, 212, 232), new Color(217, 240, 211), new Color(166, 219, 160), new Color(90, 174, 97), new Color(27, 120, 55) },
			{ new Color(118, 42, 131), new Color(153, 112, 171), new Color(194, 165, 207), new Color(231, 212, 232), new Color(247, 247, 247), new Color(217, 240, 211), new Color(166, 219, 160), new Color(90, 174, 97), new Color(27, 120, 55) },
			{ new Color(64, 0, 75), new Color(118, 42, 131), new Color(153, 112, 171), new Color(194, 165, 207), new Color(231, 212, 232), new Color(217, 240, 211), new Color(166, 219, 160), new Color(90, 174, 97), new Color(27, 120, 55), new Color(0, 68, 27) },
			{ new Color(64, 0, 75), new Color(118, 42, 131), new Color(153, 112, 171), new Color(194, 165, 207), new Color(231, 212, 232), new Color(247, 247, 247), new Color(217, 240, 211), new Color(166, 219, 160), new Color(90, 174, 97), new Color(27, 120, 55), new Color(0, 68, 27) },
	}),

	PuOr(new Color[][]{
			{ },
			{ new Color(241, 163, 64)},
			{ new Color(241, 163, 64), new Color(153, 142, 195) },
			{ new Color(241, 163, 64), new Color(247, 247, 247), new Color(153, 142, 195) },
			{ new Color(230, 97, 1), new Color(253, 184, 99), new Color(178, 171, 210), new Color(94, 60, 153) },
			{ new Color(230, 97, 1), new Color(253, 184, 99), new Color(247, 247, 247), new Color(178, 171, 210), new Color(94, 60, 153) },
			{ new Color(179, 88, 6), new Color(241, 163, 64), new Color(254, 224, 182), new Color(216, 218, 235), new Color(153, 142, 195), new Color(84, 39, 136) },
			{ new Color(179, 88, 6), new Color(241, 163, 64), new Color(254, 224, 182), new Color(247, 247, 247), new Color(216, 218, 235), new Color(153, 142, 195), new Color(84, 39, 136) },
			{ new Color(179, 88, 6), new Color(224, 130, 20), new Color(253, 184, 99), new Color(254, 224, 182), new Color(216, 218, 235), new Color(178, 171, 210), new Color(128, 115, 172), new Color(84, 39, 136) },
			{ new Color(179, 88, 6), new Color(224, 130, 20), new Color(253, 184, 99), new Color(254, 224, 182), new Color(247, 247, 247), new Color(216, 218, 235), new Color(178, 171, 210), new Color(128, 115, 172), new Color(84, 39, 136) },
			{ new Color(127, 59, 8), new Color(179, 88, 6), new Color(224, 130, 20), new Color(253, 184, 99), new Color(254, 224, 182), new Color(216, 218, 235), new Color(178, 171, 210), new Color(128, 115, 172), new Color(84, 39, 136), new Color(45, 0, 75) },
			{ new Color(127, 59, 8), new Color(179, 88, 6), new Color(224, 130, 20), new Color(253, 184, 99), new Color(254, 224, 182), new Color(247, 247, 247), new Color(216, 218, 235), new Color(178, 171, 210), new Color(128, 115, 172), new Color(84, 39, 136), new Color(45, 0, 75) },
	}),

	RdBu(new Color[][]{
			{  },
			{ new Color(239, 138, 98) },
			{ new Color(239, 138, 98), new Color(103, 169, 207) },
			{ new Color(239, 138, 98), new Color(247, 247, 247), new Color(103, 169, 207) },
			{ new Color(202, 0, 32), new Color(244, 165, 130), new Color(146, 197, 222), new Color(5, 113, 176) },
			{ new Color(202, 0, 32), new Color(244, 165, 130), new Color(247, 247, 247), new Color(146, 197, 222), new Color(5, 113, 176) },
			{ new Color(178, 24, 43), new Color(239, 138, 98), new Color(253, 219, 199), new Color(209, 229, 240), new Color(103, 169, 207), new Color(33, 102, 172) },
			{ new Color(178, 24, 43), new Color(239, 138, 98), new Color(253, 219, 199), new Color(247, 247, 247), new Color(209, 229, 240), new Color(103, 169, 207), new Color(33, 102, 172) },
			{ new Color(178, 24, 43), new Color(214, 96, 77), new Color(244, 165, 130), new Color(253, 219, 199), new Color(209, 229, 240), new Color(146, 197, 222), new Color(67, 147, 195), new Color(33, 102, 172) },
			{ new Color(178, 24, 43), new Color(214, 96, 77), new Color(244, 165, 130), new Color(253, 219, 199), new Color(247, 247, 247), new Color(209, 229, 240), new Color(146, 197, 222), new Color(67, 147, 195), new Color(33, 102, 172) },
			{ new Color(103, 0, 31), new Color(178, 24, 43), new Color(214, 96, 77), new Color(244, 165, 130), new Color(253, 219, 199), new Color(209, 229, 240), new Color(146, 197, 222), new Color(67, 147, 195), new Color(33, 102, 172), new Color(5, 48, 97) },
			{ new Color(103, 0, 31), new Color(178, 24, 43), new Color(214, 96, 77), new Color(244, 165, 130), new Color(253, 219, 199), new Color(247, 247, 247), new Color(209, 229, 240), new Color(146, 197, 222), new Color(67, 147, 195), new Color(33, 102, 172), new Color(5, 48, 97) },
	}),

	RdGy(new Color[][]{
			{ },
			{ new Color(239, 138, 98) },
			{ new Color(239, 138, 98), new Color(153, 153, 153) },
			{ new Color(239, 138, 98), new Color(255, 255, 255), new Color(153, 153, 153) },
			{ new Color(202, 0, 32), new Color(244, 165, 130), new Color(186, 186, 186), new Color(64, 64, 64) },
			{ new Color(202, 0, 32), new Color(244, 165, 130), new Color(255, 255, 255), new Color(186, 186, 186), new Color(64, 64, 64) },
			{ new Color(178, 24, 43), new Color(239, 138, 98), new Color(253, 219, 199), new Color(224, 224, 224), new Color(153, 153, 153), new Color(77, 77, 77) },
			{ new Color(178, 24, 43), new Color(239, 138, 98), new Color(253, 219, 199), new Color(255, 255, 255), new Color(224, 224, 224), new Color(153, 153, 153), new Color(77, 77, 77) },
			{ new Color(178, 24, 43), new Color(214, 96, 77), new Color(244, 165, 130), new Color(253, 219, 199), new Color(224, 224, 224), new Color(186, 186, 186), new Color(135, 135, 135), new Color(77, 77, 77) },
			{ new Color(178, 24, 43), new Color(214, 96, 77), new Color(244, 165, 130), new Color(253, 219, 199), new Color(255, 255, 255), new Color(224, 224, 224), new Color(186, 186, 186), new Color(135, 135, 135), new Color(77, 77, 77) },
			{ new Color(103, 0, 31), new Color(178, 24, 43), new Color(214, 96, 77), new Color(244, 165, 130), new Color(253, 219, 199), new Color(224, 224, 224), new Color(186, 186, 186), new Color(135, 135, 135), new Color(77, 77, 77), new Color(26, 26, 26) },
			{ new Color(103, 0, 31), new Color(178, 24, 43), new Color(214, 96, 77), new Color(244, 165, 130), new Color(253, 219, 199), new Color(255, 255, 255), new Color(224, 224, 224), new Color(186, 186, 186), new Color(135, 135, 135), new Color(77, 77, 77), new Color(26, 26, 26) },
	}),

	RdYlBu(new Color[][]{
			{ },
			{ new Color(252, 141, 89) },
			{ new Color(252, 141, 89), new Color(145, 191, 219) },
			{ new Color(252, 141, 89), new Color(255, 255, 191), new Color(145, 191, 219) },
			{ new Color(215, 25, 28), new Color(253, 174, 97), new Color(171, 217, 233), new Color(44, 123, 182) },
			{ new Color(215, 25, 28), new Color(253, 174, 97), new Color(255, 255, 191), new Color(171, 217, 233), new Color(44, 123, 182) },
			{ new Color(215, 48, 39), new Color(252, 141, 89), new Color(254, 224, 144), new Color(224, 243, 248), new Color(145, 191, 219), new Color(69, 117, 180) },
			{ new Color(215, 48, 39), new Color(252, 141, 89), new Color(254, 224, 144), new Color(255, 255, 191), new Color(224, 243, 248), new Color(145, 191, 219), new Color(69, 117, 180) },
			{ new Color(215, 48, 39), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 144), new Color(224, 243, 248), new Color(171, 217, 233), new Color(116, 173, 209), new Color(69, 117, 180) },
			{ new Color(215, 48, 39), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 144), new Color(255, 255, 191), new Color(224, 243, 248), new Color(171, 217, 233), new Color(116, 173, 209), new Color(69, 117, 180) },
			{ new Color(165, 0, 38), new Color(215, 48, 39), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 144), new Color(224, 243, 248), new Color(171, 217, 233), new Color(116, 173, 209), new Color(69, 117, 180), new Color(49, 54, 149) },
			{ new Color(165, 0, 38), new Color(215, 48, 39), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 144), new Color(255, 255, 191), new Color(224, 243, 248), new Color(171, 217, 233), new Color(116, 173, 209), new Color(69, 117, 180), new Color(49, 54, 149) },
	}),

	RdYlGn(new Color[][]{
			{  },
			{ new Color(252, 141, 89)},
			{ new Color(252, 141, 89), new Color(145, 207, 96) },
			{ new Color(252, 141, 89), new Color(255, 255, 191), new Color(145, 207, 96) },
			{ new Color(215, 25, 28), new Color(253, 174, 97), new Color(166, 217, 106), new Color(26, 150, 65) },
			{ new Color(215, 25, 28), new Color(253, 174, 97), new Color(255, 255, 191), new Color(166, 217, 106), new Color(26, 150, 65) },
			{ new Color(215, 48, 39), new Color(252, 141, 89), new Color(254, 224, 139), new Color(217, 239, 139), new Color(145, 207, 96), new Color(26, 152, 80) },
			{ new Color(215, 48, 39), new Color(252, 141, 89), new Color(254, 224, 139), new Color(255, 255, 191), new Color(217, 239, 139), new Color(145, 207, 96), new Color(26, 152, 80) },
			{ new Color(215, 48, 39), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 139), new Color(217, 239, 139), new Color(166, 217, 106), new Color(102, 189, 99), new Color(26, 152, 80) },
			{ new Color(215, 48, 39), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 139), new Color(255, 255, 191), new Color(217, 239, 139), new Color(166, 217, 106), new Color(102, 189, 99), new Color(26, 152, 80) },
			{ new Color(165, 0, 38), new Color(215, 48, 39), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 139), new Color(217, 239, 139), new Color(166, 217, 106), new Color(102, 189, 99), new Color(26, 152, 80), new Color(0, 104, 55) },
			{ new Color(165, 0, 38), new Color(215, 48, 39), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 139), new Color(255, 255, 191), new Color(217, 239, 139), new Color(166, 217, 106), new Color(102, 189, 99), new Color(26, 152, 80), new Color(0, 104, 55) },
	}),

	Spectral(new Color[][]{
			{ },
			{ new Color(252, 141, 89), },
			{ new Color(252, 141, 89), new Color(153, 213, 148) },
			{ new Color(252, 141, 89), new Color(255, 255, 191), new Color(153, 213, 148) },
			{ new Color(215, 25, 28), new Color(253, 174, 97), new Color(171, 221, 164), new Color(43, 131, 186) },
			{ new Color(215, 25, 28), new Color(253, 174, 97), new Color(255, 255, 191), new Color(171, 221, 164), new Color(43, 131, 186) },
			{ new Color(213, 62, 79), new Color(252, 141, 89), new Color(254, 224, 139), new Color(230, 245, 152), new Color(153, 213, 148), new Color(50, 136, 189) },
			{ new Color(213, 62, 79), new Color(252, 141, 89), new Color(254, 224, 139), new Color(255, 255, 191), new Color(230, 245, 152), new Color(153, 213, 148), new Color(50, 136, 189) },
			{ new Color(213, 62, 79), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 139), new Color(230, 245, 152), new Color(171, 221, 164), new Color(102, 194, 165), new Color(50, 136, 189) },
			{ new Color(213, 62, 79), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 139), new Color(255, 255, 191), new Color(230, 245, 152), new Color(171, 221, 164), new Color(102, 194, 165), new Color(50, 136, 189) },
			{ new Color(158, 1, 66), new Color(213, 62, 79), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 139), new Color(230, 245, 152), new Color(171, 221, 164), new Color(102, 194, 165), new Color(50, 136, 189), new Color(94, 79, 162) },
			{ new Color(158, 1, 66), new Color(213, 62, 79), new Color(244, 109, 67), new Color(253, 174, 97), new Color(254, 224, 139), new Color(255, 255, 191), new Color(230, 245, 152), new Color(171, 221, 164), new Color(102, 194, 165), new Color(50, 136, 189), new Color(94, 79, 162) },
	}),

	Accent(new Color[][]{
			{  },
			{ new Color(127, 201, 127) },
			{ new Color(127, 201, 127),new Color(253, 192, 134) },
			{ new Color(127, 201, 127), new Color(190, 174, 212), new Color(253, 192, 134) },
			{ new Color(127, 201, 127), new Color(190, 174, 212), new Color(253, 192, 134), new Color(255, 255, 153) },
			{ new Color(127, 201, 127), new Color(190, 174, 212), new Color(253, 192, 134), new Color(255, 255, 153), new Color(56, 108, 176) },
			{ new Color(127, 201, 127), new Color(190, 174, 212), new Color(253, 192, 134), new Color(255, 255, 153), new Color(56, 108, 176), new Color(240, 2, 127) },
			{ new Color(127, 201, 127), new Color(190, 174, 212), new Color(253, 192, 134), new Color(255, 255, 153), new Color(56, 108, 176), new Color(240, 2, 127), new Color(191, 91, 23) },
			{ new Color(127, 201, 127), new Color(190, 174, 212), new Color(253, 192, 134), new Color(255, 255, 153), new Color(56, 108, 176), new Color(240, 2, 127), new Color(191, 91, 23), new Color(102, 102, 102) },
	}),

	Dark2(new Color[][]{
			{  },
			{ new Color(27, 158, 119)},
			{ new Color(27, 158, 119), new Color(117, 112, 179) },
			{ new Color(27, 158, 119), new Color(217, 95, 2), new Color(117, 112, 179) },
			{ new Color(27, 158, 119), new Color(217, 95, 2), new Color(117, 112, 179), new Color(231, 41, 138) },
			{ new Color(27, 158, 119), new Color(217, 95, 2), new Color(117, 112, 179), new Color(231, 41, 138), new Color(102, 166, 30) },
			{ new Color(27, 158, 119), new Color(217, 95, 2), new Color(117, 112, 179), new Color(231, 41, 138), new Color(102, 166, 30), new Color(230, 171, 2) },
			{ new Color(27, 158, 119), new Color(217, 95, 2), new Color(117, 112, 179), new Color(231, 41, 138), new Color(102, 166, 30), new Color(230, 171, 2), new Color(166, 118, 29) },
			{ new Color(27, 158, 119), new Color(217, 95, 2), new Color(117, 112, 179), new Color(231, 41, 138), new Color(102, 166, 30), new Color(230, 171, 2), new Color(166, 118, 29), new Color(102, 102, 102) },
	}),

	Paired(new Color[][]{
			{ },
			{ new Color(166, 206, 227) },
			{ new Color(166, 206, 227), new Color(178, 223, 138) },
			{ new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138) },
			{ new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138), new Color(51, 160, 44) },
			{ new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138), new Color(51, 160, 44), new Color(251, 154, 153) },
			{ new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138), new Color(51, 160, 44), new Color(251, 154, 153), new Color(227, 26, 28) },
			{ new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138), new Color(51, 160, 44), new Color(251, 154, 153), new Color(227, 26, 28), new Color(253, 191, 111) },
			{ new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138), new Color(51, 160, 44), new Color(251, 154, 153), new Color(227, 26, 28), new Color(253, 191, 111), new Color(255, 127, 0) },
			{ new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138), new Color(51, 160, 44), new Color(251, 154, 153), new Color(227, 26, 28), new Color(253, 191, 111), new Color(255, 127, 0), new Color(202, 178, 214) },
			{ new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138), new Color(51, 160, 44), new Color(251, 154, 153), new Color(227, 26, 28), new Color(253, 191, 111), new Color(255, 127, 0), new Color(202, 178, 214), new Color(106, 61, 154) },
			{ new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138), new Color(51, 160, 44), new Color(251, 154, 153), new Color(227, 26, 28), new Color(253, 191, 111), new Color(255, 127, 0), new Color(202, 178, 214), new Color(106, 61, 154), new Color(255, 255, 153) },
			{ new Color(166, 206, 227), new Color(31, 120, 180), new Color(178, 223, 138), new Color(51, 160, 44), new Color(251, 154, 153), new Color(227, 26, 28), new Color(253, 191, 111), new Color(255, 127, 0), new Color(202, 178, 214), new Color(106, 61, 154), new Color(255, 255, 153), new Color(177, 89, 40) },
	}),

	Pastel1(new Color[][]{
			{ },
			{ new Color(251, 180, 174) },
			{ new Color(251, 180, 174), new Color(204, 235, 197) },
			{ new Color(251, 180, 174), new Color(179, 205, 227), new Color(204, 235, 197) },
			{ new Color(251, 180, 174), new Color(179, 205, 227), new Color(204, 235, 197), new Color(222, 203, 228) },
			{ new Color(251, 180, 174), new Color(179, 205, 227), new Color(204, 235, 197), new Color(222, 203, 228), new Color(254, 217, 166) },
			{ new Color(251, 180, 174), new Color(179, 205, 227), new Color(204, 235, 197), new Color(222, 203, 228), new Color(254, 217, 166), new Color(255, 255, 204) },
			{ new Color(251, 180, 174), new Color(179, 205, 227), new Color(204, 235, 197), new Color(222, 203, 228), new Color(254, 217, 166), new Color(255, 255, 204), new Color(229, 216, 189) },
			{ new Color(251, 180, 174), new Color(179, 205, 227), new Color(204, 235, 197), new Color(222, 203, 228), new Color(254, 217, 166), new Color(255, 255, 204), new Color(229, 216, 189), new Color(253, 218, 236) },
			{ new Color(251, 180, 174), new Color(179, 205, 227), new Color(204, 235, 197), new Color(222, 203, 228), new Color(254, 217, 166), new Color(255, 255, 204), new Color(229, 216, 189), new Color(253, 218, 236), new Color(242, 242, 242) },
	}),

	Pastel2(new Color[][]{
			{ },
			{ new Color(179, 226, 205) },
			{ new Color(179, 226, 205), new Color(203, 213, 232) },
			{ new Color(179, 226, 205), new Color(253, 205, 172), new Color(203, 213, 232) },
			{ new Color(179, 226, 205), new Color(253, 205, 172), new Color(203, 213, 232), new Color(244, 202, 228) },
			{ new Color(179, 226, 205), new Color(253, 205, 172), new Color(203, 213, 232), new Color(244, 202, 228), new Color(230, 245, 201) },
			{ new Color(179, 226, 205), new Color(253, 205, 172), new Color(203, 213, 232), new Color(244, 202, 228), new Color(230, 245, 201), new Color(255, 242, 174) },
			{ new Color(179, 226, 205), new Color(253, 205, 172), new Color(203, 213, 232), new Color(244, 202, 228), new Color(230, 245, 201), new Color(255, 242, 174), new Color(241, 226, 204) },
			{ new Color(179, 226, 205), new Color(253, 205, 172), new Color(203, 213, 232), new Color(244, 202, 228), new Color(230, 245, 201), new Color(255, 242, 174), new Color(241, 226, 204), new Color(204, 204, 204) },
	}),

	Set1(new Color[][]{
			{ },
			{ new Color(228, 26, 28) },
			{ new Color(228, 26, 28), new Color(77, 175, 74) },
			{ new Color(228, 26, 28), new Color(55, 126, 184), new Color(77, 175, 74) },
			{ new Color(228, 26, 28), new Color(55, 126, 184), new Color(77, 175, 74), new Color(152, 78, 163) },
			{ new Color(228, 26, 28), new Color(55, 126, 184), new Color(77, 175, 74), new Color(152, 78, 163), new Color(255, 127, 0) },
			{ new Color(228, 26, 28), new Color(55, 126, 184), new Color(77, 175, 74), new Color(152, 78, 163), new Color(255, 127, 0), new Color(255, 255, 51) },
			{ new Color(228, 26, 28), new Color(55, 126, 184), new Color(77, 175, 74), new Color(152, 78, 163), new Color(255, 127, 0), new Color(255, 255, 51), new Color(166, 86, 40) },
			{ new Color(228, 26, 28), new Color(55, 126, 184), new Color(77, 175, 74), new Color(152, 78, 163), new Color(255, 127, 0), new Color(255, 255, 51), new Color(166, 86, 40), new Color(247, 129, 191) },
			{ new Color(228, 26, 28), new Color(55, 126, 184), new Color(77, 175, 74), new Color(152, 78, 163), new Color(255, 127, 0), new Color(255, 255, 51), new Color(166, 86, 40), new Color(247, 129, 191), new Color(153, 153, 153) },
	}),

	Set2(new Color[][]{
			{ },
			{ new Color(102, 194, 165) },
			{ new Color(102, 194, 165), new Color(141, 160, 203) },
			{ new Color(102, 194, 165), new Color(252, 141, 98), new Color(141, 160, 203) },
			{ new Color(102, 194, 165), new Color(252, 141, 98), new Color(141, 160, 203), new Color(231, 138, 195) },
			{ new Color(102, 194, 165), new Color(252, 141, 98), new Color(141, 160, 203), new Color(231, 138, 195), new Color(166, 216, 84) },
			{ new Color(102, 194, 165), new Color(252, 141, 98), new Color(141, 160, 203), new Color(231, 138, 195), new Color(166, 216, 84), new Color(255, 217, 47) },
			{ new Color(102, 194, 165), new Color(252, 141, 98), new Color(141, 160, 203), new Color(231, 138, 195), new Color(166, 216, 84), new Color(255, 217, 47), new Color(229, 196, 148) },
			{ new Color(102, 194, 165), new Color(252, 141, 98), new Color(141, 160, 203), new Color(231, 138, 195), new Color(166, 216, 84), new Color(255, 217, 47), new Color(229, 196, 148), new Color(179, 179, 179) },
	}),

	Set3(new Color[][]{
			{ },
			{ new Color(141, 211, 199) },
			{ new Color(141, 211, 199), new Color(190, 186, 218) },
			{ new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218) },
			{ new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218), new Color(251, 128, 114) },
			{ new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218), new Color(251, 128, 114), new Color(128, 177, 211) },
			{ new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218), new Color(251, 128, 114), new Color(128, 177, 211), new Color(253, 180, 98) },
			{ new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218), new Color(251, 128, 114), new Color(128, 177, 211), new Color(253, 180, 98), new Color(179, 222, 105) },
			{ new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218), new Color(251, 128, 114), new Color(128, 177, 211), new Color(253, 180, 98), new Color(179, 222, 105), new Color(252, 205, 229) },
			{ new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218), new Color(251, 128, 114), new Color(128, 177, 211), new Color(253, 180, 98), new Color(179, 222, 105), new Color(252, 205, 229), new Color(217, 217, 217) },
			{ new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218), new Color(251, 128, 114), new Color(128, 177, 211), new Color(253, 180, 98), new Color(179, 222, 105), new Color(252, 205, 229), new Color(217, 217, 217), new Color(188, 128, 189) },
			{ new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218), new Color(251, 128, 114), new Color(128, 177, 211), new Color(253, 180, 98), new Color(179, 222, 105), new Color(252, 205, 229), new Color(217, 217, 217), new Color(188, 128, 189), new Color(204, 235, 197) },
			{ new Color(141, 211, 199), new Color(255, 255, 179), new Color(190, 186, 218), new Color(251, 128, 114), new Color(128, 177, 211), new Color(253, 180, 98), new Color(179, 222, 105), new Color(252, 205, 229), new Color(217, 217, 217), new Color(188, 128, 189), new Color(204, 235, 197), new Color(255, 237, 111) },
	}),

	Blues(new Color[][]{
			{ },
			{ new Color(222, 235, 247) },
			{ new Color(222, 235, 247), new Color(49, 130, 189) },
			{ new Color(222, 235, 247), new Color(158, 202, 225), new Color(49, 130, 189) },
			{ new Color(239, 243, 255), new Color(189, 215, 231), new Color(107, 174, 214), new Color(33, 113, 181) },
			{ new Color(239, 243, 255), new Color(189, 215, 231), new Color(107, 174, 214), new Color(49, 130, 189), new Color(8, 81, 156) },
			{ new Color(239, 243, 255), new Color(198, 219, 239), new Color(158, 202, 225), new Color(107, 174, 214), new Color(49, 130, 189), new Color(8, 81, 156) },
			{ new Color(239, 243, 255), new Color(198, 219, 239), new Color(158, 202, 225), new Color(107, 174, 214), new Color(66, 146, 198), new Color(33, 113, 181), new Color(8, 69, 148) },
			{ new Color(247, 251, 255), new Color(222, 235, 247), new Color(198, 219, 239), new Color(158, 202, 225), new Color(107, 174, 214), new Color(66, 146, 198), new Color(33, 113, 181), new Color(8, 69, 148) },
			{ new Color(247, 251, 255), new Color(222, 235, 247), new Color(198, 219, 239), new Color(158, 202, 225), new Color(107, 174, 214), new Color(66, 146, 198), new Color(33, 113, 181), new Color(8, 81, 156), new Color(8, 48, 107) },
	}),

	BuGn(new Color[][]{
			{ },
			{ new Color(229, 245, 249) },
			{ new Color(229, 245, 249), new Color(44, 162, 95) },
			{ new Color(229, 245, 249), new Color(153, 216, 201), new Color(44, 162, 95) },
			{ new Color(237, 248, 251), new Color(178, 226, 226), new Color(102, 194, 164), new Color(35, 139, 69) },
			{ new Color(237, 248, 251), new Color(178, 226, 226), new Color(102, 194, 164), new Color(44, 162, 95), new Color(0, 109, 44) },
			{ new Color(237, 248, 251), new Color(204, 236, 230), new Color(153, 216, 201), new Color(102, 194, 164), new Color(44, 162, 95), new Color(0, 109, 44) },
			{ new Color(237, 248, 251), new Color(204, 236, 230), new Color(153, 216, 201), new Color(102, 194, 164), new Color(65, 174, 118), new Color(35, 139, 69), new Color(0, 88, 36) },
			{ new Color(247, 252, 253), new Color(229, 245, 249), new Color(204, 236, 230), new Color(153, 216, 201), new Color(102, 194, 164), new Color(65, 174, 118), new Color(35, 139, 69), new Color(0, 88, 36) },
			{ new Color(247, 252, 253), new Color(229, 245, 249), new Color(204, 236, 230), new Color(153, 216, 201), new Color(102, 194, 164), new Color(65, 174, 118), new Color(35, 139, 69), new Color(0, 109, 44), new Color(0, 68, 27) },
	}),

	BuPu(new Color[][]{
			{ },
			{ new Color(224, 236, 244) },
			{ new Color(224, 236, 244), new Color(136, 86, 167) },
			{ new Color(224, 236, 244), new Color(158, 188, 218), new Color(136, 86, 167) },
			{ new Color(237, 248, 251), new Color(179, 205, 227), new Color(140, 150, 198), new Color(136, 65, 157) },
			{ new Color(237, 248, 251), new Color(179, 205, 227), new Color(140, 150, 198), new Color(136, 86, 167), new Color(129, 15, 124) },
			{ new Color(237, 248, 251), new Color(191, 211, 230), new Color(158, 188, 218), new Color(140, 150, 198), new Color(136, 86, 167), new Color(129, 15, 124) },
			{ new Color(237, 248, 251), new Color(191, 211, 230), new Color(158, 188, 218), new Color(140, 150, 198), new Color(140, 107, 177), new Color(136, 65, 157), new Color(110, 1, 107) },
			{ new Color(247, 252, 253), new Color(224, 236, 244), new Color(191, 211, 230), new Color(158, 188, 218), new Color(140, 150, 198), new Color(140, 107, 177), new Color(136, 65, 157), new Color(110, 1, 107) },
			{ new Color(247, 252, 253), new Color(224, 236, 244), new Color(191, 211, 230), new Color(158, 188, 218), new Color(140, 150, 198), new Color(140, 107, 177), new Color(136, 65, 157), new Color(129, 15, 124), new Color(77, 0, 75) },
	}),

	GnBu(new Color[][]{
			{ },
			{ new Color(224, 243, 219)},
			{ new Color(224, 243, 219), new Color(67, 162, 202) },
			{ new Color(224, 243, 219), new Color(168, 221, 181), new Color(67, 162, 202) },
			{ new Color(240, 249, 232), new Color(186, 228, 188), new Color(123, 204, 196), new Color(43, 140, 190) },
			{ new Color(240, 249, 232), new Color(186, 228, 188), new Color(123, 204, 196), new Color(67, 162, 202), new Color(8, 104, 172) },
			{ new Color(240, 249, 232), new Color(204, 235, 197), new Color(168, 221, 181), new Color(123, 204, 196), new Color(67, 162, 202), new Color(8, 104, 172) },
			{ new Color(240, 249, 232), new Color(204, 235, 197), new Color(168, 221, 181), new Color(123, 204, 196), new Color(78, 179, 211), new Color(43, 140, 190), new Color(8, 88, 158) },
			{ new Color(247, 252, 240), new Color(224, 243, 219), new Color(204, 235, 197), new Color(168, 221, 181), new Color(123, 204, 196), new Color(78, 179, 211), new Color(43, 140, 190), new Color(8, 88, 158) },
			{ new Color(247, 252, 240), new Color(224, 243, 219), new Color(204, 235, 197), new Color(168, 221, 181), new Color(123, 204, 196), new Color(78, 179, 211), new Color(43, 140, 190), new Color(8, 104, 172), new Color(8, 64, 129) },
	}),

	Greens(new Color[][]{
			{ },
			{ new Color(229, 245, 224) },
			{ new Color(229, 245, 224), new Color(49, 163, 84) },
			{ new Color(229, 245, 224), new Color(161, 217, 155), new Color(49, 163, 84) },
			{ new Color(237, 248, 233), new Color(186, 228, 179), new Color(116, 196, 118), new Color(35, 139, 69) },
			{ new Color(237, 248, 233), new Color(186, 228, 179), new Color(116, 196, 118), new Color(49, 163, 84), new Color(0, 109, 44) },
			{ new Color(237, 248, 233), new Color(199, 233, 192), new Color(161, 217, 155), new Color(116, 196, 118), new Color(49, 163, 84), new Color(0, 109, 44) },
			{ new Color(237, 248, 233), new Color(199, 233, 192), new Color(161, 217, 155), new Color(116, 196, 118), new Color(65, 171, 93), new Color(35, 139, 69), new Color(0, 90, 50) },
			{ new Color(247, 252, 245), new Color(229, 245, 224), new Color(199, 233, 192), new Color(161, 217, 155), new Color(116, 196, 118), new Color(65, 171, 93), new Color(35, 139, 69), new Color(0, 90, 50) },
			{ new Color(247, 252, 245), new Color(229, 245, 224), new Color(199, 233, 192), new Color(161, 217, 155), new Color(116, 196, 118), new Color(65, 171, 93), new Color(35, 139, 69), new Color(0, 109, 44), new Color(0, 68, 27) },
	}),

	Greys(new Color[][]{
			{ },
			{ new Color(240, 240, 240)},
			{ new Color(240, 240, 240), new Color(99, 99, 99) },
			{ new Color(240, 240, 240), new Color(189, 189, 189), new Color(99, 99, 99) },
			{ new Color(247, 247, 247), new Color(204, 204, 204), new Color(150, 150, 150), new Color(82, 82, 82) },
			{ new Color(247, 247, 247), new Color(204, 204, 204), new Color(150, 150, 150), new Color(99, 99, 99), new Color(37, 37, 37) },
			{ new Color(247, 247, 247), new Color(217, 217, 217), new Color(189, 189, 189), new Color(150, 150, 150), new Color(99, 99, 99), new Color(37, 37, 37) },
			{ new Color(247, 247, 247), new Color(217, 217, 217), new Color(189, 189, 189), new Color(150, 150, 150), new Color(115, 115, 115), new Color(82, 82, 82), new Color(37, 37, 37) },
			{ new Color(255, 255, 255), new Color(240, 240, 240), new Color(217, 217, 217), new Color(189, 189, 189), new Color(150, 150, 150), new Color(115, 115, 115), new Color(82, 82, 82), new Color(37, 37, 37) },
			{ new Color(255, 255, 255), new Color(240, 240, 240), new Color(217, 217, 217), new Color(189, 189, 189), new Color(150, 150, 150), new Color(115, 115, 115), new Color(82, 82, 82), new Color(37, 37, 37), new Color(0, 0, 0) },
	}),

	Oranges(new Color[][]{
			{ },
			{ new Color(254, 230, 206) },
			{ new Color(254, 230, 206), new Color(230, 85, 13) },
			{ new Color(254, 230, 206), new Color(253, 174, 107), new Color(230, 85, 13) },
			{ new Color(254, 237, 222), new Color(253, 190, 133), new Color(253, 141, 60), new Color(217, 71, 1) },
			{ new Color(254, 237, 222), new Color(253, 190, 133), new Color(253, 141, 60), new Color(230, 85, 13), new Color(166, 54, 3) },
			{ new Color(254, 237, 222), new Color(253, 208, 162), new Color(253, 174, 107), new Color(253, 141, 60), new Color(230, 85, 13), new Color(166, 54, 3) },
			{ new Color(254, 237, 222), new Color(253, 208, 162), new Color(253, 174, 107), new Color(253, 141, 60), new Color(241, 105, 19), new Color(217, 72, 1), new Color(140, 45, 4) },
			{ new Color(255, 245, 235), new Color(254, 230, 206), new Color(253, 208, 162), new Color(253, 174, 107), new Color(253, 141, 60), new Color(241, 105, 19), new Color(217, 72, 1), new Color(140, 45, 4) },
			{ new Color(255, 245, 235), new Color(254, 230, 206), new Color(253, 208, 162), new Color(253, 174, 107), new Color(253, 141, 60), new Color(241, 105, 19), new Color(217, 72, 1), new Color(166, 54, 3), new Color(127, 39, 4) },
	}),

	OrRd(new Color[][]{
			{ },
			{ new Color(254, 232, 200) },
			{ new Color(254, 232, 200), new Color(227, 74, 51) },
			{ new Color(254, 232, 200), new Color(253, 187, 132), new Color(227, 74, 51) },
			{ new Color(254, 240, 217), new Color(253, 204, 138), new Color(252, 141, 89), new Color(215, 48, 31) },
			{ new Color(254, 240, 217), new Color(253, 204, 138), new Color(252, 141, 89), new Color(227, 74, 51), new Color(179, 0, 0) },
			{ new Color(254, 240, 217), new Color(253, 212, 158), new Color(253, 187, 132), new Color(252, 141, 89), new Color(227, 74, 51), new Color(179, 0, 0) },
			{ new Color(254, 240, 217), new Color(253, 212, 158), new Color(253, 187, 132), new Color(252, 141, 89), new Color(239, 101, 72), new Color(215, 48, 31), new Color(153, 0, 0) },
			{ new Color(255, 247, 236), new Color(254, 232, 200), new Color(253, 212, 158), new Color(253, 187, 132), new Color(252, 141, 89), new Color(239, 101, 72), new Color(215, 48, 31), new Color(153, 0, 0) },
			{ new Color(255, 247, 236), new Color(254, 232, 200), new Color(253, 212, 158), new Color(253, 187, 132), new Color(252, 141, 89), new Color(239, 101, 72), new Color(215, 48, 31), new Color(179, 0, 0), new Color(127, 0, 0) },
	}),

	PuBu(new Color[][]{
			{ },
			{ new Color(236, 231, 242) },
			{ new Color(236, 231, 242), new Color(43, 140, 190) },
			{ new Color(236, 231, 242), new Color(166, 189, 219), new Color(43, 140, 190) },
			{ new Color(241, 238, 246), new Color(189, 201, 225), new Color(116, 169, 207), new Color(5, 112, 176) },
			{ new Color(241, 238, 246), new Color(189, 201, 225), new Color(116, 169, 207), new Color(43, 140, 190), new Color(4, 90, 141) },
			{ new Color(241, 238, 246), new Color(208, 209, 230), new Color(166, 189, 219), new Color(116, 169, 207), new Color(43, 140, 190), new Color(4, 90, 141) },
			{ new Color(241, 238, 246), new Color(208, 209, 230), new Color(166, 189, 219), new Color(116, 169, 207), new Color(54, 144, 192), new Color(5, 112, 176), new Color(3, 78, 123) },
			{ new Color(255, 247, 251), new Color(236, 231, 242), new Color(208, 209, 230), new Color(166, 189, 219), new Color(116, 169, 207), new Color(54, 144, 192), new Color(5, 112, 176), new Color(3, 78, 123) },
			{ new Color(255, 247, 251), new Color(236, 231, 242), new Color(208, 209, 230), new Color(166, 189, 219), new Color(116, 169, 207), new Color(54, 144, 192), new Color(5, 112, 176), new Color(4, 90, 141), new Color(2, 56, 88) },
	}),

	PuBuGn(new Color[][]{
			{ },
			{ new Color(236, 226, 240) },
			{ new Color(236, 226, 240), new Color(28, 144, 153) },
			{ new Color(236, 226, 240), new Color(166, 189, 219), new Color(28, 144, 153) },
			{ new Color(246, 239, 247), new Color(189, 201, 225), new Color(103, 169, 207), new Color(2, 129, 138) },
			{ new Color(246, 239, 247), new Color(189, 201, 225), new Color(103, 169, 207), new Color(28, 144, 153), new Color(1, 108, 89) },
			{ new Color(246, 239, 247), new Color(208, 209, 230), new Color(166, 189, 219), new Color(103, 169, 207), new Color(28, 144, 153), new Color(1, 108, 89) },
			{ new Color(246, 239, 247), new Color(208, 209, 230), new Color(166, 189, 219), new Color(103, 169, 207), new Color(54, 144, 192), new Color(2, 129, 138), new Color(1, 100, 80) },
			{ new Color(255, 247, 251), new Color(236, 226, 240), new Color(208, 209, 230), new Color(166, 189, 219), new Color(103, 169, 207), new Color(54, 144, 192), new Color(2, 129, 138), new Color(1, 100, 80) },
			{ new Color(255, 247, 251), new Color(236, 226, 240), new Color(208, 209, 230), new Color(166, 189, 219), new Color(103, 169, 207), new Color(54, 144, 192), new Color(2, 129, 138), new Color(1, 108, 89), new Color(1, 70, 54) },
	}),

	PuRd(new Color[][]{
			{  },
			{ new Color(231, 225, 239)},
			{ new Color(231, 225, 239), new Color(221, 28, 119) },
			{ new Color(231, 225, 239), new Color(201, 148, 199), new Color(221, 28, 119) },
			{ new Color(241, 238, 246), new Color(215, 181, 216), new Color(223, 101, 176), new Color(206, 18, 86) },
			{ new Color(241, 238, 246), new Color(215, 181, 216), new Color(223, 101, 176), new Color(221, 28, 119), new Color(152, 0, 67) },
			{ new Color(241, 238, 246), new Color(212, 185, 218), new Color(201, 148, 199), new Color(223, 101, 176), new Color(221, 28, 119), new Color(152, 0, 67) },
			{ new Color(241, 238, 246), new Color(212, 185, 218), new Color(201, 148, 199), new Color(223, 101, 176), new Color(231, 41, 138), new Color(206, 18, 86), new Color(145, 0, 63) },
			{ new Color(247, 244, 249), new Color(231, 225, 239), new Color(212, 185, 218), new Color(201, 148, 199), new Color(223, 101, 176), new Color(231, 41, 138), new Color(206, 18, 86), new Color(145, 0, 63) },
			{ new Color(247, 244, 249), new Color(231, 225, 239), new Color(212, 185, 218), new Color(201, 148, 199), new Color(223, 101, 176), new Color(231, 41, 138), new Color(206, 18, 86), new Color(152, 0, 67), new Color(103, 0, 31) },
	}),

	Purples(new Color[][]{
			{  },
			{ new Color(239, 237, 245) },
			{ new Color(239, 237, 245), new Color(117, 107, 177) },
			{ new Color(239, 237, 245), new Color(188, 189, 220), new Color(117, 107, 177) },
			{ new Color(242, 240, 247), new Color(203, 201, 226), new Color(158, 154, 200), new Color(106, 81, 163) },
			{ new Color(242, 240, 247), new Color(203, 201, 226), new Color(158, 154, 200), new Color(117, 107, 177), new Color(84, 39, 143) },
			{ new Color(242, 240, 247), new Color(218, 218, 235), new Color(188, 189, 220), new Color(158, 154, 200), new Color(117, 107, 177), new Color(84, 39, 143) },
			{ new Color(242, 240, 247), new Color(218, 218, 235), new Color(188, 189, 220), new Color(158, 154, 200), new Color(128, 125, 186), new Color(106, 81, 163), new Color(74, 20, 134) },
			{ new Color(252, 251, 253), new Color(239, 237, 245), new Color(218, 218, 235), new Color(188, 189, 220), new Color(158, 154, 200), new Color(128, 125, 186), new Color(106, 81, 163), new Color(74, 20, 134) },
			{ new Color(252, 251, 253), new Color(239, 237, 245), new Color(218, 218, 235), new Color(188, 189, 220), new Color(158, 154, 200), new Color(128, 125, 186), new Color(106, 81, 163), new Color(84, 39, 143), new Color(63, 0, 125) },
	}),

	RdPu(new Color[][]{
			{},
			{ new Color(253, 224, 221) },
			{ new Color(253, 224, 221), new Color(197, 27, 138) },
			{ new Color(253, 224, 221), new Color(250, 159, 181), new Color(197, 27, 138) },
			{ new Color(254, 235, 226), new Color(251, 180, 185), new Color(247, 104, 161), new Color(174, 1, 126) },
			{ new Color(254, 235, 226), new Color(251, 180, 185), new Color(247, 104, 161), new Color(197, 27, 138), new Color(122, 1, 119) },
			{ new Color(254, 235, 226), new Color(252, 197, 192), new Color(250, 159, 181), new Color(247, 104, 161), new Color(197, 27, 138), new Color(122, 1, 119) },
			{ new Color(254, 235, 226), new Color(252, 197, 192), new Color(250, 159, 181), new Color(247, 104, 161), new Color(221, 52, 151), new Color(174, 1, 126), new Color(122, 1, 119) },
			{ new Color(255, 247, 243), new Color(253, 224, 221), new Color(252, 197, 192), new Color(250, 159, 181), new Color(247, 104, 161), new Color(221, 52, 151), new Color(174, 1, 126), new Color(122, 1, 119) },
			{ new Color(255, 247, 243), new Color(253, 224, 221), new Color(252, 197, 192), new Color(250, 159, 181), new Color(247, 104, 161), new Color(221, 52, 151), new Color(174, 1, 126), new Color(122, 1, 119), new Color(73, 0, 106) },
	}),

	Reds(new Color[][]{
			{ },
			{ new Color(254, 224, 210)},
			{ new Color(254, 224, 210),new Color(222, 45, 38) },
			{ new Color(254, 224, 210), new Color(252, 146, 114), new Color(222, 45, 38) },
			{ new Color(254, 229, 217), new Color(252, 174, 145), new Color(251, 106, 74), new Color(203, 24, 29) },
			{ new Color(254, 229, 217), new Color(252, 174, 145), new Color(251, 106, 74), new Color(222, 45, 38), new Color(165, 15, 21) },
			{ new Color(254, 229, 217), new Color(252, 187, 161), new Color(252, 146, 114), new Color(251, 106, 74), new Color(222, 45, 38), new Color(165, 15, 21) },
			{ new Color(254, 229, 217), new Color(252, 187, 161), new Color(252, 146, 114), new Color(251, 106, 74), new Color(239, 59, 44), new Color(203, 24, 29), new Color(153, 0, 13) },
			{ new Color(255, 245, 240), new Color(254, 224, 210), new Color(252, 187, 161), new Color(252, 146, 114), new Color(251, 106, 74), new Color(239, 59, 44), new Color(203, 24, 29), new Color(153, 0, 13) },
			{ new Color(255, 245, 240), new Color(254, 224, 210), new Color(252, 187, 161), new Color(252, 146, 114), new Color(251, 106, 74), new Color(239, 59, 44), new Color(203, 24, 29), new Color(165, 15, 21), new Color(103, 0, 13) },
	}),

	YlGn(new Color[][]{
			{ },
			{ new Color(247, 252, 185) },
			{ new Color(247, 252, 185), new Color(49, 163, 84) },
			{ new Color(247, 252, 185), new Color(173, 221, 142), new Color(49, 163, 84) },
			{ new Color(255, 255, 204), new Color(194, 230, 153), new Color(120, 198, 121), new Color(35, 132, 67) },
			{ new Color(255, 255, 204), new Color(194, 230, 153), new Color(120, 198, 121), new Color(49, 163, 84), new Color(0, 104, 55) },
			{ new Color(255, 255, 204), new Color(217, 240, 163), new Color(173, 221, 142), new Color(120, 198, 121), new Color(49, 163, 84), new Color(0, 104, 55) },
			{ new Color(255, 255, 204), new Color(217, 240, 163), new Color(173, 221, 142), new Color(120, 198, 121), new Color(65, 171, 93), new Color(35, 132, 67), new Color(0, 90, 50) },
			{ new Color(255, 255, 229), new Color(247, 252, 185), new Color(217, 240, 163), new Color(173, 221, 142), new Color(120, 198, 121), new Color(65, 171, 93), new Color(35, 132, 67), new Color(0, 90, 50) },
			{ new Color(255, 255, 229), new Color(247, 252, 185), new Color(217, 240, 163), new Color(173, 221, 142), new Color(120, 198, 121), new Color(65, 171, 93), new Color(35, 132, 67), new Color(0, 104, 55), new Color(0, 69, 41) },
	}),

	YlGnBu(new Color[][]{
			{ },
			{ new Color(237, 248, 177) },
			{ new Color(237, 248, 177), new Color(44, 127, 184) },
			{ new Color(237, 248, 177), new Color(127, 205, 187), new Color(44, 127, 184) },
			{ new Color(255, 255, 204), new Color(161, 218, 180), new Color(65, 182, 196), new Color(34, 94, 168) },
			{ new Color(255, 255, 204), new Color(161, 218, 180), new Color(65, 182, 196), new Color(44, 127, 184), new Color(37, 52, 148) },
			{ new Color(255, 255, 204), new Color(199, 233, 180), new Color(127, 205, 187), new Color(65, 182, 196), new Color(44, 127, 184), new Color(37, 52, 148) },
			{ new Color(255, 255, 204), new Color(199, 233, 180), new Color(127, 205, 187), new Color(65, 182, 196), new Color(29, 145, 192), new Color(34, 94, 168), new Color(12, 44, 132) },
			{ new Color(255, 255, 217), new Color(237, 248, 177), new Color(199, 233, 180), new Color(127, 205, 187), new Color(65, 182, 196), new Color(29, 145, 192), new Color(34, 94, 168), new Color(12, 44, 132) },
			{ new Color(255, 255, 217), new Color(237, 248, 177), new Color(199, 233, 180), new Color(127, 205, 187), new Color(65, 182, 196), new Color(29, 145, 192), new Color(34, 94, 168), new Color(37, 52, 148), new Color(8, 29, 88) },
	}),

	YlOrBr(new Color[][]{
			{ },
			{ new Color(255, 247, 188)},
			{ new Color(255, 247, 188), new Color(217, 95, 14) },
			{ new Color(255, 247, 188), new Color(254, 196, 79), new Color(217, 95, 14) },
			{ new Color(255, 255, 212), new Color(254, 217, 142), new Color(254, 153, 41), new Color(204, 76, 2) },
			{ new Color(255, 255, 212), new Color(254, 217, 142), new Color(254, 153, 41), new Color(217, 95, 14), new Color(153, 52, 4) },
			{ new Color(255, 255, 212), new Color(254, 227, 145), new Color(254, 196, 79), new Color(254, 153, 41), new Color(217, 95, 14), new Color(153, 52, 4) },
			{ new Color(255, 255, 212), new Color(254, 227, 145), new Color(254, 196, 79), new Color(254, 153, 41), new Color(236, 112, 20), new Color(204, 76, 2), new Color(140, 45, 4) },
			{ new Color(255, 255, 229), new Color(255, 247, 188), new Color(254, 227, 145), new Color(254, 196, 79), new Color(254, 153, 41), new Color(236, 112, 20), new Color(204, 76, 2), new Color(140, 45, 4) },
			{ new Color(255, 255, 229), new Color(255, 247, 188), new Color(254, 227, 145), new Color(254, 196, 79), new Color(254, 153, 41), new Color(236, 112, 20), new Color(204, 76, 2), new Color(153, 52, 4), new Color(102, 37, 6) },
	}),

	YlOrRd(new Color[][]{
			{ },
			{ new Color(255, 237, 160) },
			{ new Color(255, 237, 160), new Color(240, 59, 32) },
			{ new Color(255, 237, 160), new Color(254, 178, 76), new Color(240, 59, 32) },
			{ new Color(255, 255, 178), new Color(254, 204, 92), new Color(253, 141, 60), new Color(227, 26, 28) },
			{ new Color(255, 255, 178), new Color(254, 204, 92), new Color(253, 141, 60), new Color(240, 59, 32), new Color(189, 0, 38) },
			{ new Color(255, 255, 178), new Color(254, 217, 118), new Color(254, 178, 76), new Color(253, 141, 60), new Color(240, 59, 32), new Color(189, 0, 38) },
			{ new Color(255, 255, 178), new Color(254, 217, 118), new Color(254, 178, 76), new Color(253, 141, 60), new Color(252, 78, 42), new Color(227, 26, 28), new Color(177, 0, 38) },
			{ new Color(255, 255, 204), new Color(255, 237, 160), new Color(254, 217, 118), new Color(254, 178, 76), new Color(253, 141, 60), new Color(252, 78, 42), new Color(227, 26, 28), new Color(177, 0, 38) },
			{ new Color(255, 255, 204), new Color(255, 237, 160), new Color(254, 217, 118), new Color(254, 178, 76), new Color(253, 141, 60), new Color(252, 78, 42), new Color(227, 26, 28), new Color(189, 0, 38), new Color(128, 0, 38) },
	});

	private static final String[] divNames = {
		"BrBG", "PiYG", "PRGn", "PuOr", "RdBu", "RdGy", "RdYlBu", "RdYlGn",
	"Spectral"};
	private static final String[] qualNames = {
		"Accent", "Dark2", "Paired", "Pastel1", "Pastel2", "Set1",
		"Set2", "Set3"
	};
	private static final String[] seqNames = {
		"Blues", "BuGn", "BuPu", "GnBu", "Greens", "Greys", "Oranges",
		"OrRd", "PuBu", "PuBuGn", "PuRd", "Purples", "RdPu", "Reds",
		"YlGn", "YlGnBu", "YlOrBr", "YlOrRd"    
	};
	
	public static final ColorPalettes[] qualitativePalettes = EI.wrap(qualNames).map(s->ColorPalettes.valueOf(s)).toArray(ColorPalettes.class);
	public static final ColorPalettes[] divergingPalettes = EI.wrap(divNames).map(s->ColorPalettes.valueOf(s)).toArray(ColorPalettes.class);
	public static final ColorPalettes[] sequentialPalettes = EI.wrap(seqNames).map(s->ColorPalettes.valueOf(s)).toArray(ColorPalettes.class);


	private Color[][] myColors;
	private ColorPalettes(Color[][] c) {
		myColors = c;
	}

	public int getMaxColors() {
		return myColors.length;
	}
	
	/**
	 * steps<0 means no discretization
	 * @param colors
	 * @param steps
	 * @return
	 */
	public DoubleFunction<Color> getContinuousMapper(int colors, int steps) {
		if (steps<0)
			return new ValueToColorMapper(getPalette(colors));
		
		DoubleUnaryOperator fun = v->Math.floor(v*steps)/(steps-1);
		return new ValueToColorMapper(fun, getPalette(colors));
	}

	public DoubleFunction<Color> getContinuousMapper(DoubleUnaryOperator fun, int colors) {
		return new ValueToColorMapper(fun, getPalette(colors));
	}

	public DoubleFunction<Color> getContinuousMapper(double[] breaks) {
		DoubleUnaryOperator fun = v->{
			int ind = Arrays.binarySearch(breaks, v);
			if (ind<0) 
				ind = -ind-1;
			return ind/(double)(breaks.length-1);
		};
		return new ValueToColorMapper(fun, getPalette(breaks.length-1));
	}
	
	public DoubleFunction<Color> getContinuousMapper() {
		return getContinuousMapper(2, -1);
	}
	
	public IntFunction<Color> getDiscreteMapper(int count) {
		if (count<myColors.length) {
			Color[] col = getPalette(count);
			return fa->col[fa];
		}
		return new ValueToColorMapper(getPalette(myColors.length-1)).toDiscreteMapper(count);
	}
	
	public IntFunction<Color> getCircularDiscreteMapper() {
		Color[] col = getPalette(myColors.length-1);
		return fa->col[fa%col.length];
	}
	
	public Function<Factor,Color> getFactorMapper(Factor f) {
		if (f.getLevels().length<myColors.length) {
			Color[] col = getPalette(f.getLevels().length);
			return fa->col[fa.getIndex()];
		}
		return new ValueToColorMapper(getPalette(myColors.length-1)).toFactorMapper(f);
	}

	public Color[] getPalette(int numColors) {
		if (numColors<myColors.length)
			return myColors[numColors];
		Color[] re = new Color[numColors];
		IntFunction<Color> mapper = new ValueToColorMapper(myColors[myColors.length-1]).toDiscreteMapper(numColors);
		for (int i=0; i<re.length; i++)
			re[i] = mapper.apply(i);
		return re;
	}
	public static Color[] get(String name, int numColors) {
		return ColorPalettes.valueOf(name).getPalette(numColors);
	}
	public static Color[] diverging(int index, int numColors) {
		return ColorPalettes.valueOf(divNames [index]).getPalette(numColors);
	}

	public static Color[] qualitative(int index, int numColors) {
		return ColorPalettes.valueOf(qualNames [index]).getPalette(numColors);
	}

	public static Color[] sequential(int index, int numColors) {
		return ColorPalettes.valueOf(seqNames [index]).getPalette(numColors);
	}

	public <D> HashMap<D, Color> createMap(D[] a) {
		IntFunction<Color> mapper = getDiscreteMapper(a.length);
		HashMap<D, Color> re = new HashMap<D, Color>();
		for (int i=0; i<a.length; i++)
			re.put(a[i], mapper.apply(i));
		return re;
	}

	
}

