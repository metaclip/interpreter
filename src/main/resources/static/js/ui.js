Dropzone.options.myDropzone = {
	url: '/image', 
	maxFiles:1,
	acceptedFiles:'image/png,.json',
	dictDefaultMessage:'Drop your climate product image here',
	success:function(file,response){
		var reader = new FileReader();
		reader.onload = function(e){
			var img = $('#metaclip-thumbnail');
			if(!file.name.toLowerCase().endsWith('.json')){
				img.attr('src',e.target.result);
				img.css('width','200px');
				img.attr('data-size','thumbnail');
				img.show();
				$('#positionsButton').show();
			}else{
				img.hide();
				$('#positionsButton').hide();
			}
		};
		reader.readAsDataURL(file);
		initGraph(response);
	},
	sending:function(){
		showLoading();	
	},
	complete:function(){
		hideLoading();
	},
	error:function(file,response){
		alert(response);
	}
};
function showLoading(){
	$('#loader').css('display','block');
	$('.navbar-brand img').addClass('rotating');
}
function hideLoading(){
	$('#loader').css('display','none');
	$('.navbar-brand img').removeClass('rotating');
}
function showGraph(){
	$('#nav').removeClass('hidden');
	$('#main').removeClass('hidden');
	$('#front').addClass('hidden');
}
function showFront(){
	$('#nav').addClass('hidden');
	$('#main').addClass('hidden');
	$('#front').removeClass('hidden');
	var dropZone = Dropzone.forElement("#myDropzone");
	dropZone.removeAllFiles(true);
}
function init(){
	$('.grid div.grid-item').click(function(){
		showLoading();
		var img = $(this).find('img').first();
		var sample = $(this).attr('data-sample');
		var thumb = $('#metaclip-thumbnail');
		thumb.attr('src',img.attr('src'));
		thumb.css('width','200px');
		thumb.attr('data-size','thumbnail');
		d3.json('/sample-image?sample='+sample,function(error,response){
			hideLoading();
			initGraph(response);
		});
	});
	var $imgs = $('.grid-item img');
	var msnry  = null;
	$imgs.imagesLoaded(function(){
		msnry = new Masonry('.grid',{
			itemSelector: '.grid-item',
			columnWidth: 20
		});
		msnry.layout();
	})
	
	$('.filter-button-group').on('click','button',function(){
		var filterValue = $(this).attr('data-filter');
		if(filterValue=='*'){
			msnry.$element.find('.grid-item').show();
		}else{
			msnry.$element.find('.grid-item').hide();
			msnry.$element.find('.grid-item'+filterValue).show();
		}
		msnry.layout();
	});
	$('.button-group').each(function(i,buttonGroup){
		var $buttonGroup = $( buttonGroup );
		$buttonGroup.on('click','button',function(){
			$buttonGroup.find('.active').removeClass('active');
			$(this).addClass('active');
		});
	});
}
var graph = null;
function initGraph(data){
	showGraph();
	graph = new MetaclipD3('#metaclip-graph',{
		minCollision: 60,
		graphData: data,
        onNodeDragEnd:function(){
        	if(!$('#animationButton').find('i').hasClass('fa-stop')){
        		graph.stopSim();
        	}
        },
        onNodeDoubleClick: function(node){
        	if(node.expanded===true){
        		node.expanded=false;
                graph.collapse(node);
        	}else{
        		node.expanded=true;
                graph.expand(node);        		
        	}
        },
		zoomFit: true
	});
}
$(document).ready(function(){
	init();
	$('#metaclip-thumbnail').click(function(e){
		var img = $(this);
		if(img.attr('data-size')==='thumbnail'){
			img.css('width',Math.min(600,$('#metaclip-graph').width()-10)+'px');
			img.attr('data-size','big');
		}else{
			img.css('width','200px');
			img.attr('data-size','thumbnail');
		}
	});
	$('#animationButton').click(function(e){
		var el = $(this).find('i');
		if(el.hasClass('fa-stop')){
			graph.stopSim();
			el.removeClass('fa-stop');
			el.addClass('fa-play');
		}else{
			graph.startSim();
			el.addClass('fa-stop');
			el.removeClass('fa-play');
		}
	});
	$('#fitButton').click(function(e){
		graph.zoomFit(true);
	});
	$('#restartButton').click(function(e){
		graph.update();
	});
	$('#homeButton').click(function(e){
		showFront();
	});
	$('#positionsButton').click(function(e){
		var positions = graph.positions();
		var keys = Object.keys(positions);
		var position = '{';
		for(var i=0;i<keys.length;i++){
			position += '"'+keys[i]+'":{';
			position += '"x":'+positions[keys[i]].x+',';
			position += '"y":'+positions[keys[i]].y;
			position += '}';
			if(i<keys.length-1){
				position += ',';
			}
		}
		position += '}';
		
		var data = new FormData();
		data.append('position',position);
		
		var imageSrc = $('#metaclip-thumbnail').attr('src');		
		if(imageSrc.indexOf('base64')>=0){
			var base64Content = imageSrc.replace(/^data:image\/(png|jpg);base64,/,"");
			data.append('content',base64Content);        
		}else{
			var ix = imageSrc.lastIndexOf('/');
			data.append('sample',imageSrc.substring(ix+1));
		}		
		$.ajax({
			type:'POST',
			url:'/export',
			data:data,
			cache:false,
			contentType:false,
			xhrFields:{
				responseType: 'blob'
			},
			processData:false,
			success:function(data){
				var data = new Blob([data]);
				saveAs(data, 'metaclip-image.png');
			},
			error:function(data){
				alert("Error generating image");
			}
		});
	});
});


$.fn.imagesLoaded = function(callback){
	var elems = this.find('img'), elems_src = [], self = this, len = elems.length;
	if(!elems.length){
		callback.call(this);
		return this;
	}
	elems.one('load error',function(){
		if(--len===0){
			len = elems.length;
			elems.one('load error',function(){
				if(--len===0){
					callback.call(self);
				}
			}).each(function(){
				this.src = elems_src.shift();
			});
		}
	}).each(function() {
		elems_src.push(this.src);
		this.src = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==";
	});
	return this;
};