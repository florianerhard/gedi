<?JS0
varin("id","Track id",true);
varin("colors","Colors (Either array of strings or colors, or Palette name; default: Accent)",false);
varin("names","Names of conditions",true);
varin("sizes","Line widths (default: 2)",false);
?>
<?JS
var colors = colors?colors:"Dark2";
var sizes = sizes?sizes:EI.repeat(names.length,2).toArray();

if (typeof colors === "string") {
	colors = ColorPalettes.get(colors, names.length);
} else if (!Color.class.isAssignableFrom(colors[0].getClass())) {
	var cols = JS.buffer(colors.length,Color.class);
	for (var j = 0; j < colors.length; j++) {
		var col = PaintUtils.parseColor(colors[j].toString());
		if (col==null)
			col = ColorPalettes.get(colors[j], colors.length)[j];
		if (col==null)
			col = Color.black;
		cols[j] = col;
	}
	colors = cols;
}

?>
.<?JS id ?> { "styles": 
				[
<?JS for (var j=0; j<colors.length; j++) {?>
					{ "color": "<?JS print(PaintUtils.encodeColor(colors[j])); ?>", "fill": "<?JS print(PaintUtils.encodeColor(colors[j])); ?>", "name": "<?JS print(names[j]); ?>" },
<?JS } ?>
				]
			}
