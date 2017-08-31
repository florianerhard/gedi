
Array.prototype.next=function(element){
    for (var i = 0; i < this.length; i++) {
	    if (this[i] == element) {
	      return this[i+1];
	    }
	  }
	  return undefined;
};
Array.prototype.prev=function(element){
    for (var i = 0; i < this.length; i++) {
	    if (this[i] == element) {
	      return this[i-1];
	    }
	  }
	  return undefined;
};

Utils = {
    isStrictAscending: function(a,start=0,end=a.length) {
     for (var i=start+1; i<end; i++)
			if (a[i-1]>=a[i]) return false;
		return true;
    },
    
  };

function parseReferenceGenomicRegion (definition) {
	var p = definition.split(':');
	if (p.length!=2) throw "Cannot parse as location: "+pos;
	
	return {"reference":parseReference(p[0]),"region": new GenomicRegion(p[1])};
}

function toLocationString(rgr) {
	return rgr.reference.name+rgr.reference.strand+":"+rgr.region.toString();
}





function parseReference (definition) {
	var name, strand;
	if (definition.endsWith("+") || definition.endsWith("-")) {
		strand = definition.substring(definition.length-1);
		name = definition.substring(0,definition.length-1);
	} else {
		strand = "";
		name = definition;
	}
	return {"name":name,"strand":strand};
}
	

function GenomicRegion (definition) {

	if (typeof definition === 'string') {
		// parse it 
		if (definition.length==0) throw "Empty coordinates";
		var p = new Array(2);
		var pairs = definition.split('|');
		if (pairs.length==1 && String(parseInt(pairs[0]))===pairs[0]) {
			var po = parseInt(pairs[0]);
			definition = [po,po+1];
		} else {
			var definition = [];
			for(var i=0; i<pairs.length; i++) {
				p = pairs[i].split('-');
				if (p.length!=2 || isNaN(parseInt(p[0])) || isNaN(parseInt(p[1]))) return null;
				definition.push(parseInt(p[0]));
				definition.push(parseInt(p[1]));
			}
		}
	}
	
	if ((definition.length&1)==1) throw "Number of positions not even!";
	this.coords = this._normalize(definition);
}
 
GenomicRegion.prototype._normalize = function(a) {
	if (Utils.isStrictAscending(a)) return a;
	
	var c = [];
	for (var i=0; i<a.length; i+=2)
		if (a[i]<a[i+1]) {
			c.push(a[i]);
			c.push(a[i+1]);
		}
	var coords = c.slice();
	c = [];
	if (coords.length>0) {
		c.push(coords[0]);
		for (var i=2; i<coords.length; i+=2)
			if (coords[i-1]<coords[i]) {
				c.push(coords[i-1]);
				c.push(coords[i]);
			}
		c.push(coords[coords.length-1]);
		coords = c;
	}
	return coords;
}

GenomicRegion.prototype.getNumBoundaries = function() {
	return this.coords.length;
}

GenomicRegion.prototype.getBoundary = function(b) {
	return this.coords[b];
}


GenomicRegion.prototype.getNumParts = function() {
	return this.coords.length/2;
}


GenomicRegion.prototype.getStart = function(p) {
	if(typeof p === "undefined") 
		return this.coords[0];
	return this.coords[p*2];
}

GenomicRegion.prototype.getEnd = function(p) {
	if(typeof p === "undefined") 
		return this.coords[this.coords.length-1];
	return this.coords[p*2+1];
}

GenomicRegion.prototype.isSingleton = function() {
	return this.getNumParts()==1;
}


GenomicRegion.prototype.getLength = function(part) {
	return this.getEnd(part)-this.getStart(part);
}
GenomicRegion.prototype.getIntronLength = function(upstreamPart) {
	return this.getStart(upstreamPart+1)-this.getEnd(upstreamPart);
}

GenomicRegion.prototype.getTotalLength = function() {
	var re = 0;
	for (var i=0; i<this.getNumParts(); i++)
		re+=this.getLength(i);
	return re;
}
	
	
GenomicRegion.prototype.extendBack = function(len) {
	if (len==0) return this;
	if (len<0) 
		return this.subtract(this.map(new GenomicRegion([this.getTotalLength()+len,this.getTotalLength()])));
	else
		return this.union(new GenomicRegion([this.getEnd(),this.getEnd()+len]));
}

GenomicRegion.prototype.extendFront = function(len) {
	if (len==0) return this;
	if (len<0) 
		return this.subtract(this.map(new GenomicRegion([0,-len])));
	else
		return this.union(new GenomicRegion([this.getStart()-len,this.getStart()]));
}




GenomicRegion.prototype.subtract = function(co) {
	var len = this.getNumParts()*2;
	var colen = co.getNumParts()*2;
	var re = [];
	var t = 0;
	var c = 0;
	var s = -Infinity;
	while (t<len && c<colen) {
		var is = Math.max(this.getBoundary(t),co.getBoundary(c));
		var ie = Math.min(this.getBoundary(t+1),co.getBoundary(c+1));
		if (is<ie) {
			if (!isFinite(s) && this.getBoundary(t)<co.getBoundary(c)) s= this.getBoundary(t);
			if (isFinite(s)) {
				re.push(s); re.push(is);
			}
			s = ie;
			if (this.getBoundary(t+1)<co.getBoundary(c+1)) 
				s=-Infinity;
		} else if (this.getBoundary(t+1)<co.getBoundary(c+1)) {
			re.push(!isFinite(s)?this.getBoundary(t):s);
			re.push(this.getBoundary(t+1));
			s = -Infinity;
		}

		if (this.getBoundary(t+1)<co.getBoundary(c+1))
			t+=2;
		else
			c+=2;
	}
	for (; t<len; t+=2) {
		re.push(!isFinite(s)?this.getBoundary(t):s);
		re.push(this.getBoundary(t+1));
		s=-Infinity;
	}
	return new GenomicRegion(re);
}

	
GenomicRegion.prototype.union = function(co) {
	var len = this.getNumParts()*2;
	var colen = co.getNumParts()*2;
	
	var re = [];
	var t = 0;
	var c = 0;
	while (t<len && c<colen) {
		var is = Math.min(this.getBoundary(t),co.getBoundary(c));
		var ie = this.getBoundary(t)==is?this.getBoundary(t+1):co.getBoundary(c+1);
		while (t<len && c<colen && Math.max(this.getBoundary(t), co.getBoundary(c))<Math.min(this.getBoundary(t+1), co.getBoundary(c+1))) {// overlapping 
			ie = Math.max(getBoundary(t+1), co.getBoundary(c+1));
			var tp = ie==this.getBoundary(t+1);
			var cp = ie==co.getBoundary(c+1);
			if (tp) 
				c+=2;
			if (cp) 
				t+=2;
			if (tp&&cp) break;
		}
		re.push(is); re.push(ie);
		if (t<len && this.getBoundary(t)<ie)
			t+=2;
		if (c<colen && co.getBoundary(c)<ie)
			c+=2;
	}
	while (t<len) {
		re.push(this.getBoundary(t++));
		re.push(this.getBoundary(t++)); 
	}
	while (c<colen) {
		re.push(co.getBoundary(c++));
		re.push(co.getBoundary(c++)); 
	}
	return new GenomicRegion(re);
}

GenomicRegion.prototype.intersect = function(co) {
	var len = this.getNumParts()*2;
	var colen = co.getNumParts()*2;
	var re = [];
	var t = 0;
	var c = 0;
	while (t<len && c<colen) {
		var is = Math.max(this.getBoundary(t),co.getBoundary(c));
		var ie = Math.min(this.getBoundary(t+1),co.getBoundary(c+1));
		if (is<ie) {
			re.push(is); re.push(ie);
		}
		if (this.getBoundary(t+1)<co.getBoundary(c+1))
			t+=2;
		else
			c+=2;
	}
	return new GenomicRegion(re);
}

GenomicRegion.prototype.map = function(coord) {
	if (coord.getTotalLength()==0) return new GenomicRegion();
	if (this.getTotalLength()<coord.getEnd())
		throw "coords are to long!";

	var len = this.getNumParts()*2;
	var colen = coord.getNumParts()*2;
	var create = [];
	var p = 0;
	var j = 0;
	for (var i=0; i<len && j<colen; i+=2) {
		var np = p+this.getBoundary(i+1)-this.getBoundary(i);
		while (j<colen && np>coord.getBoundary(j)) {
			create.push(this.getBoundary(i)+coord.getBoundary(j)-p);
			while (j+1<colen && np<coord.getBoundary(j+1)) {
				create.push(this.getBoundary(i+1));
				i+=2;
				create.push(this.getBoundary(i));
				p = np;
				np = p+this.getBoundary(i+1)-this.getBoundary(i);
			}
			create.push(this.getBoundary(i)+coord.getBoundary(j+1)-p);
			j+=2;
		}
		p = np;
	}
	return new GenomicRegion(create);
}

GenomicRegion.prototype.mapMaybeOutside = function(coord) {
	if (coord.getStart()>=0 && coord.getEnd()<=this.getTotalLength()) return this.map(coord);
	var before = coord.intersect(new GenomicRegion([coord.getStart(),0]));
	var after = coord.intersect(new GenomicRegion([this.getTotalLength(),coord.getEnd()]));
	var ins = coord.intersect(new GenomicRegion([0,this.getTotalLength()]));
	return before.translate(this.getStart()).union(this.map(ins)).union(after.translate(this.getEnd()-this.getTotalLength()));
}

GenomicRegion.prototype.induce = function(coord) {
	var len = this.getNumParts()*2;
	var colen = coord.getNumParts()*2;
	var re = [];
	var t = 0;
	var c = 0;
	var p = 0;
	while (t<len && c<colen) {
		var is = Math.max(this.getBoundary(t),coord.getBoundary(c));
		var ie = Math.min(this.getBoundary(t+1),coord.getBoundary(c+1));
		if (is<ie) {
			re.push(p+is-this.getBoundary(t)); re.push(p+ie-this.getBoundary(t));
		}
		if (this.getBoundary(t+1)<coord.getBoundary(c+1)) {
			p+=this.getBoundary(t+1)-this.getBoundary(t);
			t+=2;
		} else
			c+=2;

	}
	return new GenomicRegion(re);
}


	

GenomicRegion.prototype.toString = function() {
	var re = new Array(this.coords.length*2-1);
	var ind = 0;
	for (var i=0; i<this.getNumParts(); i++) {
		if (i>0)
			re[ind++]="|";
		re[ind++]=this.getStart(i);
		re[ind++]='-';
		re[ind++]=this.getEnd(i);
	}
	return re.join("");
}

GenomicRegion.prototype.getInfo = function() {
    return this.color + ' ' + this.type + ' apple';
};

