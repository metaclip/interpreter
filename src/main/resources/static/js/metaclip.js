var metaclipConfig = {
	colors:{
		'command':'#f2baf6',
		'datasource':'#ff928c',
		'datasource-details':'#ff928c',
		'verification':'#78cecb',
		'transformation':'#ffc766',
		'calibration':'#ff928c',
		'graphical':'#faafc2',
		'cluster':'#4c6ef5',
		'ipcc_terms':'#5492cd',
	},
	icons:{
		'command':'f120',
		'cluster':'f142',
		'datasource':'f1c0',
		'datasource-details':'f1c0',
		'verification':'f058',
		'ds:Transformation':'f074',
		'ds:IndexDefinition':'f121',
		'ds:IndexCalculation':'f12b',
		'ds:ArgumentValue':'f086',
		'ds:ClimateIndex':'f121',
		'ds:ClimateIndexDefinition':'f121',
		'ds:ClimateIndexCalculation':'f12b',
		'calibration':'f24e',
		'graphical':'f080',
		'ds:Aggregation':'f066',
		'ds:ClimateIndex':'f12b',
		'ds:ETCCDI':'f12b',
		'ds:DatasetSubset':'f0b0',
		'ds:Climatology':'f073',
		'ds:Project':'f288',
		'ds:ModellingCenter':'f1ad',
		'ds:DataProvider':'f015',
		'ds:Variable':'f2cb',
		'ds:SpatialExtent':'f0b2',
		'ds:HorizontalExtent':'f101',
		'ds:VerticalExtent':'f102',		
		'ds:TemporalPeriod':'f073',
		'ds:ValidationTime':'f073',
		'ds:Package':'f187',
		'ds:Argument':'f075',
		'ds:ENSOregion':'f0ac',
		'ds:SeasonalForecastingSystem':'f085',
		'ds:VariableStandardDefinition':'f02d',
		'gp:Map':'f279',
		'gp:TextAnnotation':'f031',
		'gp:MapRaster':'f00a',
		'gp:MapLines':'f201',
		'vr:Validation':'f058',
		'vr:ForecastVerification':'f058',
		'ds:Regridding':'f247',
		'ds:Interpolation':'f247',
		'ds:ObservationalDataset':'f06e',
		'ds:InterpolationMethod':'f065',
		'ds:NearestNeighbor': 'f065',
		'ds:BicubicInterpolation': 'f065',
		'ds:BilinearInterpolation': 'f065',
		'ds:InverseDistanceWeighting': 'f065',
		'ds:Splines': 'f065',
		'ds:GCM':'f0ac',
		'ds:RCM':'f00e',
		'ds:Anomaly':'f146',
		'ds:DifferenceAnomaly':'f146',
		'ds:RelativeAnomaly':'f146',
		'gp:TextLayer':'f031',
		'gp:ChartLines':'f201',
		'gp:MapPoints':'f041',
		'ds:Latitude':'f07d',
		'ds:Longitude':'f07e',
		'ds:TemporalResolution':'f017',
		'ds:Ensemble':'f111',
		'ds:MultiGraphicalProduct':'f37f',
		'cl:NPQuantileMapping':'f1de',
		'cl:PQuantileMapping':'f1de',
		'cl:Calibration':'f1de',
		'cl:BiasCorrection':'f1de'
	},
	specialClasses:[
		'ds:Dataset',
		'ds:Step',
		'ds:hadClimatology',
		'ds:hadAggregation',
		'ds:hadClimateIndexCalculation',
		'ds:ClimateIndex',
		'ds:withReference',
		'gp:hadGraphicalRepresentation',
		'ds:hadDatasetSubset',
		'gp:Layer',
		'ds:Ensemble',
		'vr:hadValidation',
		'vr:hadForecastVerification',
		'ds:hadInterpolation',
		'ds:hadRegridding'
	]
}

// based on https://github.com/eisman/neo4jd3
function MetaclipD3(_selector,_options){
	var container, graph, info, node, nodes, relationship, relationshipOutline, relationshipOverlay, relationshipText, relationships, selector, simulation, svg, svgNodes, svgRelationships, svgScale, svgTranslate,
		justLoaded = false;
	var options = {
		arrowSize: 6,
		highlight: undefined,
		minCollision: undefined,
		graphData: undefined,
		nodeRadius: 25,
		linkDistance: 175,
		relationShipMaxLength: 13,
		relationshipColor: '#a5abb6',
		animation: true,
		fullInfo: '#node-full-info',
		onNodeDragEnd: undefined,
		onNodeDragStart: undefined
	};
	
	function appendGraph(container){
		svg = container.append('svg')
			.attr('width', '100%')
			.attr('height', '100%')
			.call(d3.zoom().on('zoom', function(){
				var scale = d3.event.transform.k, translate = [d3.event.transform.x, d3.event.transform.y];
				if(svgTranslate){
					translate[0] += svgTranslate[0];
					translate[1] += svgTranslate[1];
				}
				if(svgScale){
					scale *= svgScale;
				}
				svg.attr('transform', 'translate(' + translate[0] + ', ' + translate[1] + ') scale(' + scale + ')');
			}))
			.on('dblclick.zoom', null)
			.append('g')
			.attr('width', '100%')
			.attr('height', '100%');
	}

	function appendInfoPanel(container){
		return container.append('div').attr('class', 'metaclip-info');
	}

	function appendInfoElement(cls, isNode, property, value){
		var elem = info.append('a');
		elem.attr('href', '#').attr('class', cls).html('<strong>' + property + '</strong>' + (value ? (': ' + value) : ''));
		if(!value){
			elem.style('background-color', function(d){
				return (isNode ? node2color(property,false) : defaultColor(false));
			}).style('border-color', function(d){
				return (isNode ? node2color(property,true) : defaultColor(true));
			}).style('color', function(d){
				return '#fff';
			});
		}
	}

	function appendNode(){
		return node.enter().append('g').attr('class', function(d){
			var highlight, i, classes = 'node ' + d.group;
			if(icon(d)){
				classes += ' node-icon';
			}
			if(options.highlight){
				for(i = 0; i < options.highlight.length; i++){
					highlight = options.highlight[i];
					if(d.classes[0] === highlight.class && d.properties[highlight.property] === highlight.value){
						classes += ' node-highlighted';
						break;
					}
				}
			}
			return classes;
		}).on('click', function(d){
			simulation.restart();
			if(options.fullInfo){
				updateFullInfo(d);
			}
			if(typeof options.onNodeClick === 'function'){
				options.onNodeClick(d);
			}
		}).on('dblclick', function(d){
			//stickNode(d);
			if(typeof options.onNodeDoubleClick === 'function'){
				options.onNodeDoubleClick(d);
			}
		}).on('mouseenter', function(d){
			if(info){
				updateInfo(d);
			}
			if(typeof options.onNodeMouseEnter === 'function'){
				options.onNodeMouseEnter(d);
			}
		}).on('mouseleave', function(d){
			if(info){
				clearInfo(d);
			}
			if(typeof options.onNodeMouseLeave === 'function'){
				options.onNodeMouseLeave(d);
			}
		}).call(d3.drag()
			.on('start', dragStarted)
			.on('drag', dragged)
			.on('end', dragEnded));
	}

	function appendNodeToGraph(){
		var n = appendNode();
		appendRingToNode(n);
		appendOutlineToNode(n);
		appendTextToNode(n);
		return n;
	}

	function appendOutlineToNode(node){
		return node.append('circle')
			.attr('class', 'outline')
			.attr('r', options.nodeRadius)
			.style('fill', function(d){
				return node2color(d,true);
			}).style('stroke', function(d){
				return node2color(d,false);
			}).append('title').text(function(d){
				return toString(d);
			});
	}

	function appendRingToNode(node){
		return node.append('circle')
			.attr('class', 'ring')
			.attr('r', options.nodeRadius * 1.16)
			.append('title').text(function(d){
				return toString(d);
			});
	}

	function appendTextToNode(node){
		return node.append('text')
			.attr('class', function(d){
				return 'text' + (icon(d) ? ' icon' : '');
			})
			.attr('fill', '#ffffff')
			.attr('font-size', function(d){
				return icon(d) ? (options.nodeRadius + 'px') : '10px';
			})
			.attr('pointer-events', 'none')
			.attr('text-anchor', 'middle')
			.attr('y', function(d){
				return icon(d) ? (parseInt(Math.round(options.nodeRadius * 0.32)) + 'px') : '4px';
			})
			.html(function(d){
				var _icon = icon(d);
				return _icon ? '&#x' + _icon : d.id;
			});
	}

	function appendRelationship(){
		return relationship.enter()
			.append('g')
			.attr('class', function(d){
				var additional = '';
				if(d.type){
					if(hasSpecialClass([d.type])){
						additional = ' highlight';
					}
				}
				return 'relationship'+additional;
			})
			.on('dblclick', function(d){
				if(typeof options.onRelationshipDoubleClick === 'function'){
					options.onRelationshipDoubleClick(d);
				}
			}).on('mouseenter', function(d){
				if(info){
					updateInfo(d);
				}
			});
	}

	function appendOutlineToRelationship(r){
		return r.append('path')
			.attr('class', 'outline')
			.attr('fill', '#a5abb6')
			.attr('stroke', 'none');
	}

	function appendOverlayToRelationship(r){
		return r.append('path').attr('class', 'overlay');
	}

	function appendTextToRelationship(r){
		return r.append('text')
			.attr('class', 'text')
			.attr('fill', '#000000')
			.attr('font-size', '8px')
			.attr('pointer-events', 'none')
			.attr('text-anchor', 'middle')
			.text(function(d){
				var text = d.type;
				if(text.indexOf(':')>=0){
					text = text.substring(text.indexOf(':')+1,text.length);
				}
				return text;
			});
	}

	function appendRelationshipToGraph(){
		var relationship = appendRelationship(),
			text = appendTextToRelationship(relationship),
			outline = appendOutlineToRelationship(relationship),
			overlay = appendOverlayToRelationship(relationship);
		return{
			outline: outline,
			overlay: overlay,
			relationship: relationship,
			text: text
		};
	}

	function node2color(d,dark){
		var color = defaultColor(dark);
		if(d.classes){
			if(d.classes.indexOf('ds:Step')>=0){
				color = '#405f9e';	
			}else if(metaclipConfig.colors[d.group]){
				color = metaclipConfig.colors[d.group];
			}
			if(dark){
				return d3.rgb(color).darker(1);
			}			
		}
		return color;
    }

	function clearInfo(){
        info.html('');
	}
	
	function hasSpecialClass(classes){
		for(var i=0;i<metaclipConfig.specialClasses.length;i++){
			if(classes.indexOf(metaclipConfig.specialClasses[i]) >= 0) return true;
		}
		return false;
	}

	function color(){
		return metaclipConfig.colors[metaclipConfig.colors.length * Math.random() << 0];
	}

	function contains(array, id){
		var filter = array.filter(function(elem){
			return elem.id === id;
		});
		return filter.length > 0;
	}

	function defaultColor(dark){
		if(!dark){
			return options.relationshipColor;
		}else{
			return d3.rgb(options.relationshipColor).darker(1);
		}
	}

	function dragEnded(d){
		if(!d3.event.active){
			simulation.alphaTarget(0);
		}
        if(typeof options.onNodeDragEnd === 'function'){
        	options.onNodeDragEnd(d);
        }
	}

	function dragged(d){
		stickNode(d);
	}

	function dragStarted(d){
		d.fx = d.x;
		d.fy = d.y;
		if(!d3.event.active){
			simulation.alphaTarget(0.3).restart();
		}
		if(typeof options.onNodeDragStart === 'function'){
			options.onNodeDragStart(d);
		}
	}

	function extend(obj1, obj2){
		var obj = {};
		merge(obj, obj1);
		merge(obj, obj2);
		return obj;
	}

	function icon(d){
		var code;
		if(metaclipConfig.icons[d.classes[0]]){
			code = metaclipConfig.icons[d.classes[0]];
		}else if(metaclipConfig.icons[d.group]){
			code = metaclipConfig.icons[d.group];
		}
		return code;
	}

	function init(_selector, _options){
		merge(options, _options);
		if(!options.minCollision){
			options.minCollision = options.nodeRadius * 2;
		}
		selector = _selector;
		container = d3.select(selector);
		container.html('');
		info = appendInfoPanel(container);
		appendGraph(container);
		loadMetaclipData();
		setTimeout(function(){
			zoomFit(false) 
		},500);
	}

	function initSimulation(){
		//var width = svg.node().parentElement.parentElement.clientWidth;
		//var height = svg.node().parentElement.parentElement.clientHeight;
		var simulation = d3.forceSimulation()
			.force('collide', d3.forceCollide().radius(function(d){
					return options.minCollision;
				}).iterations(1))
			.force('charge',d3.forceManyBody().strength(5))
			.force('link',d3.forceLink().id(function(d){
				return d.id;
			}).distance(options.linkDistance))
			//.force("x", d3.forceX().strength(0.7))
			//.force("y", d3.forceY().strength(0.7))
			//.force('center', d3.forceCenter(width / 2, height / 2))
			.on('tick', function(){
				tick();
			})
			.on('end', function(){
				if(!justLoaded){
					justLoaded = true;
				}
			});
		return simulation;
	}

	function loadMetaclipData(){
		nodes = options.graphData.nodes;
		relationships = options.graphData.relationships;
		options.graphData.nodes = [];
		options.graphData.relationships = [];
		update();
	}

	function merge(target, source){
		Object.keys(source).forEach(function(property){
			target[property] = source[property];
		});
	}

	function rotate(cx, cy, x, y, angle){
		var radians = (Math.PI / 180) * angle,
			cos = Math.cos(radians),
			sin = Math.sin(radians),
			nx = (cos * (x - cx)) + (sin * (y - cy)) + cx,
			ny = (cos * (y - cy)) - (sin * (x - cx)) + cy;
		return { x: nx, y: ny };
	}

	function rotatePoint(c, p, angle){
		return rotate(c.x, c.y, p.x, p.y, angle);
	}

	function rotation(source, target){
		return Math.atan2(target.y - source.y, target.x - source.x) * 180 / Math.PI;
	}

	function stickNode(d){
		d.fx = d3.event.x;
		d.fy = d3.event.y;
	}

	function tick(){
		tickNodes();
		tickRelationships();
	}

	function tickNodes(){
		if(node){
			node.attr('transform', function(d){
				return 'translate(' + d.x + ', ' + d.y + ')';
			});
		}
	}

	function tickRelationships(){
		if(relationship){
			relationship.attr('transform', function(d){
				var angle = rotation(d.source, d.target);
				return 'translate(' + d.source.x + ', ' + d.source.y + ') rotate(' + angle + ')';
			});
			tickRelationshipsTexts();
			tickRelationshipsOutlines();
			tickRelationshipsOverlays();
		}
	}

	function tickRelationshipsOutlines(){
		relationship.each(function(relationship){
			var rel = d3.select(this), outline = rel.select('.outline'), text = rel.select('.text'), padding = 3;
			outline.attr('d', function(d){
				var textBoundingBox = {width:0};
				try{
					textBoundingBox = text.node().getBBox();
				}catch(x){
					// do nothing
				}
				var center = { x: 0, y: 0 },
					angle = rotation(d.source, d.target),
					textPadding = 5,
					u = unitaryVector(d.source, d.target),
					textMargin = { x: (d.target.x - d.source.x - (textBoundingBox.width + textPadding) * u.x) * 0.5, y: (d.target.y - d.source.y - (textBoundingBox.width + textPadding) * u.y) * 0.5 },
					n = unitaryNormalVector(d.source, d.target),
					rotatedPointA1 = rotatePoint(center, { x: 0 + (options.nodeRadius + 1) * u.x - n.x, y: 0 + (options.nodeRadius + 1) * u.y - n.y }, angle),
					rotatedPointB1 = rotatePoint(center, { x: textMargin.x - n.x, y: textMargin.y - n.y }, angle),
					rotatedPointC1 = rotatePoint(center, { x: textMargin.x, y: textMargin.y }, angle),
					rotatedPointD1 = rotatePoint(center, { x: 0 + (options.nodeRadius + 1) * u.x, y: 0 + (options.nodeRadius + 1) * u.y }, angle),
					rotatedPointA2 = rotatePoint(center, { x: d.target.x - d.source.x - textMargin.x - n.x, y: d.target.y - d.source.y - textMargin.y - n.y }, angle),
					rotatedPointB2 = rotatePoint(center, { x: d.target.x - d.source.x - (options.nodeRadius + 1) * u.x - n.x - u.x * options.arrowSize, y: d.target.y - d.source.y - (options.nodeRadius + 1) * u.y - n.y - u.y * options.arrowSize }, angle),
					rotatedPointC2 = rotatePoint(center, { x: d.target.x - d.source.x - (options.nodeRadius + 1) * u.x - n.x + (n.x - u.x) * options.arrowSize, y: d.target.y - d.source.y - (options.nodeRadius + 1) * u.y - n.y + (n.y - u.y) * options.arrowSize }, angle),
					rotatedPointD2 = rotatePoint(center, { x: d.target.x - d.source.x - (options.nodeRadius + 1) * u.x, y: d.target.y - d.source.y - (options.nodeRadius + 1) * u.y }, angle),
					rotatedPointE2 = rotatePoint(center, { x: d.target.x - d.source.x - (options.nodeRadius + 1) * u.x + (- n.x - u.x) * options.arrowSize, y: d.target.y - d.source.y - (options.nodeRadius + 1) * u.y + (- n.y - u.y) * options.arrowSize }, angle),
					rotatedPointF2 = rotatePoint(center, { x: d.target.x - d.source.x - (options.nodeRadius + 1) * u.x - u.x * options.arrowSize, y: d.target.y - d.source.y - (options.nodeRadius + 1) * u.y - u.y * options.arrowSize }, angle),
					rotatedPointG2 = rotatePoint(center, { x: d.target.x - d.source.x - textMargin.x, y: d.target.y - d.source.y - textMargin.y }, angle);
				return 'M ' + rotatedPointA1.x + ' ' + rotatedPointA1.y +
					' L ' + rotatedPointB1.x + ' ' + rotatedPointB1.y +
					' L ' + rotatedPointC1.x + ' ' + rotatedPointC1.y +
					' L ' + rotatedPointD1.x + ' ' + rotatedPointD1.y +
					' Z M ' + rotatedPointA2.x + ' ' + rotatedPointA2.y +
					' L ' + rotatedPointB2.x + ' ' + rotatedPointB2.y +
					' L ' + rotatedPointC2.x + ' ' + rotatedPointC2.y +
					' L ' + rotatedPointD2.x + ' ' + rotatedPointD2.y +
					' L ' + rotatedPointE2.x + ' ' + rotatedPointE2.y +
					' L ' + rotatedPointF2.x + ' ' + rotatedPointF2.y +
					' L ' + rotatedPointG2.x + ' ' + rotatedPointG2.y +
					' Z';
			});
		});
	}

	function tickRelationshipsOverlays(){
		relationshipOverlay.attr('d', function(d){
			var center = { x: 0, y: 0 },
				angle = rotation(d.source, d.target),
				n1 = unitaryNormalVector(d.source, d.target),
				n = unitaryNormalVector(d.source, d.target, 50),
				rotatedPointA = rotatePoint(center, { x: 0 - n.x, y: 0 - n.y }, angle),
				rotatedPointB = rotatePoint(center, { x: d.target.x - d.source.x - n.x, y: d.target.y - d.source.y - n.y }, angle),
				rotatedPointC = rotatePoint(center, { x: d.target.x - d.source.x + n.x - n1.x, y: d.target.y - d.source.y + n.y - n1.y }, angle),
				rotatedPointD = rotatePoint(center, { x: 0 + n.x - n1.x, y: 0 + n.y - n1.y }, angle);
			return 'M ' + rotatedPointA.x + ' ' + rotatedPointA.y +
				' L ' + rotatedPointB.x + ' ' + rotatedPointB.y +
				' L ' + rotatedPointC.x + ' ' + rotatedPointC.y +
				' L ' + rotatedPointD.x + ' ' + rotatedPointD.y +
				' Z';
		});
	}

	function tickRelationshipsTexts(){
		relationshipText.attr('transform', function(d){
			var angle = (rotation(d.source, d.target) + 360) % 360,
				mirror = angle > 90 && angle < 270,				
				center = { x: 0, y: 0 },
				n = unitaryNormalVector(d.source, d.target),
				nWeight = mirror ? 2 : -3,
				point = { x: (d.target.x - d.source.x) * 0.5 + n.x * nWeight, y: (d.target.y - d.source.y) * 0.5 + n.y * nWeight },
				rotatedPoint = rotatePoint(center, point, angle);
			return 'translate(' + rotatedPoint.x + ', ' + rotatedPoint.y + ') rotate(' + (mirror ? 180 : 0) + ')';
		});
	}

	function toString(d){
		var s = (d.label!=null? d.label : d.id);
		s += ' instance of ' + (d.classes ? d.classes[0] : d.type);
		return s;
	}

	function unitaryNormalVector(source, target, newLength){
		var center = { x: 0, y: 0 }, vector = unitaryVector(source, target, newLength);
		return rotatePoint(center, vector, 90);
    }

	function unitaryVector(source, target, newLength){
		var length = Math.sqrt(Math.pow(target.x - source.x, 2) + Math.pow(target.y - source.y, 2)) / Math.sqrt(newLength || 1);
		return{
			x: (target.x - source.x) / length,
			y: (target.y - source.y) / length,
		};
	}

	function update(){
		var width = $(selector).width();
		var height = $(selector).height();
		for(var i in nodes){
			var node = nodes[i];
			var visible = false;
			if(node.classes){
				if(hasSpecialClass(node.classes)){
					visible = true;
				}
			}
			if(options.graphData.positions[node.id]){
				var position = options.graphData.positions[node.id];
				node.fx = position.x * width/100;
				node.fy = position.y * height/100;
			}
			if(!visible){
				if(options.graphData.positions[node.id]){
					visible = true;
				}
			}
			node.visible = visible;
		}
		svg.selectAll("*").remove();
		svgRelationships = svg.append('g').attr('class', 'relationships');
		svgNodes = svg.append('g').attr('class', 'nodes');
		simulation = initSimulation();
		if(options.animation===false){
			stopSim();
		}
		updateNodesAndRelationships();
	}
	
	function expand(n){
		var toShow = [];
		for(var i in relationships){
			var relation = relationships[i];
			if(typeof relation.target === 'string'){
				if(n.id==relation.target){
					toShow.push(relation.source);
				}else if(n.id==relation.source){
					toShow.push(relation.target);
				}
			}else{
				if(n.id===relation.target.id){
					toShow.push(relation.source.id);
				}else if(n.id==relation.source.id){
					toShow.push(relation.target.id);
				}
			}
		}
		var change = false;
		for(var i in nodes){
			var node = nodes[i];
			if(toShow.indexOf(node.id)>=0){
				if(node.visible != true) change = true;
				node.visible = true;
			}
		}		
		if(change){
			svg.selectAll("*").remove();
			svgRelationships = svg.append('g').attr('class', 'relationships');
			svgNodes = svg.append('g').attr('class', 'nodes');
			simulation.stop();
			simulation = initSimulation();
			if(options.animation===false){
				stopSim();
			}
			updateNodesAndRelationships();
		}
	}
	
	function positions(){
		var pos = [];
		var width = $(selector).width();
		var height = $(selector).height();
		for(var i in nodes){
			var node = nodes[i];
			if(node.visible){
				if(node.fx!=null && node.fy!=null){
					pos[node.id] = {x:Math.round(100*100*node.fx/width)/100,y:Math.round(100*100*node.fy/height)/100};
				}else{
					pos[node.id] = {x:Math.round(100*100*node.x/width)/100,y:Math.round(100*100*node.y/height)/100};
				}
			}
		}
		return pos;
	}
	
	function collapseInner(id,toHide){
		for(var i in relationships){
			var relation = relationships[i];
			var hideId = null;
			if(typeof relation.target === 'string'){
				if(id==relation.target){
					hideId = relation.source;
				}else if(id==relation.source){
					hideId = relation.target;
				}
			}else{
				if(id===relation.target.id){
					hideId = relation.source.id;
				}else if(id==relation.source.id){
					hideId = relation.target.id;
				}
			}
			if(hideId!=null){
				if(toHide.indexOf(hideId)<0){
					var node = find(hideId);
					if(node === null) continue;
					if(!hasSpecialClass(node.classes)){
						toHide.push(hideId);
						collapseInner(hideId,toHide);						
					}
				}
			}
		}
	}
	
	function find(id){
		for(var i in nodes){
			if(nodes[i].id===id) return nodes[i];
		}
		return null;
	}
	
	function collapse(n){
		var toHide = [];
		collapseInner(n.id,toHide);
		if(toHide.indexOf(n.id)>=0){
			toHide.splice(toHide.indexOf(n.id), 1);
		}
		var change = false;
		for(var i in nodes){
			var node = nodes[i];
			if(toHide.indexOf(node.id)>=0){
				if(node.visible === true) change = true;
				node.visible = false;
			}
		}
		if(change){
			svg.selectAll("*").remove();
			svgRelationships = svg.append('g').attr('class', 'relationships');
			svgNodes = svg.append('g').attr('class', 'nodes');
			simulation = initSimulation();
			if(options.animation===false){
				stopSim();
			}
			updateNodesAndRelationships();
		}
	}

	function updateInfo(d){
		clearInfo();
		if(d.type){
			var type = d.type, source = '', target = '';
			if(d.source){
				source = nodes.filter(function(x){return x.id===d.source.id;})[0].label;
			}
			if(d.target){
				target = nodes.filter(function(x){return x.id===d.target.id;})[0].label;
			}
			appendInfoElement('property', false, d.type, 'from ' + source + ' to ' + target);
		}else{
			var label = '', clazz = '';
			if(d.label){
				label = d.label;
			}
			if(d.classes){
				clazz = d.classes[0];
			}
			appendInfoElement('property', false, label, clazz);
		}
	}
	
	function updateFullInfo(d){
		var cont = d3.select(options.fullInfo);
		cont.style('display','block');
		cont.style('max-width','100%');
		var panel = cont.select('.info-panel');
		panel.html('');
		var info = panel.append('div');
		if(d.label){
			info.append('div').attr('class','py-1').html('<span><strong>Node</strong>: '+d.label+'</span>');
		}
		var clazzs = [
		  {code:'dc:title',label:'Title'},
		  {code:'dc:description',label:'Description'},
		  {code:'description',label:'Description'},
		  {code:'rdf:comment',label:'Comment'},
		  {code:'rdf:seeAlso',label:'See also',pre:true},
		  {code:'dc:source',label:'Source'},
		  {code:'dc:creator',label:'Creator'},
		  {code:'ds:hadLiteralCommandCall',label:'Command'},
		  {code:'ds:hasRun',label:'Run'},
		  {code:'ds:referenceURL',label:'Reference URL'},
		  {code:'ds:indexType',label:'Index type'},
		  {code:'ds:inputECV',label:'Input ECV'},
		  {code:'ds:thresholdType',label:'Threshold type'},
		  {code:'prov:specializationOf',label:'Specialization of'},
		  {code:'ds:season',label:'Season'},
		  {code:'prov:startedAtTime',label:'Start time'},
		  {code:'prov:endedAtTime',label:'End time'},
		  {code:'ds:hasTimeStep',label:'Time step'},
		  {code:'ds:hasTemporalResolution',label:'Temporal resolution'},
		  {code:'ds:hasCellMethod',label:'Cell method'},
		  {code:'gp:Mask',label:'Mask'}		  
		];
		for(var c=0;c<clazzs.length;c++){
			var clazz = clazzs[c];
			if(d.properties[clazz.code]){
				if(clazz.pre===true){
					info.append('div').attr('class','py-1').html('<span><strong>'+clazz.label+'</strong>:<pre>'+d.properties[clazz.code]+'</pre></span>');					
				}else{
					info.append('div').attr('class','py-1').html('<span><strong>'+clazz.label+'</strong>: '+d.properties[clazz.code]+'</span>');
				}
			}			
		}
		if(d.classes!=null){
			var div = info.append('div').attr('class','py-1');
			var clazz = div.append('span').html('<span><strong>Is a</strong>: '+d.classes[0]+'</span>');
			addClassInfo(div,d.classes[0],false);
		}
		var detailsDiv = panel.append('div');
		var detailsPanelHeader = detailsDiv.append('div').attr('class','py-1').append('span').html('Show details');
		detailsPanelHeader.append('div')
			.attr('title','Show/hide details')
			.attr('class','fa fa-angle-down pointer px-1')
			.on('click',function(e){
				var el = $(this);
				var t = el.parent().parent().next($(this));
				if(!t.is(':visible')){
					t.show();
					el.addClass('fa-angle-up');
					el.removeClass('fa-angle-down');
				}else{
					t.hide();
					el.removeClass('fa-angle-up');
					el.addClass('fa-angle-down');
				}
			});
		var detailsPanel = detailsDiv.append('div').style('display','none').attr('class','px-2');

		if(d.classes!=null){
			var header = detailsPanel.append('div').attr('class','py-2').append('span').html('<strong>Instance of: </strong>'+d.classes[0]);
			var classes = detailsPanel.append('ul').attr('class','px-3');
			d.classes.forEach(function(clazz){
				var li = classes.append('li');
				addClassInfo(li,clazz,true);
			});			
		}
		if(d.properties!=null){
			if(Object.keys(d.properties).length>0){
				detailsPanel.append('div').attr('class','py-2').append('span').html('<strong>Properties:</strong>');
				var props = detailsPanel.append('div').attr('class','px-3');
				Object.keys(d.properties).forEach(function(property){
					var element = props.append('li').append('span');
				
					// a known class
					var ix = property.lastIndexOf(':');
					var prefix = property.substring(0,ix);
					if(options.graphData.prefixes[prefix]){
						prefix = options.graphData.prefixes[prefix];
					}
					var className = property.substring(ix+1);
					var span = element.append('span')
					span.append('a')
						.attr('href',prefix+className)
						.attr('target','_blank')
						.attr('title','Open '+prefix)
						.append('span')
						.attr('class','fa fa-link px-1');					
					if(isValidURL(d.properties[property])){
						span.append('span')
							.text(property+": ")
								.append('a')
									.attr('href',d.properties[property])
									.attr('target','_blank')
									.text(d.properties[property]);
					}else{
						span.append('span')
							.html(property+": "+d.properties[property]);						
					}
					if(options.graphData.classes[property]){
						annotationNode(property,element);
					}
				});
			}
		}
		var relations = relationships.filter(function(r){
			if(r.source.id === d.id || r.target.id === d.id){
				return true;
			}
			return false;
		});
		if(relations.length>0){
			detailsPanel.append('div').attr('class','py-2').append('span').html('<strong>Relationships:</strong>');
			
			var props = detailsPanel.append('div').attr('class','px-3');
			relations.forEach(function(relation){
				var type = relation.type;
				var element = props.append('li');
			
				// a known class
				var ix = type.lastIndexOf(':');
				var prefix = type.substring(0,ix);
				if(options.graphData.prefixes[prefix]){
					prefix = options.graphData.prefixes[prefix];
				}
				var className = type.substring(ix+1);
				var span = element.append('span')				
				span.append('a')
					.attr('href',prefix+className)
					.attr('target','_blank')
					.attr('title','Open '+prefix)
					.append('span')
					.attr('class','fa fa-link px-1');	
				span.append('span').html(type);
				if(options.graphData.prefixes[prefix]){
					prefix = options.graphData.prefixes[prefix];
				}
				if(options.graphData.classes[type]){
					annotationNode(type,element);
				}				var info = '';
				if(relation.source.id === d.id){
					info += ' from '+relation.target.label;
				}else{
					info += ' to '+relation.source.label;
				}
				var id = relation.source.id;
				if(relation.source.id === d.id){
					id = relation.target.id;
				}
				element.append('span').attr('title','Show this node').attr('class','link').attr('data-id',id).html(info).on('click',function(){
					for(var i=0;i<nodes.length;i++){
						if(nodes[i].id===this.attributes['data-id'].value){
							updateFullInfo(nodes[i]);
							return;
						}
					}
				});
			});
		}
	}
	
	function addClassInfo(container,clazz,addLabel){
		var element = container.append('span');
		// a known class
		var ix = clazz.lastIndexOf(':');
		var prefix = clazz.substring(0,ix);
		if(options.graphData.prefixes[prefix]){
			prefix = options.graphData.prefixes[prefix];
		}
		var className = clazz.substring(ix+1);
		var span = element.append('span');
		span.append('a')
			.attr('href',prefix+className)
			.attr('target','_blank')
			.attr('title','Open '+prefix)
			.append('span')
			.attr('class','fa fa-link px-1');
		if(addLabel===true){
			span.append('span')
				.html(clazz);
		}
		if(options.graphData.classes[clazz]){
			annotationNode(clazz,element);
		}
	}
	
	function isValidURL(str){
		if(str.startsWith('http://') || str.startsWith('https://')){
			return true;
		}else{
			return false;
		}
	}
	
	function annotationNode(clazz,element){
		var annotations = options.graphData.classes[clazz].annotations;
		var keys = Object.keys(annotations);
		if(keys.length>0){
			var text = '<ul>';
			keys.forEach(function(key){
				var abbr = key, title = key;
				var ix = key.indexOf('#');
				if(ix>=0){
					abbr = key.substring(ix+1);
					title = key.substring(0,ix);
				}
				var element = '<span class="abbr" title="'+title+'">'+abbr+'</span>';
				text += '<li>'+element+': '+annotations[key]+'</li>';
			});
			text += '</ul>';
			element.append('div')
				.attr('title','Show/hide annotations')
				.attr('class','fa fa-angle-down pointer px-1')
				.on('click',function(e){
					var el = $(this);
					var t = el.next();
					if(!t.is(':visible')){
						t.show();
						el.addClass('fa-angle-up');
						el.removeClass('fa-angle-down');
					}else{
						t.hide();
						el.removeClass('fa-angle-up');
						el.addClass('fa-angle-down');
					}
				});
			element.append('div')
				.attr('class','annotations')
				.style('display','none')
				.html(text)
		}
	}

	function updateNodes(){
		var vNodes = visibleNodes();
		node = svgNodes.selectAll('.node').data(vNodes,function(d){
			return d.id;
		});
		var nodeEnter = appendNodeToGraph();
		node = nodeEnter.merge(node);
	}

	function updateNodesAndRelationships(){
		updateRelationships();
		updateNodes();
		simulation.nodes(visibleNodes());
		simulation.force('link').links(visibleRelationships());
	}

	function visibleNodes(){
		return nodes.filter(function(d){return d.visible;});
	}
	
	function visibleRelationships(){
		return relationships.filter(function(d){
			var visibleSource = nodes.filter(function(e){
				if(typeof d.source === 'string'){
					return e.id==d.source;	
				}else{
					return e.id==d.source.id;
				}
			});
			var visibleTarget = nodes.filter(function(e){
				if(typeof d.target === 'string'){
					return e.id==d.target;	
				}else{
					return e.id==d.target.id;
				}
			});
			if(visibleSource.length>0 && visibleTarget.length>0){
				return visibleSource[0].visible && visibleTarget[0].visible;				
			}
			return false;
		});
	}
	
	function updateRelationships(){
		relationship = svgRelationships.selectAll('.relationship').data(visibleRelationships(),function(d){
			return d.id;
		});
		var relationshipEnter = appendRelationshipToGraph();
		relationship = relationshipEnter.relationship.merge(relationship);
		relationshipOutline = svg.selectAll('.relationship .outline');
		relationshipOutline = relationshipEnter.outline.merge(relationshipOutline);
		relationshipOverlay = svg.selectAll('.relationship .overlay');
		relationshipOverlay = relationshipEnter.overlay.merge(relationshipOverlay);
		relationshipText = svg.selectAll('.relationship .text');
		relationshipText = relationshipEnter.text.merge(relationshipText);
	}
	
	function zoomFit(scale){
		scale = scale===true;
		var bounds = svg.node().getBBox(),
			parent = svg.node().parentElement.parentElement,
			fullWidth = parent.clientWidth,
			fullHeight = parent.clientHeight,
			width = bounds.width,
			height = bounds.height,
			midX = bounds.x + width / 2,
			midY = bounds.y + height / 2;
		if(width === 0 || height === 0){
			return;
		}
		svgScale = 0.85 / Math.max(width / fullWidth, height / fullHeight);
		svgTranslate = [fullWidth / 2 - svgScale * midX, fullHeight / 2 - svgScale * midY];
		svg.attr('transform', 'translate(' + svgTranslate[0] + ', ' + svgTranslate[1] + ') '+ (scale ? 'scale(' + svgScale + ')' : ''));
	}
	
	function stopSim(){
		animation = false;
		simulation.stop();
	}
	
	function startSim(){
		animation = true;
		simulation.restart();
	}
	
	init(_selector, _options);

	return{
		update: update,
		stopSim: stopSim,
		startSim: startSim,
		zoomFit: zoomFit,
		expand: expand,
		collapse: collapse,
		positions: positions
	};
}