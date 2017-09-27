$( document ).ready(function() {

var webSocket;
function connect() {
	webSocket = new WebSocket("ws://" + location.hostname + ":" + location.port + "/test");
	webSocket.onmessage = function (msg) { message(msg); };
	webSocket.onclose = function (e) { console.log("reconnect"); connect();};
	webSocket.onerror = function (e) { console.log(JSON.stringify(e)); };
	webSocket.binaryType = "arraybuffer";
}
connect();

$("#send").click(update);
$("#location").keypress(function (e) {   if (e.keyCode === 13) { update(); }   });

function updateCanvasWidth(){   document.getElementById('tracks').width = $(window).width();  }

$(window).resize(updateCanvasWidth);
updateCanvasWidth();


var genome;
var ppbp;

var sentLoc;
setInterval(function () {
	if ($("#location").val()!=sentLoc)
		update();
},200);

function update() {
	if (webSocket.readyState==1) {
		sentLoc = $("#location").val();
		$(".track").each(function(i,o) {
			o.translate2=0;
			o.zoom2=1;
		});
		var msg = JSON.stringify({"location": sentLoc, "width": $(tracks).width() });
		webSocket.send(msg);
	}
}


function message(msg) {
	var jsonlen = new DataView(msg.data).getInt32(0);
	var opt = JSON.parse(String.fromCharCode.apply(null, new Uint8Array(msg.data,4,jsonlen)));
	var data = msg.data.slice(4+jsonlen);
	
	if (opt.msg=="info") {
		genome = opt.genome;
	}
	else {
	
		if (opt.location!=sentLoc)
			return; // dont do anything, another location was already sent!
		
		if (opt.msg=="viewinfo") {
			ppbp = opt.ppbp;
		} 
		else if (opt.msg=="trackdata") {
//		console.log("new data for "+sentLoc);
			var target = document.getElementById("tracks");
			target.height = opt.height;
			if (opt.format=="svg")
				svg(target,data);
			else
				blobImage(target,data,opt.format);
		}
	}
	

}



function svg(target, message) {
	var svg = String.fromCharCode.apply(null, new Uint8Array(message));
	var ctx = target.getContext('2d');
	target.translate = target.translate2;
	target.zoom = target.zoom2;
	target.render=function() {
		ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
		ctx.drawSvg(svg, this.translate,0,svg.width*this.zoom,svg.height);
	};
	target.render();
}


function blobImage(target, message, format) {
	var blob = new Blob([message], {type : 'image/'+format});
	createImageBitmap(blob).then(function(response) {
		var ctx = target.getContext('2d');
		target.translate = target.translate2;
		target.zoom = target.zoom2;
		target.render=function() {
//			console.log("Rendering t="+this.translate+" z="+this.zoom)
			ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
			ctx.drawImage(response, this.translate,0,response.width*this.zoom,response.height);
		};
		target.render();
	});
}

var zoomMultiplicationFactor = 1.2;

var dragX = -1;

	$(document).on("mousemove", function(e) {
       if (dragX>-1) {
		    var dx = e.pageX-dragX;
			dx = dx/ppbp;
			
			if (Math.abs(dx)>=1) {
				var loc = $("#location").val();
				var locs = loc.split("; ").map(function(s) {return parseReferenceGenomicRegion(s)});

				if (dx<0) {
					locs = reduceLeft(locs,-Math.round(dx));
					locs = extendRight(locs,-Math.round(dx));
				}
				else {
					locs = reduceRight(locs,Math.round(dx));
					locs = extendLeft(locs,Math.round(dx));
				}
				$("#location").val(locs.map(function(s) {return toLocationString(s)}).join("; "));
			
				$(".track").each(function(i,o) {
					o.translate +=Math.round(dx)*ppbp;
					o.translate2 +=Math.round(dx)*ppbp;
					o.render();
				});
			    dragX = e.pageX;
			    document.getElementById("tracks").render();
			}
		   
	    }
    });


    $(document).on("mousedown", "#tracks", function (e) {
        dragX = e.pageX;
    });

    $(document).on("mouseup", function (e) {
        dragX = -1;
    });
	
	$(document).on('mousewheel', function(e) {
	e.preventDefault();
            
        var wheelRotation = -e.deltaY;
	    var f = e.pageX/document.getElementById('tracks').width;

		var loc = $("#location").val();
		var locs = loc.split("; ").map(function(s) {return parseReferenceGenomicRegion(s)});

		var total = 0;
		for (var i=0; i<locs.length; i++)
			total += locs[i].region.getTotalLength();


		if (wheelRotation<0) {

			var newTotal = Math.round(total/zoomMultiplicationFactor);
			if (newTotal==0) return;

			var newStart = Math.round(f*(total-newTotal));


			var removeLeft = newStart;
			var removeRight = total-newStart-newTotal;

			locs = reduceLeft(locs,removeLeft);
			locs = reduceRight(locs,removeRight);


		}
		if (wheelRotation>0) {
			var addToTotal = Math.round(total*(zoomMultiplicationFactor-1));
			var addLeft = Math.round((f)*addToTotal);
			var addRight = Math.round((1-f)*addToTotal);

			if (addLeft==0 && addRight==0) {
				if (f>0.5)
					addLeft++;
				else
					addRight++;
			}

			locs = extendLeft(locs,addLeft);
			locs = extendRight(locs,addRight);

		}
		
		$("#location").val(locs.map(function(s) {return toLocationString(s)}).join("; "));
		var fac = Math.pow(zoomMultiplicationFactor,-Math.sign(wheelRotation));
		$(".track").each(function(i,o) {
			o.translate-=e.pageX*(1-o.zoom);
			o.zoom*=fac;
			o.translate+=e.pageX*(1-o.zoom);
			o.translate2-=e.pageX*(1-o.zoom2);
			o.zoom2*=fac;
			o.translate2+=e.pageX*(1-o.zoom2);
			o.render();
		});
	    
		document.getElementById("tracks").render();
	});




	function extendRight(locs, right) {
		while (right>0) {
			var lastLength = genome.map[locs[locs.length-1].reference.name];
			var lastEnd = locs[locs.length-1].region.getEnd();
			if (lastEnd+right>lastLength) {
				var after = {"name":genome.order.next(locs[locs.length-1].reference.name),"strand":locs[locs.length-1].reference.strand};
				locs[locs.length-1].region = locs[locs.length-1].region.extendBack(lastLength-lastEnd);
				if (typeof after.name === "undefined") {
					right = 0;
				} else {
					right-=lastLength-lastEnd;
					var start = 0;
					var end = Math.min(genome.map[after.name], right);
					locs.splice(locs.length, 0, {"reference": after, "region": new GenomicRegion([start,end])});
					right-=end-start;
				}
			} else {
				// fits into current chromosome
				locs[locs.length-1].region = locs[locs.length-1].region.extendBack(right);
				right = 0;
			}
		}
		return locs;
		
	}
	function extendLeft(locs, left) {
		while (left>0) {
			if (locs[0].region.getStart()-left<0) {
				// either take the next ref left of that or decrease addLeft
				var before = {"name":genome.order.prev(locs[0].reference.name),"strand":locs[0].reference.strand};
				left-=locs[0].region.getStart();
				locs[0].region = locs[0].region.extendFront(locs[0].region.getStart());
				if (typeof before.name === "undefined") {
					left = 0;
				} else {
					var end = genome.map[before.name];
					var start = Math.max(0,end-left);
					locs.splice(0, 0, {"reference": before, "region": new GenomicRegion([start,end])});
					left-=end-start;
				}
			} else {
				// fits into current chromosome
				locs[0].region = locs[0].region.extendFront(left);
				left = 0;
			}
		}
		return locs;
	}

	function reduceLeft(locs, left) {
		var pos = 0;
		for (var i=0; i<locs.length; i++) {
			if (pos+locs[i].region.getTotalLength()>left) {
				locs = locs.slice(i);
				locs[0].region = locs[0].region.map(new GenomicRegion([left-pos,locs[0].region.getTotalLength()]));
				break;
			}
			pos+=locs[i].region.getTotalLength();
		}
		return locs;
	}

	function reduceRight(locs, right) {
		var total = 0;
		for (var i=0; i<locs.length; i++)
			total += locs[i].region.getTotalLength();
		var newTotal = total-right;

		var pos = 0;
		for (var i=0; i<locs.length; i++) {
			if (pos+locs[i].region.getTotalLength()>newTotal) {
				locs = locs.slice(0,i+1);
				locs[i].region = locs[i].region.map(new GenomicRegion([0,newTotal-pos]));
				break;
			}
			pos+=locs[i].region.getTotalLength();
		}
		return locs;
	}


});

