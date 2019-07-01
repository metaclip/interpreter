package es.predictia.metaclip.visualizer.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MetaclipExtractor {
	
	public static MetaclipExtraction extract(File input) throws Exception{
		ImageInputStream iis = ImageIO.createImageInputStream(input);
		try{
			return extract(iis);
		}finally{
			iis.close();
		}
	}
	
	public static MetaclipExtraction extract(InputStream input) throws Exception{
		ImageInputStream iis = ImageIO.createImageInputStream(input);
		try{
			return extract(iis);
		}finally{
			iis.close();
		}
	}

	public static MetaclipExtraction extract(ImageInputStream input) throws Exception{
		MetaclipExtraction result = new MetaclipExtraction();
	    Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
	    if(readers.hasNext()){
			ImageReader reader = readers.next();
			reader.setInput(input, true);
			IIOMetadata metadata = reader.getImageMetadata(0);
			String[] names = metadata.getMetadataFormatNames();
			int length = names.length;
			for(int i = 0; i < length; i++){
				Element element = (Element)metadata.getAsTree(names[i]);
				Optional<String> metaclip = findMetaclipElement(element,METACLIP_ATTRIBUTE_VALUE);
				if(metaclip.isPresent()){
					// TODO detect if the string format (e.g. base64...)
					String decoded = decompress(hexToBinary(metaclip.get()));
					result.setMetaclip(Optional.of(decoded));
				}
				Optional<String> metaclipLayout = findMetaclipElement(element,METACLIP_LAYOUT_ATTRIBUTE_VALUE);
				if(metaclipLayout.isPresent()){
					result.setLayout(metaclipLayout);
				}
			}
		}
	    return result;
	}
	
	public static byte[] writePositions(BufferedImage image,String format,String positions,String metaclip) throws Exception{
		ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();

		ImageWriteParam writeParam = writer.getDefaultWriteParam();
		ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);

		javax.imageio.metadata.IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);

		IIOMetadataNode textEntryLayout = null;
		if(!StringUtils.isEmpty(positions)){
			textEntryLayout = new IIOMetadataNode(METACLIP_TAG_NAME);
			textEntryLayout.setAttribute("keyword", METACLIP_LAYOUT_ATTRIBUTE_VALUE);
			textEntryLayout.setAttribute("value", positions);
		}
		
		IIOMetadataNode textEntryMetaclip = new IIOMetadataNode(METACLIP_TAG_NAME);
		textEntryMetaclip.setAttribute("keyword", METACLIP_ATTRIBUTE_VALUE);
		textEntryMetaclip.setAttribute("value", binaryToHex(compress(metaclip.getBytes())));

		IIOMetadataNode text = new IIOMetadataNode("tEXt");
		if(textEntryLayout != null){
			text.appendChild(textEntryLayout);
		}
		text.appendChild(textEntryMetaclip);
		
		IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
		root.appendChild(text);
		
		metadata.mergeTree("javax_imageio_png_1.0", root);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		javax.imageio.stream.ImageOutputStream stream = ImageIO.createImageOutputStream(baos);
		writer.setOutput(stream);
		writer.write(metadata, new IIOImage(image, null, metadata), writeParam);
		stream.close();
		return baos.toByteArray();
	}
	
	private static byte[] hexToBinary(String hex) {
		 return DatatypeConverter.parseHexBinary(hex);
	}
	
	public static String binaryToHex(byte[] array) {
		return DatatypeConverter.printHexBinary(array);
	}
	
	private static String decompress(byte[] data) throws IOException, DataFormatException {
		Inflater inflater = new Inflater();
		inflater.setInput(data);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
		byte[] buffer = new byte[1024];
		while (!inflater.finished()) {
			int count = inflater.inflate(buffer);
			outputStream.write(buffer, 0, count);
		}
		outputStream.close();
		byte[] output = outputStream.toByteArray();
		return new String(output);
	}
	
	public static byte[] compress(byte[] data) throws IOException{  
		Deflater deflater = new Deflater();  
		deflater.setInput(data);  
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);   
		deflater.finish();  
		byte[] buffer = new byte[1024];   
		while(!deflater.finished()){  
			int count = deflater.deflate(buffer);  
			outputStream.write(buffer, 0, count);   
		}  
		outputStream.close();  
		return outputStream.toByteArray();
	}
	
	private static Optional<String> findMetaclipElement(Element elem,final String key) {
		List<String> result = new LinkedList<String>();
		findMetaclipElement(elem, result, key);
		if(!result.isEmpty()){
			return Optional.of(result.get(0));
		}
		return Optional.empty();
	}
	
	private static void findMetaclipElement(Element el, List<String> elementList,final String key) {
		if (METACLIP_TAG_NAME.equals(el.getTagName())){
			if(el.hasAttribute(METACLIP_ATTRIBUTE_NAME)){
				if(el.getAttribute(METACLIP_ATTRIBUTE_NAME).equalsIgnoreCase(key)){
					elementList.add(el.getAttribute(METACLIP_ATTRIBUTE_DATA_NAME));
					return;
				}
			}
		}
		Element elem = getFirstElement(el);
		while (elem != null) {
			findMetaclipElement(elem, elementList, key);
			elem = getNextElement(elem);
		}
	}
	
	private static Element getFirstElement(Node parent) {
		Node n = parent.getFirstChild();
		while (n != null && Node.ELEMENT_NODE != n.getNodeType()) {
			n = n.getNextSibling();
		}
		if (n == null) {
			return null;
		}
		return (Element) n;
	}
	
	private static Element getNextElement(Element el) {
		Node nd = el.getNextSibling();
		while (nd != null) {
			if (nd.getNodeType() == Node.ELEMENT_NODE) {
				return (Element) nd;
			}
			nd = nd.getNextSibling();
		}
		return null;
	}

	public static class MetaclipExtraction{		
		private Optional<String> metaclip = Optional.empty();
		private Optional<String> layout = Optional.empty();
		public Optional<String> getMetaclip() {
			return metaclip;
		}
		public void setMetaclip(Optional<String> metaclip) {
			this.metaclip = metaclip;
		}
		public Optional<String> getLayout() {
			return layout;
		}
		public void setLayout(Optional<String> layout) {
			this.layout = layout;
		}	
	}
	
	private static final String METACLIP_TAG_NAME = "tEXtEntry";
	private static final String METACLIP_ATTRIBUTE_NAME = "keyword";
	private static final String METACLIP_ATTRIBUTE_VALUE = "metaclip";
	private static final String METACLIP_LAYOUT_ATTRIBUTE_VALUE = "metaclip-layout";
	private static final String METACLIP_ATTRIBUTE_DATA_NAME = "value";	
	
}
