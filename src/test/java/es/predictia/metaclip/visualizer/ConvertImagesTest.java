package es.predictia.metaclip.visualizer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadataNode;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import es.predictia.metaclip.visualizer.model.SampleImage;
import es.predictia.metaclip.visualizer.utils.MetaclipExtractor;
import es.predictia.metaclip.visualizer.utils.MetaclipExtractor.MetaclipExtraction;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ConvertImagesTest {

	@Test
	public void testConvert() throws Exception{
		for(SampleImage sample : SampleImage.values()){
			File file = new File("/tmp/"+sample.name().toLowerCase()+"-original.png");
			InputStream stream = getClass().getResourceAsStream(sample.resource());
			FileUtils.copyInputStreamToFile(stream,file);
			stream.close();

			MetaclipExtraction metaclip = MetaclipExtractor.extract(file);
			//String metaclipConverted = metaclip.getMetaclip().get().replaceAll("http://metaclip.predictia.es/","http://metaclip.org/");
			String metaclipConverted = metaclip.getMetaclip().get().replaceAll("http://metaclip.org/","http://www.metaclip.org/");
			String positions = null;
			
			if(metaclip.getLayout().isPresent()){
				positions = metaclip.getLayout().get();
			}
			
			BufferedImage image = ImageIO.read(file);
			byte[] bytes = MetaclipExtractor.writePositions(image,"png",positions,metaclipConverted);
			FileUtils.writeByteArrayToFile(new File("/tmp/"+sample.name().toLowerCase()+".png"),bytes);
			file.delete();
		}
	}
	
	public static byte[] writeMetaclip(BufferedImage image,String format,String metaclip) throws Exception{
		ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();

		ImageWriteParam writeParam = writer.getDefaultWriteParam();
		ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);

		javax.imageio.metadata.IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);

		IIOMetadataNode textEntryMetaclip = new IIOMetadataNode(METACLIP_TAG_NAME);
		textEntryMetaclip.setAttribute("keyword", METACLIP_ATTRIBUTE_VALUE);
		textEntryMetaclip.setAttribute("value", MetaclipExtractor.binaryToHex(MetaclipExtractor.compress(metaclip.getBytes())));
		
		IIOMetadataNode text = new IIOMetadataNode("tEXt");
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
	
	private static final String METACLIP_TAG_NAME = "tEXtEntry";
	private static final String METACLIP_ATTRIBUTE_VALUE = "metaclip";

}
