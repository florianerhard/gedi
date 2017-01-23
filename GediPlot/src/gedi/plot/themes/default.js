var font = "Arial";

style = {

		"size": {
			"min": 1,
			"max": 3.5
		},
		"lwd": {
			"min": 1,
			"max": 8
		},

		"title": {
			"color": Color.black,
			"font": font,
			"bold": true,
			"size": 5
		},

		"xlab": {
			"color": Color.black,
			"font": font,
			"bold": false,
			"size": 3
		},

		"ylab": {
			"color": Color.black,
			"font": font,
			"bold": false,
			"size": 3
		},

		"background" : {
			"color": new Color(229,229,229),
			"tickColor": Color.white,
			"subtickColor": new Color(238,238,238)
		},

		"plot": {
			"background": Color.white,
			"ticks": 9
		},

		"ticks":  {
			"color": new Color(137,137,137),
			"width": 1.5,					// this is in pixel space!
			"type": LineType.SOLID,
			"size": 0.7
		},

		"ticklabels": {
			"color": new Color(137,137,137),
			"font": font,
			"bold": false,
			"size": 3,
			"distance": 0.5
		},

		"subticks":  {
			"width": 1, 				// this is in pixel space!
			"type": LineType.SOLID
		},

		"legend": {
			"title": {
				"background": new Color(229,229,229),
				"color": Color.black,
				"font": font,
				"bold": true,
				"size": 3
			},		


			"labels": {
				"color": Color.black,
				"font": font,
				"bold": false,
				"size": 2.5,
			},

			"border": {
				"color": Color.black,
				"linetype": LineType.SOLID,
				"width": 0
			},

			"field": {
				"background": new Color(229,229,229),
				"size": 4
			},
			
			"distance": 1.0,
			"background": new Color(245,245,245)

		}
}

createLayout = function(context) {

	var re = table();
	re.row()	.col(empty())	.col(empty())	.col(title())		.col(empty());
	re.row()	.col(ylab())	.col(scaleLeft()).sp(subplot(0,0))	.col(stack().layer(background()).layer(getLayersForSubplot(subplot(0,0)))).weight(1).spm(subplot(0,0))	.col(legends()).margin([0,style.legend.distance,0,style.legend.distance]);
	re.row()	.col(empty())	.col(empty())	.col(scaleBottom()).sp(subplot(0,0))	.col(empty());
	re.row()	.col(empty())	.col(empty())	.col(xlab())		.col(empty());
	return re;

}